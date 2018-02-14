package ssh;

import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile;

import java.io.File;

public class CredentialsHandler {

    private static KeyProvider key;

    public static void loadKey(String pathname) {
        PuTTYKeyFile k = new PuTTYKeyFile();
        k.init(new File(pathname));
        key = k;
    }

    public static KeyProvider getKey() {
        return key;
    }

}
