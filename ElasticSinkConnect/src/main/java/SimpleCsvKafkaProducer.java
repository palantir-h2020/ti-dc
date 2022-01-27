import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.*;
import java.util.Properties;

/**
 * SimpleCsvKafkaProducer A test class with main method to ingest some records from a netflow csv
 * file to Kafka.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class SimpleCsvKafkaProducer {
    public static void main(String[] args) throws IOException, InterruptedException {
        String topicName = "netflow-raw-connector";

        Properties props = new Properties();
        //props.put("bootstrap.servers", "52.166.49.104:9092");
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = new KafkaProducer<String, String>(props);

        String csvDir = "K:/elelments/SpaceHellasP/Apache Spot/nfcapd_sample/primeTel/csv/";
        File[] files = new File(csvDir).listFiles(obj -> obj.isFile() && obj.getName().endsWith(".csv"));

        int msg_counter = 0;
        for (int i = 0; i < files.length; i++) {
            System.out.println("Appending file " + (i + 1) + "/" + files.length);

            BufferedReader bufferedReader = new BufferedReader(new FileReader(files[i]));
            String line = bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                if (line.trim().equals("Summary")) {
                    continue;
                } else if (line.trim().equals("flows,bytes,packets,avg_bps,avg_pps,avg_bpp")) {
                    line = bufferedReader.readLine();
                    continue;
                }

                Thread.sleep(5000);
                producer.send(new ProducerRecord<String, String>(
                        topicName,
                        "test-netflow-message-" + msg_counter,
                        line
                ));
                producer.flush();

                msg_counter++;
            }
        }
        producer.close();
    }
}
