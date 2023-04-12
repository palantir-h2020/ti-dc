package sink;

import elastic.OpenSearchUtils;
import models.NetflowAgent;
import models.NetflowInput;
import models.NetflowRecord;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.apache.log4j.Logger;
import utils.Helpers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.io.FileWriter;
import java.io.IOException;

/**
 * ElasticSinkTask is a Task that receives raw and preprocessed netflow data
 * in csv format, converts them to json format and stores them to Elastic.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class ElasticSinkTask extends SinkTask {
    /**
     * Logger Instance.
     */
    private Logger logger = Logger.getLogger(ElasticSinkTask.class);
    /**
     * Object, that initializes connection with Elastic and does operations.
     */
    private OpenSearchUtils elasticFunctions;

    /**
     * HashMap(String, String) A hashmap contains all props passed in start() function.
     */
    private HashMap<String, String> config = new HashMap<String, String>();

    /**
     * A boolean variable, which defines if the raw incoming netflows, will be also inserted
     * to Elastic. Default: False
     */
    private boolean SINK_RAW_NETFLOW = false;

    /**
     * A variable that says if the code runs in benchmark mode.
     */
    private boolean BENCHMARK_MODE = false;

    /**
     * A variable that says if netflows are integrated with Zeek.
     */
    private boolean ZEEK_ENABLED = false;

    /**
     * Helper functions.
     */
    private Helpers helperFunctions = null;

    /**
     * Metadata about collector agent.
     */
    NetflowAgent agent = null;
    /**
     * Metadata about collector input.
     */
    NetflowInput input = null;

    /**
     * Start function of sink task. It creates an Elastic functions object,
     * load some required mapping files for conversion.
     *
     * @param props Map containing configuration properties.
     */
    @Override
    public void start(Map<String, String> props) {
        // Copy all props
        config.putAll(props);

        try {
            ZEEK_ENABLED = Boolean.parseBoolean(props.get("zeek.enabled"));
        }
        catch (Exception e) {
            logger.warn("Cannot read property zeek.enabled from file elastic-sink.properties.");
            logger.warn("Using default value (false). Netflows will be inserted without Zeek score.");

            ZEEK_ENABLED = false;
        }
        logger.info("ZEEK_ENABLED:" + ZEEK_ENABLED);

        try {
            BENCHMARK_MODE = Boolean.parseBoolean(props.get("benchmark_mode"));
        }
        catch (Exception e) {
            logger.warn("Cannot read property benchmark_mode from file elastic-sink.properties.");
            logger.warn("Using default value (false). The connector will not run in benchmark mode.");

            BENCHMARK_MODE = false;
        }
        logger.info("BENCHMARK_MODE:" + BENCHMARK_MODE);

        // Connector properties
        try {
            SINK_RAW_NETFLOW = Boolean.parseBoolean(props.get("connector.sink.netflow.raw"));
        } catch (Exception e) {
            logger.warn("Cannot read property connector.sink.netflow.raw from file elastic-sink.properties.");
            logger.warn("Using default value (false). Raw netflow data will not be ingested in Elastic.");

            SINK_RAW_NETFLOW = false;
        }
        logger.info("Raw Netflow Data will be ignored: " + !SINK_RAW_NETFLOW);

        // Create ElasticUtils object.
        logger.info("Establishing connection to Elastic in " + config.get(ElasticSinkConnector.ELASTIC_HOST_IP) + ":" + config.get(ElasticSinkConnector.ELASTIC_HOST_PORT));
        elasticFunctions = new OpenSearchUtils(
                config.get(ElasticSinkConnector.ELASTIC_HOST_IP),
                Integer.parseInt(config.get(ElasticSinkConnector.ELASTIC_HOST_PORT)),
                config.get(ElasticSinkConnector.ELASTIC_USERNAME),
                config.get(ElasticSinkConnector.ELASTIC_PASSWORD)
        );

        // Initialize Elastic indexes for raw & preprocessed netflows
        elasticFunctions.initializeElasticIndex(config.get(ElasticSinkConnector.RAW_NETFLOW_ELASTIC_INDEX));
        elasticFunctions.initializeElasticIndex(config.get(ElasticSinkConnector.PREPROCESSED_NETFLOW_ELASTIC_INDEX));

        // Create Helpers object for creating JSON object for Elasticsearch
        helperFunctions = new Helpers();

        // Initialize some metadata properties about collector, that will be
        // included in final object, that will be pushed in Elasticsearch.
        agent = new NetflowAgent(
                helperFunctions.getHostname(), helperFunctions.getIpAddress(), "kafka-connect", "2.8.0",
                helperFunctions.getOsInfo(), helperFunctions.isContainerized(), helperFunctions.getMacAddress(),
                helperFunctions.getHostArch()
        );
        input = new NetflowInput(
                "netflow"
        );
    }

    /**
     * Main task of sink connectors. Retrieves data from Kafka topics, converts them
     * from csv format to json format using mapping files and stores them to appropriate
     * Elastic index.
     *
     * @param sinkRecords Collection(SinkRecord) A Java collection, that contains all
     *                    retrieved records from Kafka topics.
     */
    public void put(Collection<SinkRecord> sinkRecords) {
        int counterRaw = 0;
        int counterPreprocessed = 0;

        for (SinkRecord record : sinkRecords) {
            if (BENCHMARK_MODE) {
                try {
                    FileWriter fw = new FileWriter("benchmarking.log", true);
                    fw.write(record.topic() + " " + record.key().toString() + " get-sink " + System.currentTimeMillis() + "\n");
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            if (record.topic().equals(config.get(ElasticSinkConnector.RAW_NETFLOW_KAFKA_TOPIC))) {
                if (SINK_RAW_NETFLOW) {
                    try {
                        NetflowRecord netflowRecord = new NetflowRecord(
                                record.key().toString(), record.value().toString(), agent, input
                        );

                        // Insert them to Elastic index
                        elasticFunctions.insertNetflowRecord(
                                record.key().toString(),
                                netflowRecord,
                                config.get(ElasticSinkConnector.RAW_NETFLOW_ELASTIC_INDEX)
                        );

                        if (BENCHMARK_MODE) {
                            try {
                                FileWriter fw = new FileWriter("benchmarking.log", true);
                                fw.write(record.topic() + " " + record.key().toString() + " send-elk " + System.currentTimeMillis() + "\n");
                                fw.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    catch (NullPointerException e) {
                        logger.error("Cannot convert raw record (" + record.key().toString() + ") " + record.value().toString());
                    }

                    counterRaw++;
                }
            } else if (record.topic().equals(config.get(ElasticSinkConnector.PREPROCESSED_NETFLOW_KAFKA_TOPIC))) {
                try {
                    NetflowRecord netflowRecord = new NetflowRecord(
                            record.key().toString(), record.value().toString(), agent, input
                    );

                    // Insert them to Elastic index
                    elasticFunctions.insertNetflowRecord(
                            record.key().toString(),
                            netflowRecord,
                            config.get(ElasticSinkConnector.PREPROCESSED_NETFLOW_ELASTIC_INDEX)
                    );

                    if (BENCHMARK_MODE) {
                        try {
                            FileWriter fw = new FileWriter("benchmarking.log", true);
                            fw.write(record.topic() + " " + record.key().toString() + " send-elk " + System.currentTimeMillis() + "\n");
                            fw.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (NullPointerException e) {
                    logger.error("Cannot convert preprocessed & anonymized record (" + record.key().toString() + ") " + record.value().toString());
                }

                counterPreprocessed++;
            }
        }
        logger.info("Retrieved " + sinkRecords.size() + " messages (" + counterRaw + " raw netflows, " + counterPreprocessed + " prepprocessed netflows)!");
    }

    /**
     * Stop sink task. Closes Elastic connection.
     */
    @Override
    public void stop() {
        logger.info("Closing elastic connection.");
        if (elasticFunctions != null) {
            elasticFunctions.close();
        }

        logger.info("Stopping netflow sink task.");
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
