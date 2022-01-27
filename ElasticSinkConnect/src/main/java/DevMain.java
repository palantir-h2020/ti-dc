import elastic.ElasticUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONObject;
import utils.CsvRecordSchema;
import utils.ElasticJsonObject;
import utils.Helpers;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

/**
 * DevMain A test class with main method to test functions of Elastic library.
 * Main function reads from a Kafka topic (csv records) and sends them to Elastic.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class DevMain {
    static Logger logger = Logger.getLogger(DevMain.class);

    public static void main(String[] args) {
        // Load logger properties. If anything goes wrong, use some default logger values.
        try {
            Properties loggerProps = new Properties();
            loggerProps.load(DevMain.class.getResourceAsStream("/log4j.properties"));
            PropertyConfigurator.configure(loggerProps);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load properties file for application. If anything goes wrong, set default values.
        String appPropertiesFile = null;
        Properties appProps = new Properties();

        if (args.length > 0) {
            // Properties filename has been passed as argument
            appPropertiesFile = args[0];
        } else {
            // Default properties file
            appPropertiesFile = "elastic-sink.properties";
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
            appProps.setProperty("topics", "netflow-raw-connector,netflow-preprocessed-connector");
            appProps.setProperty("kafka.topic.sink.netflow.raw", "netflow-raw-connector,netflow-preprocessed-connector");
            appProps.setProperty("kafka.topic.sink.netflow.preprocessed", "netflow-raw-connector,netflow-preprocessed-connector");

            appProps.setProperty("elastic.host.ip", "10.0.19.116");
            appProps.setProperty("elastic.host.port", "9200");

            appProps.setProperty("elastic.authentication.username", "admin");
            appProps.setProperty("elastic.authentication.password", "admin");

            appProps.setProperty("elastic.indexes.shards", "1");
            appProps.setProperty("elastic.indexes.partitions", "1");

            appProps.setProperty("elastic.indexes.netflow", "netflow-raw-index,netflow-preprocessed-index");
            appProps.setProperty("elastic.indexes.netflow.raw", "netflow-raw-index");
            appProps.setProperty("elastic.indexes.netflow.preprocessed", "netflow-preprocessed-index");

            appProps.setProperty("connector.sink.netflow.raw", "false");
        }

        ElasticUtils elasticUtils = new ElasticUtils(
                appProps.getProperty("elastic.host.ip"),
                Integer.parseInt(appProps.getProperty("elastic.host.port")),
                appProps.getProperty("elastic.authentication.username"),
                appProps.getProperty("elastic.authentication.password")
        );

        String[] elasticIndexes = appProps.getProperty("elastic.indexes.netflow").split(",");
        for (int i = 0; i < elasticIndexes.length; i++) {
            elasticUtils.initializeElasticIndex(
                    elasticIndexes[i],
                    Integer.parseInt(appProps.getProperty("elastic.indexes.shards")),
                    Integer.parseInt(appProps.getProperty("elastic.indexes.partitions"))
            );
        }

        CsvRecordSchema csvRecordSchema = new CsvRecordSchema(true);
        ElasticJsonObject elasticJsonObject = new ElasticJsonObject(true);

        String devRecord1 = "2019-11-13 23:49:43,2019-11-13 23:49:43,0.000,31.13.92.14,217.27.34.92,443,61383,TCP,.A....,0,0,1,40,0,0,13,14,0,0,0,0,0,0,10.10.5.199,0.0.0.0,0,0,00:13:5f:f7:78:1a,00:00:00:00:00:00,6c:3b:6b:c4:a0:71,6c:3b:6b:c4:a0:6d,0-0-0,0-0-0,0-0-0,0-0-0,0-0-0,0-0-0,0-0-0,0-0-0,0-0-0,0-0-0,    0.000,    0.000,    0.000,10.10.5.1,0/0,1,2019-11-13 23:50:00.489";
        HashMap<String, Object> recordMap = csvRecordSchema.parseRecord(devRecord1);

        JSONObject elasticJson = elasticJsonObject.createRecord(recordMap);
        System.out.println(elasticJson);

        System.exit(0);

        /*
        HashMap<String, String> rawMappingFile = Helpers.loadMappingFile(true, true);
        HashMap<String, String> preprocessedMappingFile = Helpers.loadMappingFile(false, true);

        //String kafkaBootstrapServers = "52.166.49.104:9092";
        String kafkaBootstrapServers = "localhost:9092";
        Consumer<String, String> kafkaConsumer = createConsumer(kafkaBootstrapServers, appProps.getProperty("topics"));
        while (true) {
            final ConsumerRecords<String, String> consumerRecords =
                    kafkaConsumer.poll(Duration.ofSeconds(1));

            consumerRecords.forEach(record -> {
                if(record.topic().equals(appProps.getProperty("kafka.topic.sink.netflow.raw"))) {
                    // Raw data. Input: csv file
                    JSONObject doc = Helpers.parseCsvToJson(record.value(), rawMappingFile);
                    String elasticIndex = appProps.getProperty("elastic.index.netflow.raw");

                    elasticUtils.insertJsonDoc(doc.toString(), elasticIndex);
                }
                else if(record.topic().equals(appProps.getProperty("kafka.topic.sink.netflow.preprocessed"))) {
                    // Preprocessed data. Input: Spark App
                    JSONObject doc = Helpers.parseCsvToJson(record.value(), preprocessedMappingFile);
                    String elasticIndex = appProps.getProperty("elastic.index.netflow.preprocessed");

                    elasticUtils.insertJsonDoc(doc.toString(), elasticIndex);
                }
            });

            kafkaConsumer.commitSync();
        }
        */
    }

    static Consumer<String, String> createConsumer(String kafkaBootstrapServers, String kafkaTopics) {
        String[] topics = kafkaTopics.split(",");

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                "KafkaExampleConsumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());

        // Create the consumer using props.
        final Consumer<String, String> consumer = new KafkaConsumer<>(props);

        // Subscribe to the topic.
        consumer.subscribe(Arrays.asList(topics));
        return consumer;
    }
}
