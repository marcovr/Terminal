package commands;

import screen.Screen;
import screen.CellStyle;
import screen.Cursor;
import ssh.ConnectionHandler;
import terminal.Terminal;

import java.io.EOFException;
import java.io.IOException;

public class CommandHandler {

    private final Screen screen;
    private final ConnectionHandler handler;
    private final Terminal terminal;
    private final NumArgAccumulator numArgs;
    private Cursor cursor;
    private boolean qModifier;

    private int TABSIZE = 8; // TODO: make changeable

    public CommandHandler(Terminal terminal) {
        this.terminal = terminal;
        handler = terminal.getConnectionHandler();
        screen = terminal.getScreen();
        numArgs = new NumArgAccumulator();
        cursor = screen.getCursor();
        qModifier = false;
    }

    public void start() {
        new Thread(this::readLoop).start();
    }

    private void readLoop() {
        Thread.currentThread().setName("CommandHandler");

        try {
            while (true) {
                handleChar(handler.receive());
                terminal.repaint();
            }
        } catch (IOException e) {
            if (e instanceof EOFException) {
                terminal.shutdown();
                return;
            }
            e.printStackTrace();
        }
    }

    private void handleChar(int b) throws IOException {
        switch (b) {
            case 0: // NUL
                break;
            case 7: // BEL
                java.awt.Toolkit.getDefaultToolkit().beep();
                break;
            case 8: // BS
                cursor.prev();
                break;
            case 10: // LF
                cursor.lineFeed();
                break;
            case 13: // CR
                cursor.x = 0;
                break;
            case 27: // ESC
                handleESC(handler.receive());
                break;
            default:
                screen.write(b);
        }
    }

    private void handleESC(int b) throws IOException {
        switch (b) {
            case '[': // CSI
                processCSI();
                break;
            case ']': // OSC
                processOSC(handler.receive());
                break;
            case '(':
            case ')':
            case '*':
            case '+':
                handler.receive(); // ignore
                break;
            case '7':
                screen.saveCursor();
                break;
            case '8':
                screen.restoreCursor();
                cursor = screen.getCursor();
                break;
            case 'M':
                screen.scroll(-1);
                break;
            case '=': // NumLock off
            case '>': // NumLock on
                break;
            default:
                unsupported("ESC", b);
        }
    }

    private void processCSI() throws IOException {
        int b;
        numArgs.start();
        do {
            b = handler.receive();
        } while (numArgs.accumulate(b));

        int n = numArgs.getArgOrDef(0, 1);
        int x;

        switch (b) {
            case '@': // Blanks
                times(n, screen::writeBlank);
                break;
            case 'A':
                cursor.up(n);
                break;
            case 'B':
                cursor.down(n);
                break;
            case 'C':
                cursor.right(n);
                break;
            case 'D':
                cursor.left(n);
                break;
            case 'E':
                cursor.down(n);
                cursor.x = 0;
                break;
            case 'F':
                cursor.up(n);
                cursor.x = 0;
                break;
            case '`':
            case 'G':
                cursor.x = n - 1;
                break;
            case 'f':
            case 'H':
                cursor.x = numArgs.getArgOrDef(1, 1) - 1;
                cursor.y = n - 1;
                break;
            case 'I': // Tab stops
                x = cursor.x / TABSIZE + n * TABSIZE;
                cursor.x = Math.max(x, screen.getWidth() - 1);
                break;
            case 'J':
                erase(numArgs.getArgOrDef(0, 0));
                break;
            case 'K':
                erase(numArgs.getArgOrDef(0, 0) + 4);
                break;
            /*
            case 'L':
                break;
            case 'M':
                break;*/
            case 'P':
                screen.delete(n);
                break;
            case 'S':
                screen.scroll(n);
                break;
            case 'T':
                screen.scroll(-n);
                break;
            /*case 'X':
                break;*/
            case 'Z':
                x = (cursor.x - 1) / TABSIZE - (n - 1) * TABSIZE;
                cursor.x = Math.min(x, 0);
                break;
            case 'd':
                cursor.y = n - 1;
                break;
            case 'h':
            case 'l':
                do {
                    processCSIhl((char)b, numArgs.consumeArgOrDef(-1));
                } while (!numArgs.isEmpty());
                break;
            case 'm': // SGR
                do {
                    applySGRArg(numArgs.consumeArgOrDef(0));
                } while (!numArgs.isEmpty());
                break;
            case 'r':
                screen.scrollTop = n - 1;
                screen.scrollBottom = numArgs.getArgOrDef(1, screen.getHeight());
                break;

            case '?':
                qModifier = true;
                processCSI();
                return;
            default:
                unsupported("CSI " + numArgs, b);
        }
        qModifier = false;
        handler.println();
    }

    private void processOSC(int b) throws IOException {
        switch (b) {
            case '0': // set window title
            case '1':
            case '2':
                handler.receive();
                terminal.setTitle(readTitle());
                break;
            default:
                unsupported("OSC", b);
        }
    }

    private String readTitle() throws IOException {
        int b;
        StringBuilder title = new StringBuilder();

        boolean incomplete = true;
        while (incomplete) {
            switch (b = handler.receive()) {
                case 7: // BEL
                    incomplete = false;
                    break;
                case 27: // ESC
                    b = handler.receive();
                    if (b != '\\') {
                        unsupported("OSC-title: " + title.toString() + "\\e", b);
                    }
                    incomplete = false;
                    break;
                default:
                    title.append((char)b);
            }
        }
        return title.toString();
    }

    private void applySGRArg(int x) {
        if (x < 30) {
            switch(x) {
                case 0:
                    cursor.foreground = CellStyle.getForeground();
                    cursor.background = CellStyle.getBackground();
                    cursor.style = CellStyle.REGULAR;
                    break;
                case 1:
                case 5:
                    cursor.style = cursor.style | CellStyle.BOLD;
                    break;
                case 2:
                case 3:
                    cursor.style = cursor.style | CellStyle.ITALIC;
                    break;
                case 4:
                    cursor.style = cursor.style | CellStyle.UNDERLINE;
                    break;
                case 7:
                    cursor.invertColors();
                    break;
                /*case 8:
                    cursor.foreground = cursor.background;
                    break;*/
                case 21:
                case 22:
                case 25:
                    cursor.style = cursor.style & ~CellStyle.BOLD;
                    break;
                case 23:
                    cursor.style = cursor.style & ~CellStyle.ITALIC;
                    break;
                case 24:
                    cursor.style = cursor.style & ~CellStyle.UNDERLINE;
                    break;
                case 27:
                    cursor.invertColors();
                    break;
                /*case 28:
                    // hmmm
                    break;*/
            }
        }
        else if (x < 40) {
            x -= 30;
            cursor.foreground = CellStyle.getForeground(x);
        }
        else if (x < 50) {
            x -= 40;
            cursor.background = CellStyle.getBackground(x);
        }
        else if (x >= 90 && x <= 97) {
            x -= 80;
            cursor.foreground = CellStyle.getForeground(x);
        }
        else if (x >= 100 && x <= 107) {
            x -= 90;
            cursor.background = CellStyle.getBackground(x);
        }
        else {
            unsupported("SGR " + x);
        }
    }

    private void processCSIhl(char b, int x) {
        boolean is_h = b == 'h';

        if (!qModifier) {
            switch (x) {
                case 4:
                    screen.replaceMode = !is_h;
                    break;
                default:
                    unsupported("CSI " + x + " " + b);
            }
            return;
        }
        switch (x) {
            case 1:
                terminal.applicationCursorKeys = is_h;
                break;
            case 4:
                break;
            case 5:
                screen.inverted = is_h;
                break;
            case 7:
                screen.autoWrap = is_h;
                break;
            case 12:
                cursor.blinking = is_h;
                break;
            case 25:
                cursor.visible = is_h;
                break;
            case 47:
            case 1047:
                if (is_h) screen.useAlternateBuffer();
                else screen.useNormalBuffer(); // TODO: clearing it first ???
                break;
            case 1048:
                if (is_h) screen.saveCursor();
                else screen.restoreCursor();
                cursor = screen.getCursor();
                break;
            case 1049:
                if (is_h) {
                    screen.saveCursor();
                    screen.useAlternateBuffer();
                    screen.clear();
                }
                else {
                    screen.useNormalBuffer();
                    screen.restoreCursor();
                    cursor = screen.getCursor();
                }
                break;
            default:
                unsupported("CSI? " + x + " " + b);
        }
    }

    private void unsupported(String text) {
        handler.println();
        System.err.println("unsupported: " + text);
    }

    private void unsupported(String text, int b) {
        handler.println();
        System.err.println("unsupported: " + text + " " + (char)b + " [" + b + "]");
    }

    private void times(int n, Runnable r) {
        for (int i = 0; i < n; i++) {
            r.run();
        }
    }

    private void erase(int n) {
        switch(n) {
            case 0:
                for (int i = cursor.y + 1; i < screen.getHeight(); i++) {
                    screen.clearLine(i);
                }
                break;
            case 1:
                for (int i = 0; i < cursor.y; i++) {
                    screen.clearLine(i);
                }
                break;
            case 2:
                screen.clear();
                break;
            case 3:
                unsupported("CSI 3J");
                break;
            case 4:
                for (int i = cursor.x; i < screen.getWidth(); i++) {
                    screen.clearCell(i, cursor.y);
                }
                break;
            case 5:
                for (int i = 0; i < cursor.x; i++) {
                    screen.clearCell(i, cursor.y);
                }
                break;
            case 6:
                screen.clearLine(cursor.y);
                break;
        }
    }
}
