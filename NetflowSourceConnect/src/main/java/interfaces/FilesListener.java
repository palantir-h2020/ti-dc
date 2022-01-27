package interfaces;

import java.util.ArrayList;

/**
 * FilesListener interface about new detected files. Using Observer patthern,
 * when FileWatcher service detects new file, it will create and transmit
 * an event to all classes that implement FilesListener.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public interface FilesListener {
    /**
     * It will notify all classes that implement this listener, about
     * new detected files.
     *
     * @param files An ArrayList(String) with all new filenames detected.
     */
    void getFilenames(ArrayList<String> files);
}
