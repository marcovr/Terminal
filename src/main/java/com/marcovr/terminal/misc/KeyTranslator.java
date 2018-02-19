package com.marcovr.terminal.misc;

import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.*;

public class KeyTranslator {

    /**
     * True: use application cursor escape codes
     * False: use normal cursor escape codes
     */
    public boolean applicationCursorKeys = false;

    /**
     * Checks key event and returns defined translation
     *
     * @param e the KEY_PRESSED KeyEvent to handle
     */
    public String translateKey(KeyEvent e) {
        char c = e.getKeyChar();
        if (c != CHAR_UNDEFINED) {
            return translateNormalKey(e);
        } else {
            return translateSpecialKey(e);
        }
    }

    /**
     * Replaces normal key if necessary
     *
     * @param e the KEY_PRESSED KeyEvent
     */
    private String translateNormalKey(KeyEvent e) {
        char c = e.getKeyChar();
        switch (c) {
            case VK_DELETE:
                return "\033[3~";
            case VK_ENTER:
                return "\r";
            default:
                return String.valueOf(c);
        }
    }

    /**
     * Checks special keys and uses defined replacements
     *
     * @param e the KEY_PRESSED KeyEvent
     */
    private String translateSpecialKey(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_UP:
                return cursorCMD('A');
            case VK_DOWN:
                return cursorCMD('B');
            case VK_RIGHT:
                return cursorCMD('C');
            case VK_LEFT:
                return cursorCMD('D');
            case VK_HOME:
                return cursorCMD('H');
            case VK_END:
                return cursorCMD('F');
            case VK_PAGE_UP:
                return "\033[5~";
            case VK_PAGE_DOWN:
                return "\033[6~";
            default:
                return null;
        }
    }

    /**
     * Creates correct cursor escape command
     *
     * @param dir cursor direction ('A' to 'F')
     */
    private String cursorCMD(char dir) {
        return (applicationCursorKeys ? "\033O" : "\033[") + dir;
    }

}
