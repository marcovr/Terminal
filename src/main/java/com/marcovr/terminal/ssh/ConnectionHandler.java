package com.marcovr.terminal.ssh;

import com.marcovr.terminal.misc.UnicodeHelper;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.LoggerFactory;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.PTYMode;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.method.AuthMethod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConnectionHandler {

    private static final boolean DEBUG = false;

    private SSHClient ssh;
    private Session session;
    private OutputStreamWriter writer;
    private InputStreamReader reader;
    private Session.Shell shell;

    public void connect(String hostname, int port) throws IOException {
        ssh = new SSHClient();
        //ssh.useCompression();

        //ssh.loadKnownHosts();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        ssh.connect(hostname, port);
    }

    public void authenticate(String username, AuthMethod auth) throws UserAuthException, TransportException {
        ssh.auth(username, auth);
    }

    public void startShell() throws ConnectionException, TransportException {
        session = ssh.startSession();

        Map<PTYMode, Integer> modes = new HashMap<>();
        session.allocatePTY("xterm", 80, 24, 640, 384, modes);

        shell = session.startShell();

        new StreamCopier(shell.getErrorStream(), System.err, LoggerFactory.DEFAULT)
                .bufSize(shell.getLocalMaxPacketSize())
                .spawn("stderr");

        OutputStream outputStream = shell.getOutputStream();
        InputStream inputStream = shell.getInputStream();
        reader = new UnicodeHelper.UnicodeStreamReader(inputStream);
        writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
    }

    public void disconnect() {
        try {
            session.close();
        } catch (Exception ignored) {}
        try {
            ssh.disconnect();
        } catch (Exception ignored) {}
    }

    public void resizeShell(int cols, int rows, int width, int height) {
        if (shell != null) {
            try {
                shell.changeWindowDimensions(cols, rows, width, height);
            } catch (TransportException e) {
                e.printStackTrace();
            }
        }
    }

    public int receive() throws IOException {
        int x = reader.read();
        if (x < 0) {
            throw new EOFException();
        }
        logIN(x);
        return x;
    }

    public void send(char c) {
        if (writer != null) {
            try {
                logOUT(c);
                writer.write(c);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(String s) {
        if (writer != null) {
            try {
                for (char c : s.toCharArray()) {
                    writer.write(c);
                }
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void logIN(int b) {
        if (DEBUG) {
            System.err.print(UnicodeHelper.codePointToReadable(b));
        }
    }

    private void logOUT(char c) {
        if (DEBUG) {
            System.err.println("[OUT:" + c + "]");
        }
    }

    public void println() {
        if (DEBUG) {
            System.err.println();
        }
    }
}
