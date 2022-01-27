package source;

import interfaces.FilesListener;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import netflow.NetflowUtils;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.log4j.Logger;

import utils.FileWatcher;

import java.io.FileWriter;
import java.io.IOException;

/**
 * NetflowSourceTask is a Task that receives events with new netflow
 * files from FileWatcher service, process them and stores them in Kafka.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class NetflowSourceTask extends SourceTask implements FilesListener {
    /**
     * Logger Instance.
     */
    private Logger logger = Logger.getLogger(NetflowSourceConnector.class);

    /**
     * Service for scheduling execution of a Runnable class with a time interval.
     */
    private ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    /**
     * FileWatcher service, which observes a directory for new files and sends
     * events, when it detects new netflow files.
     */
    private FileWatcher fileWatcher;

    /**
     * ArrayList(String), where all new netflow filenames are kept.
     */
    private ArrayList<String> netflowFiles = new ArrayList<String>();

    /**
     * String Kafka topic, where collected records will be sent.
     */
    private String KAFKA_TOPIC;

    /**
     * A variable that says if the code runs in benchmark mode.
     */
    private String BENCHMARK_MODE = "false";

    /**
     * HashMap(String, String) A hashmap contains all props passed in start() function.
     */
    private HashMap<String, String> config = new HashMap<String, String>();

    /**
     * Start function of source task. It implements FilesListener observer.
     * It stars a FileWatcher service and register an observer to its events.
     *
     * @param props Map containing configuration properties.
     */
    @Override
    public void start(Map<String, String> props) {
        // Collector properties
        int COLLECTOR_ID = Integer.parseInt(props.get("collector.id"));
        // Data dir properties
        String DATA_DIR_FILE_PREFIX = props.get("data.dir.file.prefix");
        // Filewatcher properties
        String DATA_DIR_OBSERVE = props.get("filewatcher.dir.observe");
        int FILEWATCHER_INTERVAL_S = Integer.parseInt(props.get("filewatcher.interval.s"));

        BENCHMARK_MODE = props.get("benchmark_mode");

        KAFKA_TOPIC = props.get("kafka.topic.source");

        logger.info("BENCHMARK_MODE: " + BENCHMARK_MODE);
        logger.info(props.toString());

        // Create a new FileWatcher service.
        logger.info("Creating FileWatcher service.");
        fileWatcher = new FileWatcher(
                COLLECTOR_ID,
                DATA_DIR_FILE_PREFIX,
                DATA_DIR_OBSERVE
        );

        // Register DevMain class to FileWatcher events
        logger.info("Registering observer to FileWatcher events.");
        fileWatcher.addListener(this);

        // Schedule this service to run in background.
        logger.info("Starting FileWatcher service.");
        exec.scheduleAtFixedRate(
                fileWatcher,
                0,
                FILEWATCHER_INTERVAL_S,
                TimeUnit.SECONDS
        );

        // Copy all props
        config.putAll(props);
    }

    /**
     * Poll function of source task. It runs all time to create new SourceRecords
     * and sends them to Kafka. It check the list with new detected filenames. If
     * there are filenames, it retrieves the oldest one (and removes it from list),
     * it extracts netflow records using NetflowUtils. Each netflow record is a new
     * Kafka SourceRecord. All SourceRecords will be sent to Kafka for storage.
     *
     * @return List(SourceRecord) A list containing all SourceRecords that will be
     * sent to Kafka for storage.
     */
    @Override
    public List<SourceRecord> poll() {
        List<SourceRecord> records = new ArrayList<SourceRecord>();

        if (netflowFiles.size() > 0) {
            // Get oldest file for processing, and remove it from list.
            String filename = netflowFiles.remove(0);

            logger.info("Processing file " + filename + ".");

            if (BENCHMARK_MODE.equals("true")) {
                try {
                    FileWriter fw = new FileWriter("benchmarking.log", true);
                    fw.write(filename + " get-source " + System.currentTimeMillis() + "\n");
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Convert csv records to Source Records. If list with Source Records
            // is not empty, add them to the queue for Kafka.
            ArrayList<SourceRecord> csvRecords = NetflowUtils.loadNetflowCsv(
                    config.get("filewatcher.dir.observe") + "/" + filename,
                    KAFKA_TOPIC,
                    config.get("collector.id") + "_" + filename.replaceAll("\\.", "_") + "_",
                    BENCHMARK_MODE);
            logger.debug("Received " + csvRecords.size() + " records from NetflowUtils");
            if (csvRecords.size() > 0) {
                records.addAll(csvRecords);
                if (BENCHMARK_MODE.equals("true")) {
                    try {
                        FileWriter fw = new FileWriter("benchmarking.log", true);
                        fw.write(filename + " send-source " + System.currentTimeMillis() + "\n");
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //Stream their results through kafka-topic
        if (records.size() > 0) {
            return records;
        }

        return null;
    }

    /**
     * Stop source task. It unregisters listener for new files
     * and stops the FileWatcher service.
     */
    @Override
    public void stop() {
        logger.info("Unregistering observer from FileWatcher service.");
        //fileWatcher.removeListener(this);
        logger.info("Stopping FileWatcher service.");
        //exec.shutdownNow();

        logger.info("Stopping netflow source task.");
    }

    /**
     * Returns version.
     *
     * @return String version
     */
    @Override
    public String version() {
        return AppInfoParser.getVersion();
    }

    /**
     * Observer, that retrieves events from FileWatcher. Each event
     * is an ArrayList with detected filenames, that needs processing.
     *
     * @param files An ArrayList(String) with all new filenames detected.
     */
    @Override
    public void getFilenames(ArrayList<String> files) {
        if (files.size() > 0) {
            logger.info("Received an event with " + files.size() + " new files.");
            netflowFiles.addAll(files);
        }
    }
}
