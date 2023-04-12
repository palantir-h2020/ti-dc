package sink;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

/**
 * ElasticSinkConnector implements the connector interface
 * to retrieve csv data from Kafka, convert them to JSON format
 * using a mapping file, and stores them in the appropriate
 * index in Elastic.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class ElasticSinkConnector extends SinkConnector {
    /**
     * Ip of the Elastic host.
     * Default: localhost
     */
    public static final String ELASTIC_HOST_IP = "elastic.host.ip";

    // Config param groups
    /**
     * Port, where Elastic is listenning.
     * Default: 9200
     */
    public static final String ELASTIC_HOST_PORT = "elastic.host.port";
    /**
     * Username for Elastic authentication.
     * Default: admin
     */
    public static final String ELASTIC_USERNAME = "elastic.authentication.username";
    /**
     * Password for Elastic authentication.
     * Default: admin
     */
    public static final String ELASTIC_PASSWORD = "elastic.authentication.password";

    // Elastic properties
    /**
     * Elastic indexes for syslog data. Two indexes are required for raw and
     * preprocessed syslog data. Comma separated.
     * Default: syslog-raw-index,syslog-preprocessed-index
     */
    public static final String ELASTIC_INDEXES = "elastic.indexes.syslog";
    /**
     * Elastic index for raw syslog data.
     * Default: syslog-raw-index
     */
    public static final String RAW_SYSLOG_ELASTIC_INDEX = "elastic.index.syslog.raw";
    /**
     * Elastic index for preprocessed syslog data.
     * Default: syslog-preprocessed-index
     */
    public static final String PREPROCESSED_SYSLOG_ELASTIC_INDEX = "elastic.index.syslog.preprocessed";
    /**
     * Number of shards for new Elastic indexes.
     * Default: 1
     */
    public static final String ELASTIC_INDEX_SHARDS = "elastic.indexes.shards";
    /**
     * Number of paritions for new Elastic indexes.
     * Default: 1
     */
    public static final String ELASTIC_INDEX_PARTITIONS = "elastic.indexes.partitions";
    /**
     * Kafka topics, from where data will be retrieved to be sent in Elastic.
     * For syslog collection two topics are required, for both raw and
     * preprocessed syslog data. Topics will be comma separated.
     * Default: syslog-raw-connector,syslog-preprocessed-connector
     */
    public static final String KAFKA_SINK_TOPICS = "topics";
    /**
     * Kafka topic, where raw syslog data are ingested.
     * Default: syslog-raw-connector
     */
    public static final String RAW_SYSLOG_KAFKA_TOPIC = "kafka.topic.sink.syslog.raw";
    /**
     * Kafka topic, where preprocessed syslog data are ingested.
     * Default: syslog-preprocessed-connector
     */
    public static final String PREPROCESSED_SYSLOG_KAFKA_TOPIC = "kafka.topic.sink.syslog.preprocessed";
    /**
     * Flag, which controls if connector will also sink raw syslog data to Elastic or not.
     * Default: false
     */
    public static final String SINK_RAW_SYSLOG = "connector.sink.syslog.raw";

    /**
     * A variable that says if the code runs in benchmark mode.
     */
    public static final String BENCHMARK_MODE = "benchmark_mode";

    // kafka properties
    
    /**
     * Data and directories params group.
     */
    private static final String ELASTIC_PARAM_GROUP = "Elastic Properties";
    /**
     * Kafka params group.
     */
    private static final String KAFKA_PARAM_GROUP = "Kafka Properties";

    private static final String OTHER_PARAM_GROUP = "Other Properties";

    /**
     * Connector params group.
     */
    private static final String CONNECTOR_PARAM_GROUP = "Connector Properties";

    // Connector properties
    /**
     * Object, storing configuration from .properties file.
     */
    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(ELASTIC_HOST_IP, ConfigDef.Type.STRING, "localhost", ConfigDef.Importance.HIGH,
                    "Elastic Host IP", ELASTIC_PARAM_GROUP, 0, ConfigDef.Width.SHORT,
                    "Elastic Host IP (required)")
            .define(ELASTIC_HOST_PORT, ConfigDef.Type.INT, 9200, ConfigDef.Importance.HIGH,
                    "Elastic Host Port", ELASTIC_PARAM_GROUP, 1, ConfigDef.Width.SHORT,
                    "Elastic Host Port (required)")
            .define(ELASTIC_USERNAME, ConfigDef.Type.STRING, "admin", ConfigDef.Importance.HIGH,
                    "Elastic's username", ELASTIC_PARAM_GROUP, 2, ConfigDef.Width.SHORT,
                    "Elastic's username (required)")
            .define(ELASTIC_PASSWORD, ConfigDef.Type.STRING, "admin", ConfigDef.Importance.HIGH,
                    "Elastic's password", ELASTIC_PARAM_GROUP, 3, ConfigDef.Width.SHORT,
                    "Elastic's password (required)")
            .define(ELASTIC_INDEXES, ConfigDef.Type.STRING, "syslog-raw-index,syslog-preprocessed-index",
                    ConfigDef.Importance.HIGH, "Elastic Indexes (comma-separated)", ELASTIC_PARAM_GROUP,
                    4, ConfigDef.Width.SHORT, "Elastic Indexes (required)")
            .define(RAW_SYSLOG_ELASTIC_INDEX, ConfigDef.Type.STRING, "syslog-raw-index",
                    ConfigDef.Importance.HIGH, "Elastic index for raw syslog data", ELASTIC_PARAM_GROUP,
                    5, ConfigDef.Width.SHORT, "Elastic index for raw syslog data (required)")
            .define(PREPROCESSED_SYSLOG_ELASTIC_INDEX, ConfigDef.Type.STRING, "syslog-preprocessed-index",
                    ConfigDef.Importance.HIGH, "Elastic index for preprocessed syslog data",
                    ELASTIC_PARAM_GROUP, 6, ConfigDef.Width.SHORT,
                    "Elastic index for preprocessed syslog data (required)")
            .define(ELASTIC_INDEX_SHARDS, ConfigDef.Type.INT, 1, ConfigDef.Importance.HIGH,
                    "Elastic index shards", ELASTIC_PARAM_GROUP, 7, ConfigDef.Width.SHORT,
                    "Number of shards for new Elastic indexes (required)")
            .define(ELASTIC_INDEX_PARTITIONS, ConfigDef.Type.INT, 1, ConfigDef.Importance.HIGH,
                    "Elastic index partitions", ELASTIC_PARAM_GROUP, 8, ConfigDef.Width.SHORT,
                    "Number of partitions for new Elastic indexes (required)")
            .define(KAFKA_SINK_TOPICS, ConfigDef.Type.STRING,
                    "syslog-raw-connector,syslog-preprocessed-connector", ConfigDef.Importance.HIGH,
                    "Kafka topics for reading data from (comma separated)", KAFKA_PARAM_GROUP,
                    0, ConfigDef.Width.SHORT, "Kafka topics for reading data from (required).")
            .define(RAW_SYSLOG_KAFKA_TOPIC, ConfigDef.Type.STRING, "syslog-raw-connector",
                    ConfigDef.Importance.HIGH, "Kafka topics for reading raw data from (comma separated)",
                    KAFKA_PARAM_GROUP, 1, ConfigDef.Width.SHORT,
                    "Kafka topics for reading raw data from (required).")
            .define(PREPROCESSED_SYSLOG_KAFKA_TOPIC, ConfigDef.Type.STRING, "syslog-preprocessed-connector",
                    ConfigDef.Importance.HIGH,
                    "Kafka topics for reading preprocessed data from (comma separated)", KAFKA_PARAM_GROUP,
                    2, ConfigDef.Width.SHORT,
                    "Kafka topics for reading preprocessed data from (required).")
            .define(SINK_RAW_SYSLOG, ConfigDef.Type.STRING, "false", ConfigDef.Importance.HIGH,
                    "Sink raw syslog data or not", CONNECTOR_PARAM_GROUP, 0, ConfigDef.Width.SHORT,
                    "Sink raw syslog data or not (required).")
            .define(BENCHMARK_MODE, ConfigDef.Type.STRING, "false", ConfigDef.Importance.HIGH,
                    "Run Kafka Source Connector in benchmark mode (Default: false)", OTHER_PARAM_GROUP, 0, ConfigDef.Width.SHORT,
                    "Run Kafka Source Connector in benchmark mode (optional).");
    /**
     * Map, that will store configuration from configDef object, after validation.
     */
    private final Map<String, String> config = new HashMap<String, String>();
    /**
     * Logger Instance.
     */
    private Logger logger = Logger.getLogger(ElasticSinkConnector.class);

    /**
     * Method, that will start sink connector. It will load logger properties
     * and parse configuration.
     *
     * @param props Configuration properties.
     */
    @Override
    public void start(Map<String, String> props) {
        // Load logger properties. If anything goes wrong, use some default logger values.
        try {
            Properties loggerProps = new Properties();
            loggerProps.load(ElasticSinkConnector.class.getResourceAsStream("/log4j.properties"));
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

        logger.info("Starting Elastic sink connector.");
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
        return ElasticSinkTask.class;
    }

    /**
     * Stop source connector.
     */
    @Override
    public void stop() {
        logger.info("Stopping Syslog Kafka source connector.");
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
