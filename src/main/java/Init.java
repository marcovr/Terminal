import ssh.CredentialsHandler;
import terminal.Frame;

public class Init {

    /**
     * Entry-point for application. Starts terminal window.
     *
     * @param args Command line arguments (ignored)
     */
    public static void main(String[] args) {
        CredentialsHandler.loadKey("C:\\Tools\\Putty Data\\private key.ppk");
        Frame.launch();
    }

}
