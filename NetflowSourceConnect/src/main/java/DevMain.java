import interfaces.FilesListener;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import netflow.NetflowUtils;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import utils.FileWatcher;
import utils.Helpers;

/**
 * DevMain A test class with main method to test FileWatcher service
 * and FilesListener interface.
 * Main function starts FileWatcher service for a specific directory,
 * registers a listener to FileWatcher events and prints how many new
 * files are retrieved in each new event and their names, if there
 * are any new files. It also creates an ArrayList with Kafka SourceRecords.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class DevMain implements FilesListener {
    static Logger logger = Logger.getLogger(DevMain.class);

    public static void main(String[] args) throws InterruptedException {
        // Load logger properties. If anything goes wrong, use some default logger values.
        try {
            Properties loggerProps = new Properties();
            loggerProps.load(DevMain.class.getResourceAsStream("/log4j.properties"));
            loggerProps.setProperty("log4j.appender.fout.File", Helpers.getAppDataFolder() + "netflow-kafka-connector-0.log");
            PropertyConfigurator.configure(loggerProps);
        } catch (IOException e) {
            logger.error("Could not load logger properties. Some logger features will be disabled.");
        }

        // Load properties file for application. If anything goes wrong, set default values.
        String appPropertiesFile = null;
        Properties appProps = new Properties();
        if (Helpers.onWindows()) {
            // Load default Windows .properties file.
            appPropertiesFile = "/netflow-source-windows.properties";
        }
        if (Helpers.onLinux()) {
            // Load default Linux .properties file.
            appPropertiesFile = "/netflow-source-linux.properties";
        }
        if (args.length > 0) {
            // Properties filename has been passed as argument
            appPropertiesFile = args[0];
        }
        try {
            if (appPropertiesFile != null) {
                appProps.load(DevMain.class.getResourceAsStream(appPropertiesFile));
            } else {
                throw new NullPointerException("App Properties file is null.");
            }
        } catch (IOException | NullPointerException e) {
            logger.error("Cannot initialize app properties. Loading some default values.");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            // If any exception occurs, load some default values for app properties.
            // Set Id for this collector with random value in range [100,500)
            appProps.setProperty("collector.id", Integer.toString(new Random().nextInt(400) + 100));
            appProps.setProperty("collector.id", "");
            appProps.setProperty("data.dir.file.prefix", "netflow-cache-");
            if (Helpers.onWindows()) {
                appProps.setProperty("data.dir.observer", "C:\\palantir-demo\\");
            } else if (Helpers.onLinux()) {
                appProps.setProperty("data.dir.observer", "/home/palantir-demo/");
            } else {
                appProps.setProperty("data.dir.observer", "./");
            }
            appProps.setProperty("filewatcher.interval.s", "5");
        }

        // Initialize required folders.
        Helpers.initializeDirs();

        // Create a new FileWatcher service.
        FileWatcher fileWatcher = new FileWatcher(
                Integer.parseInt(appProps.getProperty("collector.id")),
                appProps.getProperty("data.dir.file.prefix"),
                appProps.getProperty("filewatcher.dir.observe")
        );

        // Register DevMain class to FileWatcher events
        DevMain dev = new DevMain();
        fileWatcher.addListener(dev);

        // Schedule this service to run in background.
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(
                fileWatcher,
                0,
                Integer.parseInt(appProps.getProperty("filewatcher.interval.s")),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void getFilenames(ArrayList<String> files) {
        if (files.size() > 0) {
            logger.debug("======================================");
            logger.debug("Retrieved an event with " + files.size() + " new files.");
            for (String f : files) {
                ArrayList<SourceRecord> csvRecords = NetflowUtils.loadNetflowCsv(
                        "C:\\palantir-demo\\" + "/" + f,
                        "",
                        "0_" + f.replaceAll("\\.", "_") + "_",
                        "");
                logger.debug("[" + f + "]: " + csvRecords.size() + " netflow records.");
            }
            logger.debug("======================================");
        }
    }
}
