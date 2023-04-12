package source;

import java.io.IOException;
import java.util.*;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import utils.Helpers;

/**
 * NetflowSourceConnector implements the connector interface
 * to retrieve netflow data from filesystem and send it to Kafka.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class NetflowSourceConnector extends SourceConnector {
    /**
     * Id of the collector. This must be unique among all collectors.
     * If no id is specified in .properties file a random number in
     * range [100-500) is selected as id.
     */
    public static final String COLLECTOR_ID = "collector.id";

    // Config params
    /**
     * Prefix for file, that will store the last seen netflow file.
     * Default: netflow-cache-
     */
    public static final String DATA_DIR_FILE_PREFIX = "data.dir.file.prefix";
    /**
     * Directory to observe for new files. Each connector will be responsible
     * for a specific folder. Default: ./
     */
    public static final String DATA_DIR_OBSERVE = "filewatcher.dir.observe";
    /**
     * Time interval (seconds), that FileWatcher service will look for new files.
     * Default: 5 seconds.
     */
    public static final String FILEWATCHER_INTERVAL_S = "filewatcher.interval.s";
    /**
     * Kafka topic, where collected netflows will be sent. Can support multiple
     * topics, comma separated. Default: source-connector.
     */
    public static final String KAFKA_SOURCE_TOPIC = "kafka.topic.source";

    /**
     * A variable that says if the code runs in benchmark mode.
     */
    public static final String BENCHMARK_MODE = "benchmark_mode";

    /**
     * Tenant Id, for supporting multiple tenants. This must be unique among tenants.
     * If not provided, -1 will be used by default
     */
    public static final String TENANT_ID = "tenant.id";

    // Collector properties
    /**
     * Collector params group.
     */
    private static final String COLLECTOR_PARAM_GROUP = "Collector Properties";

    // Data paths properties
    /**
     * Data and directories params group.
     */
    private static final String DATA_PARAM_GROUP = "Data Properties";

    // Filewatcher service properties
    /**
     * FileWatcher service param groupd.
     */
    private static final String FILEWATCHER_PARAM_GROUP = "FileWatcher Properties";
    /**
     * Kafka params group.
     */
    private static final String KAFKA_PARAM_GROUP = "Kafka Properties";

    /**
     * Other params.
     */
    private static final String OTHER_PARAM_GROUP = "Other Properties";
    
    /**
     * Tenant params.
     */
    private static final String TENANT_PARAM_GROUP = "Tenant Properties";

    // kafka properties
    /**
     * Object, storing configuration from .properties file.
     */
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(COLLECTOR_ID, ConfigDef.Type.INT, new Random().nextInt(400) + 100,
                    ConfigDef.Importance.HIGH, "Collector Id", COLLECTOR_PARAM_GROUP, 0,
                    ConfigDef.Width.SHORT, "Collector Id (required)")
            .define(DATA_DIR_FILE_PREFIX, ConfigDef.Type.STRING, "netflow-cache-", ConfigDef.Importance.HIGH,
                    "Prefix for files, storing last seen netflow file", DATA_PARAM_GROUP, 0,
                    ConfigDef.Width.SHORT, "Prefix for files, storing last seen netflow file (required)")
            .define(DATA_DIR_OBSERVE, ConfigDef.Type.STRING, "./", ConfigDef.Importance.HIGH,
                    "Directory to observe for new netflow files", FILEWATCHER_PARAM_GROUP, 0,
                    ConfigDef.Width.SHORT, "Directory to observe for new netflow files (required).")
            .define(FILEWATCHER_INTERVAL_S, ConfigDef.Type.INT, 60, ConfigDef.Importance.HIGH,
                    "Time interval (seconds) to look for new files", FILEWATCHER_PARAM_GROUP, 1,
                    ConfigDef.Width.SHORT, "Time interval (seconds) to look for new files (required).")
            .define(KAFKA_SOURCE_TOPIC, ConfigDef.Type.STRING, "source-connector", ConfigDef.Importance.HIGH,
                    "Kafka topics for streaming results (comma separated)", KAFKA_PARAM_GROUP,
                    0, ConfigDef.Width.SHORT, "Kafka topic for streaming results (required).")
            .define(BENCHMARK_MODE, ConfigDef.Type.STRING, "false", ConfigDef.Importance.HIGH,
                    "Run Kafka Source Connector in benchmark mode (Default: false)", OTHER_PARAM_GROUP, 
                    0, ConfigDef.Width.SHORT, "Run Kafka Source Connector in benchmark mode (optional).")
            .define(TENANT_ID, ConfigDef.Type.INT, -1,ConfigDef.Importance.HIGH, "Tenant Id", TENANT_PARAM_GROUP, 
                    0, ConfigDef.Width.SHORT, "Tenant Id (required)");
    /**
     * Map, that will store configuration from configDef object, after validation.
     */
    private final Map<String, String> config = new HashMap<String, String>();
    /**
     * Logger Instance.
     */
    private Logger logger = Logger.getLogger(NetflowSourceConnector.class);

    /**
     * Method, that will start source connector. It will load logger properties,
     * initialize required directories in filesystem and parse configuration.
     *
     * @param props Configuration properties.
     */
    @Override
    public void start(Map<String, String> props) {
        // Load logger properties. If anything goes wrong, use some default logger values.
        try {
            Properties loggerProps = new Properties();
            loggerProps.load(NetflowSourceConnector.class.getResourceAsStream("/log4j.properties"));
            loggerProps.setProperty("log4j.appender.fout.File", Helpers.getAppDataFolder() + "netflow-kafka-connector-0.log");
            PropertyConfigurator.configure(loggerProps);
        } catch (IOException e) {
            logger.error("Could not load logger properties. Some logger features will be disabled.");
        }

        // Load configuration
        logger.info("Loading configuration.");
        Map<String, Object> configParsed = CONFIG_DEF.parse(props);
        for (Map.Entry<String, Object> entry : configParsed.entrySet()) {
            this.config.put(entry.getKey(), entry.getValue().toString());
        }

        // Initialize required folders.
        logger.info("Initializing required directories.");
        Helpers.initializeDirs();

        logger.info("Starting netflow Kafka source connector.");
    }

    /**
     * Configure tasks for connector.
     *
     * @param maxTasks Maximum number of tasks, that will run.
     * @return List(Map[String, String]) A list with all tasks.
     */
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<Map<String, String>>();

        for (int i = 0; i < maxTasks; i++) {
            taskConfigs.add(new HashMap<String, String>(this.config));
        }

        return taskConfigs;
    }

    /**
     * Returns the class of connect task.
     *
     * @return Task Class of task.
     */
    @Override
    public Class<? extends Task> taskClass() {
        return NetflowSourceTask.class;
    }

    /**
     * Stop source connector.
     */
    @Override
    public void stop() {
        logger.info("Stopping netflow Kafka source connector.");
    }

    /**
     * Return connect configuration.
     *
     * @return ConfigDef Configuration loaded from .properties file.
     */
    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
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
}
