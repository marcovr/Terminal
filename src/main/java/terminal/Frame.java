package terminal;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Terminal window
 */
public class Frame extends JFrame {

    private JPanel contentPanel;
    private Panel termPanel;
    private final Terminal terminal;

    /**
     * initialises the terminal window
     */
    private Frame() {
        super("Terminal");
        setContentPane(contentPanel);

        terminal = new Terminal(this);
        termPanel.init(terminal);

        // Properly close
        addWindowListener(new WindowAdapter() {
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
        JFrame frame = new Frame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

}
