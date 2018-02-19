package com.marcovr.terminal.GUI;

import com.marcovr.terminal.Terminal;
import com.marcovr.terminal.screen.CellStyle;
import com.marcovr.terminal.screen.Screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Terminal panel derived from JPanel
 */
class TerminalPanel extends JPanel {

    private Screen screen;
    private Terminal terminal;

    /**
     * Creates a new terminal panel
     *
     * @param terminal the terminal to interact with
     */
    TerminalPanel(Terminal terminal) {
        this.terminal = terminal;
        screen = terminal.getScreen();

        setPreferredSize(new Dimension(642, 386));
        setFocusable(true);
        setCursor(new Cursor(Cursor.TEXT_CURSOR));

        // repaint on focus change
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });

        MouseMultiListener listener = new MouseMultiListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);

        // stronger than KeyListener - also catches TAB etc.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && hasFocus()) {
                this.terminal.handleKey(e); // if focused, pass event to terminal
                return true;
            }
            return false;
        });
    }

    /**
     * Overrides default paint method with custom one.
     *
     * @param g the graphics object
     */
    @Override
    protected void paintComponent(Graphics g) {
        _paintComponent((Graphics2D)g);
        //super.paintComponent(g);
    }

    /**
     * Paint the terminal screen, or a red square if none available
     *
     * @param g the graphics object
     */
    private void _paintComponent(Graphics2D g) {
        g.setColor(CellStyle.getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (screen == null) {
            g.setColor(Color.RED);
            g.fillRect(1, 1, CellStyle.WIDTH, CellStyle.HEIGHT);
        }
        else {
            screen.paint(g, hasFocus());
        }
        g.setColor(CellStyle.getBackground());
        g.fillRect(0, 0, 1, getHeight());
    }

    /**
     * Unites MouseEventListeners for terminal panel
     */
    private class MouseMultiListener implements MouseListener, MouseMotionListener {

        private final int NONE = 0;
        private final int CAN_DRAG = 1;
        private final int DRAGGING = 2;

        private int dragState = NONE;
        private Point dragStart;

        @Override
        public void mouseClicked(MouseEvent e) {
            // paste on right-click
            if (e.getButton() == MouseEvent.BUTTON3) {
                terminal.paste();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // start drag on left-press
            if (e.getButton() == MouseEvent.BUTTON1) {
                dragState = CAN_DRAG;
                dragStart = e.getPoint();
                dragStart.translate(0, -1);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // stop dragging on left-release
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (dragState == DRAGGING) {
                    terminal.copy();
                }
                else {
                    terminal.clearSelection();
                }
                dragState = NONE;
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mouseDragged(MouseEvent e) {
            // if dragging, notify terminal of new mouse position
            if (dragState != NONE) {
                Point dragEnd = e.getPoint();
                dragEnd.translate(0, -1);
                terminal.select(dragStart.getLocation(), dragEnd);
                dragState = DRAGGING;
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {}
    }
}
