package com.marcovr.terminal.GUI;

import com.marcovr.terminal.Terminal;
import com.marcovr.terminal.screen.CellStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Terminal window
 */
public class TerminalFrame extends JFrame {

    //private boolean maximised;

    /**
     * creates the terminal window
     */
    public TerminalFrame(Terminal terminal) {
        super("Terminal");

        TerminalPanel terminalPanel = new TerminalPanel(terminal);
        setContentPane(terminalPanel);

        // Properly close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                terminal.disconnect();
            }
        });
        /*addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                maximised = (e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            }
        });*/
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension size = terminalPanel.getSize();
                int x = size.width / CellStyle.WIDTH;
                int y = size.height / CellStyle.HEIGHT;
                int width = CellStyle.WIDTH * x + 2;
                int height = CellStyle.HEIGHT * y + 2;

                /*if (!maximised) {
                    size.setSize(width, height);
                    terminalPanel.setPreferredSize(size);
                    pack();
                }*/
                terminal.resize(x, y, width, height);
            }
        });
    }

}
