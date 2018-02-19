package com.marcovr.terminal;

import com.marcovr.terminal.GUI.TerminalFrame;
import com.marcovr.terminal.commands.CommandHandler;
import com.marcovr.terminal.misc.KeyTranslator;
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
}
