package terminal;

import screen.Buffer;
import screen.BufferPainter;
import screen.CellStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Terminal panel derived from JPanel
 */
public class Panel extends JPanel {

    private Buffer buffer;
    private Terminal terminal;
    private boolean running = true;

    private static final int REFRESH_RATE = 60;

    /**
     * Creates a new terminal panel
     */
    public Panel() {
        setPreferredSize(new Dimension(642, 386));
        setFocusable(true);
        setCursor(new Cursor(Cursor.TEXT_CURSOR));
    }

    /**
     * Initialises the terminal panel and starts a thread checking for refreshes.
     *
     * @param terminal the terminal to interact with
     */
    public void init(Terminal terminal) {
        this.terminal = terminal;
        buffer = terminal.getBuffer();

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

        new Thread(this::refreshCheck).start();
    }

    /**
     * Stops the refresh thread
     */
    public void shutdown() {
        running = false;
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
     * Uses {@link BufferPainter} to paint the terminal buffer, or a red square if none available
     *
     * @param g the graphics object
     */
    private void _paintComponent(Graphics2D g) {
        g.setColor(CellStyle.getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        if (buffer == null) {
            g.setColor(Color.RED);
            g.fillRect(1, 1, CellStyle.WIDTH, CellStyle.HEIGHT);
        }
        else {
            BufferPainter.paint(g, buffer, hasFocus());
        }
        g.setColor(CellStyle.getBackground());
        g.fillRect(0, 0, 1, getHeight());
    }

    /**
     * Tests whether or not the terminal buffer needs to be repainted.
     */
    private void refreshCheck() {
        Thread.currentThread().setName("RefreshChecker");
        while (running) {
            try {
                Thread.sleep(1000 / REFRESH_RATE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // if buffer exists and is tainted, repaint
            if (buffer != null && buffer.tainted.getAndSet(false)) {
                repaint();
            }
        }
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
