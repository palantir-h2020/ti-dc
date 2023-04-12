package netflow;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * NetflowUtils class. Contains static methods that may
 * be used for operations in netflow files (.csv).
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class NetflowUtils {
    /**
     * Logger Instance.
     */
    private static Logger logger = Logger.getLogger(utils.Helpers.class);

    /**
     * Create a Kafka SourceRecord for each record in a .csv file. Returns an
     * Arraylist with these SourceRecords.
     *
     * @param csvFile        String Filename of .csv to be parsed.
     * @param KAFKA_TOPIC    String Kafka topic that SourceRecord will be sent.
     * @param keyPrefix      String A prefix for each record's key. Keys are prefix
     *                       plus a counter. prefix is
     *                       [collectorId]_[filename]_[counter].
     * @param benchmark_mode String Variable that says if the code runs in benchmarking mode.
     * @return ArrayList(SourceRecord) A list, which contains all created
     *         SourceRecords.
     */
    public static ArrayList<SourceRecord> loadNetflowCsv(String csvFile, String KAFKA_TOPIC, String keyPrefix, String benchmark_mode) {
        // Store all created records for Kafka.
        ArrayList<SourceRecord> netflowRecords = new ArrayList<SourceRecord>();

        try {
            // Open given file.
            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader(
                            new File(csvFile)
                    )
            );

            int counter = 0;
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                // Ignore "No matched flows" & empty lines
                if(!(line.trim().equals("")) && !(line.trim().equals("No matched flows")))  {
                    // Create a new Source Record.
                    // Add 8 new fields in data collected from nfcapd. Integration with SDA platform
                    netflowRecords.add(createSourceRecord(
                            KAFKA_TOPIC,
                            keyPrefix + counter,
                            line + ",0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0"
                    ));

                    if (benchmark_mode.equals("true")) {
                        try {
                            FileWriter fw = new FileWriter("benchmarking.log", true);
                            fw.write(keyPrefix + counter + " add-netflow " + System.currentTimeMillis() + "\n");
                            fw.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // Increase key counter.
                    counter++;
                }
            }

            // Close file stream.
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException. Returning arraylist with successfully processed source records so far.");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error("IOException. Returning arraylist with successfully processed source records so far.");
            logger.error(e.getCause());
            logger.error(e.getMessage());
        }

        return netflowRecords;
    }

    /**
     * Create a SourceRecord from a .csv line. Set STRING_SCHEMA for
     * both key and value serializers and deserializers.
     *
     * @param KAFKA_TOPIC String Kafka topic to be sent.
     * @param key         String Key for this record in Kafka.
     * @param value       String Value of this record in Kafka.
     * @return SourceRecord A SourceRecord object to be sent in Kafka.
     */
    private static SourceRecord createSourceRecord(String KAFKA_TOPIC, String key, String value) {
        return new SourceRecord(
                Collections.singletonMap("", ""), Collections.singletonMap("", ""),
                KAFKA_TOPIC,
                Schema.STRING_SCHEMA, key,
                Schema.STRING_SCHEMA, value);
    }
}
