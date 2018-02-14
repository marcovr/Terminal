package terminal;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Terminal window
 */
public class Frame {

    private static JFrame frame;
    private JPanel contentPanel;
    private Panel termPanel;
    private final Terminal terminal;

    /**
     * initialises the terminal window
     */
    private Frame() {

        terminal = new Terminal(frame);
        termPanel.init(terminal);

        // Properly close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            terminal.disconnect();
            termPanel.shutdown();
            }
        });

        terminal.connect("ras.pi"); // TODO: make changeable
    }

    /**
     * Opens a new Window containing a terminal panel
     */
    public static void launch() {
        frame = new JFrame("Terminal");
        frame.setContentPane(new Frame().contentPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}
