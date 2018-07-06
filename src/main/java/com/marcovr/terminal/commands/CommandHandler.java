package com.marcovr.terminal.commands;

import com.marcovr.terminal.screen.Screen;
import com.marcovr.terminal.screen.CellStyle;
import com.marcovr.terminal.screen.Cursor;
import com.marcovr.terminal.ssh.ConnectionHandler;
import com.marcovr.terminal.Terminal;

import java.io.EOFException;
import java.io.IOException;

public class CommandHandler {

    private final Screen screen;
    private final ConnectionHandler handler;
    private final Terminal terminal;
    private final NumArgRetriever numArgs;
    private Cursor cursor;
    private boolean qModifier;

    private static final int TAB_SIZE = 8;

    public CommandHandler(Terminal terminal) {
        this.terminal = terminal;
        handler = terminal.getConnectionHandler();
        screen = terminal.getScreen();
        numArgs = new NumArgRetriever(handler);
        cursor = screen.getCursor();
        qModifier = false;
    }

    public void start() {
        new Thread(this::readLoop).start();
    }

    @SuppressWarnings("InfiniteLoopStatement")
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
            case 5: // ENQ
                handler.send("Terminal");
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
                cursor.carriageReturn();
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
        int b = numArgs.retrieve();
        int n = numArgs.getArgOrDef(0, 1);
        int x;

        switch (b) {
            case '@': // Blanks
                screen.insertBlanks(n);
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
                cursor.carriageReturn();
                break;
            case 'F':
                cursor.up(n);
                cursor.carriageReturn();
                break;
            case '`':
            case 'G':
                cursor.setX(n - 1);
                break;
            case 'f':
            case 'H':
                cursor.setX(numArgs.getArgOrDef(1, 1) - 1);
                cursor.setY(n - 1);
                break;
            case 'I': // Tab stops
                x = cursor.getX() / TAB_SIZE + n * TAB_SIZE;
                cursor.setX(Math.max(x, screen.getWidth() - 1));
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
                x = (cursor.getX() - 1) / TAB_SIZE - (n - 1) * TAB_SIZE;
                cursor.setX(Math.min(x, 0));
                break;
            case 'd':
                cursor.setY(n - 1);
                break;
            case 'h':
            case 'l':
                do {
                    processCSIhl((char)b, numArgs.consumeArgOrDef(-1));
                } while (numArgs.hasArguments());
                break;
            case 'm': // SGR
                do {
                    applySGRArg(numArgs.consumeArgOrDef(0));
                } while (numArgs.hasArguments());
                break;
            case 'r':
                screen.scrollTop = n - 1;
                screen.scrollBottom = numArgs.getArgOrDef(1, screen.getHeight());
                break;
            case 't':
                numArgs.consumeArgOrDef(0);
                if (!terminal.handleXterm(n, numArgs.toList())) {
                    unsupported("CSI " + n + ";" + numArgs + " t");
                }
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
                    cursor.setForeground(CellStyle.getForeground());
                    cursor.setBackground(CellStyle.getBackground());
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
                    cursor.setInverted(true);
                    break;
                /*case 8:
                    // conceal
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
                    cursor.setInverted(false);
                    break;
                /*case 28:
                    // reveal
                    break;*/
            }
        }
        else if (x < 40) {
            x -= 30;
            cursor.setForeground(CellStyle.getForeground(x));
        }
        else if (x < 50) {
            x -= 40;
            cursor.setBackground(CellStyle.getBackground(x));
        }
        else if (x >= 90 && x <= 97) {
            x -= 80;
            cursor.setForeground(CellStyle.getForeground(x));
        }
        else if (x >= 100 && x <= 107) {
            x -= 90;
            cursor.setBackground(CellStyle.getBackground(x));
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
                terminal.setApplicationCursorKeys(is_h);
                break;
            case 4:
                break;
            case 5:
                screen.inverted = is_h;
                /*if (is_h) {
                    terminal.repaint();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/
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
                if (is_h) screen.useAlternateBuffer(false);
                else screen.useNormalBuffer();
                break;
            case 1047:
                if (is_h) screen.useAlternateBuffer(true);
                else screen.useNormalBuffer();
                break;
            case 1048:
                if (is_h) screen.saveCursor();
                else screen.restoreCursor();
                cursor = screen.getCursor();
                break;
            case 1049:
                if (is_h) {
                    screen.saveCursor();
                    screen.useAlternateBuffer(true);
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

    private void erase(int n) {
        switch(n) {
            case 0:
                for (int i = cursor.getY() + 1; i < screen.getHeight(); i++) {
                    screen.clearLine(i);
                }
                break;
            case 1:
                for (int i = 0; i < cursor.getY(); i++) {
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
                for (int i = cursor.getX(); i < screen.getWidth(); i++) {
                    screen.clearCell(i, cursor.getY());
                }
                break;
            case 5:
                for (int i = 0; i < cursor.getX(); i++) {
                    screen.clearCell(i, cursor.getY());
                }
                break;
            case 6:
                screen.clearLine(cursor.getY());
                break;
        }
    }
}
