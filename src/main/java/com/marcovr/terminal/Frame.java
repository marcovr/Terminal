package com.marcovr.terminal;

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
public class Frame extends JFrame {

    private JPanel contentPanel;
    private Panel termPanel;
    private Terminal terminal;
    //private boolean maximised;

    private Frame() {
        super("Terminal");
    }

    /**
     * initialises the terminal window
     */
    private void init() {
        setContentPane(contentPanel);

        terminal = new Terminal(this);
        termPanel.init(terminal);

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
                Dimension size = termPanel.getSize();
                int x = size.width / CellStyle.WIDTH;
                int y = size.height / CellStyle.HEIGHT;
                int width = CellStyle.WIDTH * x + 2;
                int height = CellStyle.HEIGHT * y + 2;

                /*if (!maximised) {
                    size.setSize(width, height);
                    termPanel.setPreferredSize(size);
                    pack();
                }*/
                terminal.resize(x, y, width, height);
            }
        });

        terminal.connect("ras.pi"); // TODO: make changeable
    }

    /**
     * Opens a new Window containing a terminal panel
     */
    public static void launch() {
        Frame frame = new Frame();
        frame.init();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}
