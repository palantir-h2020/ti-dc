package utils;

import interfaces.FilesListener;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Filewatcher service. Watches a specific folder about new files.
 * Uses a file (netflow-watcher-[collectorId].out) to store last read file.
 * In Windows under location %APPDATA%\Local\Temp\Palantir.
 * In Linux this file is saved under /var/tmp/Palantir.
 * Filters all files in the directory and keeps only these that have name
 * bigger compared with the stored one. This is based on the name of the
 * nfcapd files, which have same prefix and date in their names in the format
 * YYYYMMDD, so the new ones can be filtered. This will run in a separate
 * thread, scans the directory evey X (configured) seconds. When new files found
 * the file in application's path will be updated and an event must be sent to
 * Kafka source connector, with all new detected files.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class FileWatcher implements Runnable {
    /**
     * Logger Instance.
     */
    private Logger logger = Logger.getLogger(FileWatcher.class);

    /**
     * A list, with all listeners, that will receive transmitted events.
     */
    private List<FilesListener> listeners = new ArrayList<FilesListener>();

    /**
     * Id of the connector.
     */
    private int collectorId;
    /**
     * Path where application files and logs will be saved.
     * For Windows OSes, this path is %APPDATA%\Local\Temp\Palantir\.
     * For Linus OSes, this path is /var/tmp/Palantir/
     */
    private String dataPath;
    /**
     * Prefix of the files, where the last seen file for each connector will
     * be stored. Each connector will has its own file, identified by tis id.
     */
    private String dataFilePrefix;

    /**
     * The path, which this file watcher will observer for new files.
     */
    private String pathObserve;
    /**
     * Filename, where the last processed netflow file will be saved.
     * Its absolute path is: [dataPath]/[dataFilePrefix]-[collectorId].out
     */
    private String lastSeenFilename;

    /**
     * Constructor
     *
     * @param collectorId   Id of this collector. Id must be unique.
     * @param prefix        Prefix of file, where last seen nfcapd is stored.
     * @param pathToObserve Path to scan for new nfcapd files.
     */
    public FileWatcher(int collectorId, String prefix, String pathToObserve) {
        this.collectorId = collectorId;
        this.dataPath = Helpers.getAppDataFolder();
        this.dataFilePrefix = prefix;
        this.pathObserve = pathToObserve;

        // Check if path to observe exists. If not, create it.
        if (!Helpers.ifDirectory(pathToObserve)) {
            Helpers.createDirectory(pathToObserve);
        }

        this.lastSeenFilename = dataPath + dataFilePrefix + collectorId + ".out";
    }

    /**
     * Reads file, where the name of last processed nfcapd is saved.
     * Returns filename of last processed nfcapd file.
     *
     * @return String Last processed nfcapd file.
     */
    @Nullable
    private String getLastSeen() {
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader(
                            new File(lastSeenFilename)
                    )
            );
            String lastSeenFile = bufferedReader.readLine();
            bufferedReader.close();

            logger.debug("Last seen netflow file " + lastSeenFile);

            return lastSeenFile;
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException. Returning null.");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error("IOException. Returning null.");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * Updates file, where the name of last processed nfcapd is saved.
     *
     * @param lastFilename Last new nfcapd file scanned.
     */
    private void updateLastSeen(String lastFilename) {
        logger.debug("Updating last seen netflow file with record " + lastFilename);

        try {
            BufferedWriter bufferedWriter = new BufferedWriter(
                    new FileWriter(
                            new File(lastSeenFilename)
                    )
            );
            bufferedWriter.write(lastFilename + "\n");
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            logger.error("IOException. Returning null.");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }
    }

    /**
     * Scans a directory, for new created files. Retrieves all files in
     * directory and compares their name with the saved one. If their
     * names are bigger compared to the saved one, it considers them
     * as new. The names of all new files will be returned in an Arraylist.
     * It also updates the file, where the name of last processed is stored.
     *
     * @return ArrayList(String) The names of new detected files.
     */
    public ArrayList<String> filterDirectory() {
        ArrayList<String> addedFiles = new ArrayList<>();
        File dir = new File(pathObserve);
        // Get last processed filename.
        String lastFilename = getLastSeen();

        logger.debug("===============================================================================================");

        // List all files in the directory.
        String[] existingFiles = dir.list();
        if (lastFilename != null && existingFiles != null) {
            for (String f : existingFiles) {
                logger.debug("Filtering files in directory " + pathObserve);

                // Filter files. Every file with name bigger than last seen will be added
                // in new files list. This will work in nfcapd files, as they have a standard
                // prefix, and then a date follows. Also, filter .csv files converted from
                // .nfcapd files, using prefix and postfix.
                if (f.compareTo(lastFilename) > 0 && f.startsWith("nfcapd.") && f.endsWith(".csv")) {
                    logger.debug("Adding file " + f);
                    addedFiles.add(f);
                } else {
                    logger.debug("Ignoring file " + f);
                }
            }
        } else if (existingFiles != null) {
            // If last processed file cannot be defined, add all
            // files in the directory in processing pipeline, that
            // are converted csv from nfcapd files.
            for (String f : existingFiles) {
                if (f.startsWith("nfcapd") && f.endsWith("csv")) {
                    logger.debug("Adding file " + f);
                    addedFiles.add(f);
                } else {
                    logger.debug("Ignoring file " + f);
                }
            }
            logger.debug("New files for processing detected: " + addedFiles.size());
        }

        // Update the file with the last retrieved file.
        if (addedFiles.size() > 0) {
            updateLastSeen(addedFiles.get(addedFiles.size() - 1));
        }

        logger.debug("===============================================================================================");

        return addedFiles;
    }

    /**
     * Main method of service. It calls filterDirectory method every X seconds.
     */
    @Override
    public void run() {
        // Scan directory for new files.
        ArrayList<String> detectedFiles = filterDirectory();

        if (detectedFiles.size() > 0) {
            logger.info("Notifying observers about " + detectedFiles.size() + " new files.");

            // Notify all listeners
            for (FilesListener fl : listeners) {
                fl.getFilenames(detectedFiles);
            }
        }
    }

    /**
     * Register a new listener. Every class that implements FileListener
     * calling this function can be registered and receive new events.
     *
     * @param toAdd Class that implements FilesListener.
     */
    public void addListener(FilesListener toAdd) {
        listeners.add(toAdd);
    }

    /**
     * Remove a registered listener. This observer will stop
     * receiving events.
     *
     * @param toRemove Class that implements FilesListener.
     */
    public void removeListener(FilesListener toRemove) {
        listeners.remove(toRemove);
    }
}
