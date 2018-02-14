package terminal;

import net.schmizz.sshj.userauth.method.AuthMethod;
import net.schmizz.sshj.userauth.method.AuthPublickey;
import screen.Buffer;
import commands.CommandHandler;
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

public class Terminal {

    private final Buffer buffer;
    private final JFrame frame;
    private ConnectionHandler handler;
    private String hostname;
    private String username;

    public boolean applicationCursorKeys;

    public Terminal(JFrame frame) {
        this.frame = frame;
        buffer = new Buffer();
        applicationCursorKeys = false;
    }

    public void connect(String hostname) {
        this.hostname = hostname;
        username = "pi";
        frame.setTitle(hostname + " - Terminal");
        new Thread(this::_connect).start();
    }

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
        }
    }

    public void disconnect() {
        handler.disconnect();
    }

    public void shutdown() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public ConnectionHandler getConnectionHandler() {
        return handler;
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public void copy() {
        getClipboard().setContents(new StringSelection(buffer.getSelection()), null);
    }

    public void paste() {
        try {
            handler.send((String) getClipboard().getData(DataFlavor.stringFlavor));
        } catch (UnsupportedFlavorException | IOException ignored) {}
    }

    private Clipboard getClipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    public void select(Point start, Point end) {
        buffer.select(start, end);
    }

    public void clearSelection() {
        buffer.clearSelection();
    }

    public void handleKey(KeyEvent e) {
        char c = e.getKeyChar();
        if (c != CHAR_UNDEFINED) {
            //System.out.println(e.paramString() + " -> " + (int) c);
            handleNormalKey(e);
        }
        else {
            handleSpecialKey(e);
        }
    }

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

    private void sendCursorCMD(char dir) {
        handler.send((applicationCursorKeys ? "\033O" : "\033[") + dir);
    }

    private void print(String s) {
        buffer.getCursor().write(s);
    }

    private void println(String s) {
        print(s);
        buffer.getCursor().CR_LF();
    }
}
