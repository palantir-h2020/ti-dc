package utils;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers class. Contains static methods
 * that may be used from all classes.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class Helpers {
    /**
     * Logger Instance.
     */
    private static Logger logger = Logger.getLogger(Helpers.class);

    /**
     * Checks if OS is Windows.
     *
     * @return boolean True if OS is Windows, false otherwise.
     */
    public static boolean onWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    /**
     * Checks if OS is Linux-based.
     *
     * @return boolean True if OS is Linux, false otherwise.
     */
    public static boolean onLinux() {
        return SystemUtils.IS_OS_LINUX;
    }

    /**
     * Gets the path, where files will be stored for this application.
     *
     * @return String The path where application files will be stored.
     * If any error occurs, returns null.
     */
    @Nullable
    public static String getAppDataFolder() {
        if (onWindows()) {
            logger.info("Detected OS: Windows.");
            return System.getenv("LOCALAPPDATA") + "\\Temp\\Palantir\\";
        }
        if (onLinux()) {
            logger.info("Detected OS: Linux.");
            return "/var/tmp/Palantir/";
        }
        return null;
    }

    /**
     * Initializes any non-existing paths required for the application to work.
     * If paths do not exists, it creates them. If any error occurs, it exists.
     */
    public static void initializeDirs() {
        String appDataFolder = getAppDataFolder();

        if (appDataFolder == null) {
            logger.error("Could not specify data folder for application. Exiting with code 101.");
            System.exit(101);
        } else {
            if (!ifDirectory(appDataFolder)) {
                logger.debug("Directory " + appDataFolder + " does not exists.");
                if (createDirectory(appDataFolder)) {
                    logger.debug("Directory " + appDataFolder + " created.");
                } else {
                    logger.error("Directory " + appDataFolder + " could not be created. Exiting with code 102.");
                    System.exit(102);
                }
            }
        }
    }

    /**
     * Checks if a given path exists and is a directory.
     *
     * @param dir The given path to be checked.
     * @return boolean True if path exists and is directory.
     * False, otherwise.
     */
    public static boolean ifDirectory(String dir) {
        File f = new File(dir);
        return f.exists() && f.isDirectory();
    }

    /**
     * Checks if a given path exists and is a file.
     *
     * @param filename The given path to be checked.
     * @return boolean True if path exists and is a file.
     * False, otherwise.
     */
    public static boolean ifFile(String filename) {
        File f = new File(filename);
        return f.exists() && f.isFile();
    }

    /**
     * Creates a directory.
     *
     * @param dir The path to be created.
     * @return boolean True if directory successfully created.
     * False, otherwise
     */
    public static boolean createDirectory(String dir) {
        File f = new File(dir);
        return f.mkdirs();
    }

    /**
     * Deletes a specific file.
     *
     * @param filename The file to be deleted.
     * @return boolean True if file exists, is a file
     * and successfully deleted. False, otherwise.
     */
    public static boolean deleteFile(String filename) {
        File f = new File(filename);
        return f.exists() && f.isFile() && f.delete();
    }
}
