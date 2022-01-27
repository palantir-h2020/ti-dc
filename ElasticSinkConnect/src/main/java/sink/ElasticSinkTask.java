package sink;

import elastic.ElasticUtils;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import utils.CsvRecordSchema;
import utils.ElasticJsonObject;
import utils.Helpers;

import java.io.File;
import java.net.URISyntaxException;
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
     * An object responsible for storing information about the columns in csv schema (raw netflow schema)
     * (name, description, type). It also parses a csv records, convertes the fields that required
     * convertions a returns a Hashmap with key the csv column name and value its value.
     */
    CsvRecordSchema csvRecordSchemaRaw;
    /**
     * An object responsible for storing information about the columns in csv schema (preprocessed
     * netflow schema) (name, description, type). It also parses a csv records, convertes the fields
     * that required convertions a returns a Hashmap with key the csv column name and value its value.
     */
    CsvRecordSchema csvRecordSchemaPreprocessed;
    /**
     * An object keeping information about the JSON schema (raw netflow records),
     * that will be used for storing records in Elastic. It loads a JSON file, which has information
     * about the mapping between the input csv schema and the output JSON schema for Elastic. It converts
     * a csv record to an Elastic object with the proper schema. It also contains some helper functions.
     */

    ElasticJsonObject elasticJsonObjectRaw;
    /**
     * An object keeping information about the JSON schema (preprocessed netflow records),
     * that will be used for storing records in Elastic. It loads a JSON file, which has information
     * about the mapping between the input csv schema and the output JSON schema for Elastic. It converts
     * a csv record to an Elastic object with the proper schema. It also contains some helper functions.
     */
    ElasticJsonObject elasticJsonObjectPreprocessed;
    /**
     * Logger Instance.
     */
    private Logger logger = Logger.getLogger(ElasticSinkTask.class);
    /**
     * Object, that initializes connection with Elastic and does operations.
     */
    private ElasticUtils elasticFunctions;

    /**
     * String Kafka topics, where records will be collected from.
     */
    private String[] KAFKA_TOPICS;

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
    private String BENCHMARK_MODE = "false";

    /**
     * Start function of sink task. It creates an Elastic functions object,
     * load some required mapping files for conversion.
     *
     * @param props Map containing configuration properties.
     */
    @Override
    public void start(Map<String, String> props) {
        // Kafka properties
        String KAFKA_SINK_TOPICS = props.get("topics");

        // Elastic properties
        String ELASTIC_IP = props.get("elastic.host.ip");
        int ELASTIC_PORT = Integer.parseInt(props.get("elastic.host.port"));
        String ELASTIC_USERNAME = props.get("elastic.authentication.username");
        String ELASTIC_PASSWORD = props.get("elastic.authentication.password");

        BENCHMARK_MODE = props.get("benchmark_mode");
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

        // Split Kafka topics into an array
        KAFKA_TOPICS = KAFKA_SINK_TOPICS.split(",");

        // Create ElasticUtils object.
        logger.info("Establishing connection to Elastic in " + ELASTIC_IP + ":" + ELASTIC_PORT);
        elasticFunctions = new ElasticUtils(
                ELASTIC_IP,
                ELASTIC_PORT,
                ELASTIC_USERNAME,
                ELASTIC_PASSWORD
        );

        // Create a csv parsers and an Elastic JSON converter for raw
        // (if enabled) netflow records
        if (SINK_RAW_NETFLOW) {
            csvRecordSchemaRaw = new CsvRecordSchema(true);
            elasticJsonObjectRaw = new ElasticJsonObject(true);
        }

        // Create a csv parsers and an Elastic JSON converter for preprocessed
        // netflow records
        csvRecordSchemaPreprocessed = new CsvRecordSchema(false);
        elasticJsonObjectPreprocessed = new ElasticJsonObject(false);

        // Copy all props
        config.putAll(props);
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
            
            if (BENCHMARK_MODE.equals("true")) {
                try {
                    FileWriter fw = new FileWriter("benchmarking.log", true);
                    fw.write(record.topic() + " " + record.key().toString() + " get-sink " + System.currentTimeMillis() + "\n");
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            if (record.topic().equals(config.get("kafka.topic.sink.netflow.raw"))) {
                counterRaw++;

                if (SINK_RAW_NETFLOW) {
                    // Raw netflow data. Parse and convert to Elastic JSON (if enabled)
                    HashMap<String, Object> recordMap = csvRecordSchemaRaw.parseRecord(record.value().toString());
                    try {
                        JSONObject docRaw = elasticJsonObjectRaw.createRecord(recordMap);

                        // Insert them to Elastic index
                        String elasticIndex = config.get("elastic.index.netflow.raw");
                        elasticFunctions.insertJsonDoc(docRaw.toString(), record.key().toString(), elasticIndex);

                        if (BENCHMARK_MODE.equals("true")) {
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
                        logger.error("Cannot convert raw record " + recordMap.toString());
                    }
                }
            } else if (record.topic().equals(config.get("kafka.topic.sink.netflow.preprocessed"))) {
                counterPreprocessed++;
                // Preprocessed netflow data. Parse and convert to Elastic JSON.
                HashMap<String, Object> recordMap = csvRecordSchemaPreprocessed.parseRecord(record.value().toString());

                try {
                    JSONObject docPreprocessed = elasticJsonObjectPreprocessed.createRecord(recordMap);

                    // Insert them to Elastic index
                    String elasticIndex = config.get("elastic.index.netflow.preprocessed");
                    elasticFunctions.insertJsonDoc(docPreprocessed.toString(), record.key().toString(), elasticIndex);

                    if (BENCHMARK_MODE.equals("true")) {
                        try {
                            FileWriter fw = new FileWriter("benchmarking.log", true);
                            fw.write(record.topic() + " " + record.key().toString() + " send-elk "
                                    + System.currentTimeMillis() + "\n");
                            fw.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                catch (NullPointerException e) {
                    logger.error("Cannot convert preprocessed & anonymized record " + recordMap.toString());
                }
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
