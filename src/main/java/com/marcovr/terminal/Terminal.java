package com.marcovr.terminal;

import com.marcovr.terminal.GUI.TerminalFrame;
import com.marcovr.terminal.commands.CommandHandler;
import com.marcovr.terminal.misc.KeyTranslator;
import com.marcovr.terminal.screen.CellStyle;
import com.marcovr.terminal.screen.Screen;
import com.marcovr.terminal.ssh.ConnectionHandler;
import com.marcovr.terminal.ssh.CredentialsHandler;
import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPublickey;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Main terminal class
 */
public class Terminal {

    private final Screen screen;
    private final TerminalFrame frame;
    private final KeyTranslator keys;
    private ConnectionHandler handler;
    private String hostname;
    private int port;
    private String username;

    /**
     * Creates a new terminal
     */
    public Terminal() {
        screen = new Screen();
        frame = new TerminalFrame(this);
        keys = new KeyTranslator();

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Starts a connection in a new thread
     *
     * @param hostname the host to connect to
     * @param port the port to connect to
     */
    public void connect(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
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
            handler.connect(hostname, port);

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
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
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

    public void setApplicationCursorKeys(boolean status) {
        keys.applicationCursorKeys = status;
    }

    /**
     * Sets the title of the corresponding JFrame
     *
     * @param title the title String
     */
    public void setTitle(String title) {
        frame.setTitle(title);
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
            String keyPress = keys.translateKey(e);
            if (keyPress != null) {
                handler.send(keyPress);
            }
        }
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

    /**
     * Repaints the terminal window
     */
    public void repaint() {
        frame.repaint();
    }

    public boolean handleXterm(int n, java.util.List<Integer> args) {
        if (args.size() > 2) {
            return false;
        }

        switch (n) {
            case 1:
                removeState(Frame.ICONIFIED);
                break;
            case 2:
                addState(Frame.ICONIFIED);
                break;
            case 3:
                if (args.size() < 2) {
                    return false;
                }
                frame.setLocation(args.get(0), args.get(1));
                break;
            case 4:
                if (args.size() < 2) {
                    return false;
                }
                frame.getContentPane().setPreferredSize(new Dimension(args.get(1), args.get(0)));
                frame.pack();
                break;
            case 5:
                frame.toFront();
                break;
            case 6:
                frame.toBack();
                break;
            case 7:
                frame.repaint();
                break;
            case 8:
                if (args.size() < 2) {
                    return false;
                }
                int width = CellStyle.WIDTH * args.get(1) + 2;
                int height = CellStyle.HEIGHT * args.get(0) + 2;
                frame.setSize(width, height);
                break;
            case 9:
                if (args.size() == 0) {
                    return false;
                }
                switch (args.get(0)) {
                    case 0: removeState(Frame.MAXIMIZED_BOTH);
                    case 1: addState(Frame.MAXIMIZED_BOTH);
                }
                break;
            case 11:
                int iconified = (frame.getState() & Frame.ICONIFIED) + 1;
                handler.send("\033[" + iconified + "t");
                break;
            case 13:
                Point p = frame.getLocation();
                handler.send("\033[3;" + p.x + ";" + p.y + "t");
                break;
            case 14:
                Dimension s = frame.getContentPane().getSize();
                handler.send("\033[3;" + s.height + ";" + s.width + "t");
                break;
            case 18:
                handler.send("\033[8;" + screen.getHeight() + ";" + screen.getWidth() + "t");
                break;
            case 19:
                handler.send("\033[9;" + screen.getHeight() + ";" + screen.getWidth() + "t");
                break;
            case 20:
                handler.send("\033]L" + frame.getTitle() + "\033\\");
                break;
            case 21:
                handler.send("\033]l" + frame.getTitle() + "\033\\");
                break;
            default: return false;
        }
        return true;
    }

    private void addState(int state) {
        frame.setState(frame.getState() | state);
    }

    private void removeState(int state) {
        frame.setState(frame.getState() & ~state);
    }
}
