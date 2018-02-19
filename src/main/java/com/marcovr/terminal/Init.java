package com.marcovr.terminal;

import com.marcovr.terminal.ssh.CredentialsHandler;

public class Init {

    /**
     * Entry-point for application. Starts terminal window.
     *
     * @param args Command line arguments (ignored)
     */
    public static void main(String[] args) {
        CredentialsHandler.loadKey("C:\\Tools\\Putty Data\\private key.ppk");
        new Terminal().connect("ras.pi");
    }

}
