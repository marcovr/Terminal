package terminal;

import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import commands.CommandHandler;
import screen.Screen;
import ssh.ConnectionHandler;
import ssh.CredentialsHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import static java.awt.event.KeyEvent.*;

import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Main terminal class
 */
public class Terminal {

    private final Screen screen;
    private final JFrame frame;
    private ConnectionHandler handler;
    private String hostname;
    private String username;

    /**
     * True: transmit application cursor escape codes
     * False: transmit normal cursor escape codes
     */
    public boolean applicationCursorKeys;

    /**
     * Creates a new terminal
     *
     * @param frame the JFrame hosting the terminal (used for title and close on EOF)
     */
    public Terminal(JFrame frame) {
        this.frame = frame;
        screen = new Screen();
        applicationCursorKeys = false;
    }

    /**
     * Starts a connection in a new thread
     *
     * @param hostname the host to connect to
     */
    public void connect(String hostname) {
        this.hostname = hostname;
        username = "pi"; // TODO: make changeable
        frame.setTitle(hostname + " - Terminal");
        new Thread(this::_connect).start();
    }

    /**
     * Creates a {@link ConnectionHandler} and starts a connection
     */
    private void _connect() {
        Thread.currentThread().setName("Connection");

        handler = new ConnectionHandler();
        try {
            handler.connect(hostname);

            println("Using username \"" + username + "\".");
            AuthMethod auth = new AuthPublickey(CredentialsHandler.getKey());
            handler.authenticate(username, auth);

            handler.startShell();

            new CommandHandler(this).start();
        } catch (IOException e) {
            e.printStackTrace();
            handler = null;
            println(e.toString());
        }
    }

    /**
     * Disconnects from host
     */
    public void disconnect() {
        handler.disconnect();
        handler = null;
    }

    /**
     * Sends a close event to the corresponding JFrame
     */
    public void shutdown() {
        if (frame != null) {
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        }
    }

    /**
     * @return the terminal screen
     */
    public Screen getScreen() {
        return screen;
    }

    /**
     * @return the connectionHandler
     */
    public ConnectionHandler getConnectionHandler() {
        return handler;
    }

    /**
     * Sets the title of the corresponding JFrame
     *
     * @param title the title String
     */
    public void setTitle(String title) {
        if (frame != null) {
            frame.setTitle(title);
        }
    }

    /**
     * Resize terminal
     *
     * @param cols number of columns
     * @param rows number of rows
     * @param width in pixel
     * @param height in pixel
     */
    public void resize(int cols, int rows, int width, int height) {
        if (handler != null) {
            handler.resizeShell(cols, rows, width, height);
            screen.resize(cols, rows);
        }
        repaint();
    }

    /**
     * Copies the text from the selected screen area to the clipboard
     */
    public void copy() {
        getClipboard().setContents(new StringSelection(screen.getSelection()), null);
    }

    /**
     * Pastes any text from the clipboard as input
     */
    public void paste() {
        if (handler != null) {
            try {
                handler.send((String) getClipboard().getData(DataFlavor.stringFlavor));
            } catch (UnsupportedFlavorException | IOException ignored) {}
        }
    }

    /**
     * @return the system clipboard
     */
    private Clipboard getClipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    /**
     * Marks the selected area in the screen
     *
     * @param start selection start (pixels relative to terminal screen)
     * @param end selection end (pixels relative to terminal screen)
     */
    public void select(Point start, Point end) {
        screen.select(start, end);
        repaint();
    }

    /**
     * Clears selection if any
     */
    public void clearSelection() {
        screen.clearSelection();
        repaint();
    }

    /**
     * Checks key event and performs defined action
     *
     * @param e the KEY_PRESSED KeyEvent to handle
     */
    public void handleKey(KeyEvent e) {
        if (handler != null) {
            char c = e.getKeyChar();
            if (c != CHAR_UNDEFINED) {
                //System.out.println(e.paramString() + " -> " + (int) c);
                handleNormalKey(e);
            } else {
                handleSpecialKey(e);
            }
        }
    }

    /**
     * Replaces normal key if necessary, then uses it as input
     *
     * @param e the KEY_PRESSED KeyEvent
     */
    private void handleNormalKey(KeyEvent e) {
        char c = e.getKeyChar();
        switch (c) {
            case VK_DELETE:
                handler.send("\033[3~");
                break;
            case VK_ENTER:
                handler.send('\r');
                break;
            default:
                handler.send(c);
        }
    }

    /**
     * Checks special keys and uses defined replacements as input
     *
     * @param e the KEY_PRESSED KeyEvent
     */
    private void handleSpecialKey(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_UP:
                sendCursorCMD('A');
                break;
            case VK_DOWN:
                sendCursorCMD('B');
                break;
            case VK_RIGHT:
                sendCursorCMD('C');
                break;
            case VK_LEFT:
                sendCursorCMD('D');
                break;
            case VK_HOME:
                sendCursorCMD('H');
                break;
            case VK_END:
                sendCursorCMD('F');
                break;
            case VK_PAGE_UP:
                handler.send("\033[5~");
                break;
            case VK_PAGE_DOWN:
                handler.send("\033[6~");
                break;
        }
    }

    /**
     * Creates correct cursor escape command and uses it as input
     *
     * @param dir cursor direction ('A' to 'F')
     */
    private void sendCursorCMD(char dir) {
        handler.send((applicationCursorKeys ? "\033O" : "\033[") + dir);
    }

    /**
     * Writes text onto the screen as if received
     *
     * @param s String to print
     */
    private void print(String s) {
        screen.write(s);
        repaint();
    }

    /**
     * Writes text onto the screen as if received. Appends a newline
     *
     * @param s String to print
     */
    private void println(String s) {
        screen.write(s);
        screen.getCursor().CR_LF();
        repaint();
    }

    public void repaint() {
        frame.repaint();
    }
}
