package utils;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * CsvRecordSchema A class that loads csv records' columns names, types and
 * a short info about each one.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class CsvRecordSchema {
    /**
     * Logger instance.
     */
    private static Logger logger = Logger.getLogger(CsvRecordSchema.class);

    /**
     * A JSON object, where the csv schema (column names, types, info) will be stored to.
     */
    private JSONObject csvSchema = null;
    /**
     * A boolean variable to declare if the schema for raw csv records
     * will be loaded or not.
     */
    private boolean rawScehmaFile = false;

    /**
     * Constructor. Loads schema from .json file to JSON object.
     *
     * @param rawSchema Boolean A boolean that declares if the schema for raw
     *                  syslog records will be loaded or not.
     */
    public CsvRecordSchema(boolean rawSchema) {
        this.rawScehmaFile = rawSchema;
        this.csvSchema = loadSchemaFile(this.rawScehmaFile);

        logger.info("Raw Netflows: " + rawSchema);
        logger.info(csvSchema);
    }

    /**
     * Applies the loaded csv schema to a csv record (comma separated). It splits the csv
     * line (by comma) and for each field in the line it adds a record in a map, where
     * the key is the name of the csv column that is represented in this index and the value
     * is the value of this column, parsed o the correct data type.
     *
     * @param csvRecord String A comma separated string, which will be handled
     *                  as a csv line.
     * @return HashMap(String, Object) A map that contains the name of a column
     * and its value converted in the correct data type according to the schema.
     */
    public HashMap<String, Object> parseRecord(String csvRecord) {
        String[] recordCols = csvRecord.split(",");
        HashMap<String, Object> recordMap = new HashMap<String, Object>();

        for (int i = 0; i < recordCols.length; i++) {
            JSONObject colJson = this.getFieldByIndex(i);

            if (colJson.getString("type").trim().equals("Integer")) {
                // Put to map as int
                recordMap.put(colJson.getString("name"), Integer.parseInt(recordCols[i].trim()));
            } else if (colJson.getString("type").trim().equals("Double")) {
                // Put to map as double
                recordMap.put(colJson.getString("name"), Double.parseDouble(recordCols[i].trim()));
            } else {
                // Put to map as string
                recordMap.put(colJson.getString("name"), recordCols[i].trim());
            }
        }

        return recordMap;
    }

    /**
     * Returns a JSON from a JSON array, given its index in the array.
     *
     * @param fieldIdx Int The index of the requested JSON.
     * @return JSONObject The request JSON object from the JSON array.
     */
    private JSONObject getFieldByIndex(int fieldIdx) {
        return this.csvSchema.getJSONArray("csv_fields").getJSONObject(fieldIdx);
    }

    /**
     * Load the csv schema from a .json file into a JSON object. If the schema
     * file cannot be loaded a default schema will be returned.
     *
     * @param raw Boolean A boolean, which declares if the schema for
     *            raw syslog will be used or not.
     * @return JSONObject A JSON object, which contains all the csv schema.
     */
    public JSONObject loadSchemaFile(boolean raw) {
        // Preprocessed syslog data schema
        String resourceName = "csv-preprocessed-schema.json";
        if (raw) {
            // Raw syslog data schema
            resourceName = "csv-raw-schema.json";
        }

        try {
            InputStream is = new FileInputStream(new File(resourceName));

            JSONTokener tokener = new JSONTokener(is);
            JSONObject csvSchema = new JSONObject(tokener);

            return csvSchema;
        } catch (Exception e) {
            logger.error("FileNotFoundException. Something went wrong loading csv schema file (raw=" + raw + "). Returning a default predefined csv schema.");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            if (raw) {
                // Return default csv schema for raw syslog
                return new JSONObject("{\"csv_fields\":[{\"id\":0,\"name\":\"ts\",\"description\":\"Start Time - first seen\",\"type\":\"String\"},{\"id\":1,\"name\":\"te\",\"description\":\"End Time - last seen\",\"type\":\"String\"},{\"id\":2,\"name\":\"td\",\"description\":\"Duration\",\"type\":\"Double\"},{\"id\":3,\"name\":\"sa\",\"description\":\"Source Address\",\"type\":\"String\"},{\"id\":4,\"name\":\"da\",\"description\":\"Destination Address\",\"type\":\"String\"},{\"id\":5,\"name\":\"sp\",\"description\":\"Source Port\",\"type\":\"Integer\"},{\"id\":6,\"name\":\"dp\",\"description\":\"Destination Port\",\"type\":\"Integer\"},{\"id\":7,\"name\":\"pr\",\"description\":\"Protocol\",\"type\":\"String\"},{\"id\":8,\"name\":\"flg\",\"description\":\"TCP Flags\",\"type\":\"String\"},{\"id\":9,\"name\":\"fwd\",\"description\":\"Forwarding Status\",\"type\":\"Integer\"},{\"id\":10,\"name\":\"stos\",\"description\":\"Source Tos\",\"type\":\"Integer\"},{\"id\":11,\"name\":\"ipkt\",\"description\":\"Input Packets\",\"type\":\"Integer\"},{\"id\":12,\"name\":\"ibyt\",\"description\":\"Input Bytes\",\"type\":\"Integer\"},{\"id\":13,\"name\":\"opkt\",\"description\":\"Output Packets\",\"type\":\"Integer\"},{\"id\":14,\"name\":\"obyt\",\"description\":\"Output Bytes\",\"type\":\"Integer\"},{\"id\":15,\"name\":\"in\",\"description\":\"Input Interface num\",\"type\":\"Integer\"},{\"id\":16,\"name\":\"out\",\"description\":\"Output Interface num\",\"type\":\"Integer\"},{\"id\":17,\"name\":\"sas\",\"description\":\"Source AS\",\"type\":\"Integer\"},{\"id\":18,\"name\":\"das\",\"description\":\"Destination AS\",\"type\":\"Integer\"},{\"id\":19,\"name\":\"smk\",\"description\":\"Source mask\",\"type\":\"Integer\"},{\"id\":20,\"name\":\"dmk\",\"description\":\"Destination mask\",\"type\":\"Integer\"},{\"id\":21,\"name\":\"dtos\",\"description\":\"Destination Tos\",\"type\":\"Integer\"},{\"id\":22,\"name\":\"dir\",\"description\":\"Direction: ingress, egress\",\"type\":\"Integer\"},{\"id\":23,\"name\":\"nh\",\"description\":\"Next-hop IP Address\",\"type\":\"String\"},{\"id\":24,\"name\":\"nhb\",\"description\":\"BGP Next-hop IP Address\",\"type\":\"String\"},{\"id\":25,\"name\":\"svln\",\"description\":\"Src vlan label\",\"type\":\"Integer\"},{\"id\":26,\"name\":\"dvln\",\"description\":\"Dst vlan label\",\"type\":\"Integer\"},{\"id\":27,\"name\":\"ismc\",\"description\":\"Input Src Mac Addr\",\"type\":\"String\"},{\"id\":28,\"name\":\"odmc\",\"description\":\"Output Dst Mac Addr\",\"type\":\"String\"},{\"id\":29,\"name\":\"idmc\",\"description\":\"Input Dst Mac Addr\",\"type\":\"String\"},{\"id\":30,\"name\":\"osmc\",\"description\":\"Output Src Mac Addr\",\"type\":\"String\"},{\"id\":31,\"name\":\"mpls1\",\"description\":\"MPLS label 1\",\"type\":\"String\"},{\"id\":32,\"name\":\"mpls2\",\"description\":\"MPLS label 2\",\"type\":\"String\"},{\"id\":33,\"name\":\"mpls3\",\"description\":\"MPLS label 3\",\"type\":\"String\"},{\"id\":34,\"name\":\"mpls4\",\"description\":\"MPLS label 4\",\"type\":\"String\"},{\"id\":35,\"name\":\"mpls5\",\"description\":\"MPLS label 5\",\"type\":\"String\"},{\"id\":36,\"name\":\"mpls6\",\"description\":\"MPLS label 6\",\"type\":\"String\"},{\"id\":37,\"name\":\"mpls7\",\"description\":\"MPLS label 7\",\"type\":\"String\"},{\"id\":38,\"name\":\"mpls8\",\"description\":\"MPLS label 8\",\"type\":\"String\"},{\"id\":39,\"name\":\"mpls9\",\"description\":\"MPLS label 9\",\"type\":\"String\"},{\"id\":40,\"name\":\"mpls10\",\"description\":\"MPLS label 10\",\"type\":\"String\"},{\"id\":41,\"name\":\"cl\",\"description\":\"Client latency\",\"type\":\"Double\"},{\"id\":42,\"name\":\"sl\",\"description\":\"Server latency\",\"type\":\"Double\"},{\"id\":43,\"name\":\"al\",\"description\":\"Application latency\",\"type\":\"Double\"},{\"id\":44,\"name\":\"ra\",\"description\":\"Router IP Address\",\"type\":\"String\"},{\"id\":45,\"name\":\"eng\",\"description\":\"Engine Type\\/ID\",\"type\":\"String\"},{\"id\":46,\"name\":\"exid\",\"description\":\"Exporter ID\",\"type\":\"Integer\"},{\"id\":47,\"name\":\"tr\",\"description\":\"Time the flow was received by the collector\",\"type\":\"String\"}]}");
            } else {
                // Return default csv schema for preprocessed syslog
                return new JSONObject("{\"csv_fields\":[{\"name\":\"ts\",\"description\":\"Start Time - first seen\",\"id\":0,\"type\":\"String\"},{\"name\":\"te\",\"description\":\"End Time - last seen\",\"id\":1,\"type\":\"String\"},{\"name\":\"td\",\"description\":\"Duration\",\"id\":2,\"type\":\"Double\"},{\"name\":\"sa\",\"description\":\"Source Address\",\"id\":3,\"type\":\"String\"},{\"name\":\"da\",\"description\":\"Destination Address\",\"id\":4,\"type\":\"String\"},{\"name\":\"sp\",\"description\":\"Source Port\",\"id\":5,\"type\":\"Integer\"},{\"name\":\"dp\",\"description\":\"Destination Port\",\"id\":6,\"type\":\"Integer\"},{\"name\":\"pr\",\"description\":\"Protocol\",\"id\":7,\"type\":\"String\"},{\"name\":\"flg\",\"description\":\"TCP Flags\",\"id\":8,\"type\":\"String\"},{\"name\":\"fwd\",\"description\":\"Forwarding Status\",\"id\":9,\"type\":\"Integer\"},{\"name\":\"stos\",\"description\":\"Source Tos\",\"id\":10,\"type\":\"Integer\"},{\"name\":\"ipkt\",\"description\":\"Input Packets\",\"id\":11,\"type\":\"Integer\"},{\"name\":\"ibyt\",\"description\":\"Input Bytes\",\"id\":12,\"type\":\"Integer\"},{\"name\":\"opkt\",\"description\":\"Output Packets\",\"id\":13,\"type\":\"Integer\"},{\"name\":\"obyt\",\"description\":\"Output Bytes\",\"id\":14,\"type\":\"Integer\"},{\"name\":\"in\",\"description\":\"Input Interface num\",\"id\":15,\"type\":\"Integer\"},{\"name\":\"out\",\"description\":\"Output Interface num\",\"id\":16,\"type\":\"Integer\"},{\"name\":\"sas\",\"description\":\"Source AS\",\"id\":17,\"type\":\"Integer\"},{\"name\":\"das\",\"description\":\"Destination AS\",\"id\":18,\"type\":\"Integer\"},{\"name\":\"smk\",\"description\":\"Source mask\",\"id\":19,\"type\":\"Integer\"},{\"name\":\"dmk\",\"description\":\"Destination mask\",\"id\":20,\"type\":\"Integer\"},{\"name\":\"dtos\",\"description\":\"Destination Tos\",\"id\":21,\"type\":\"Integer\"},{\"name\":\"dir\",\"description\":\"Direction: ingress, egress\",\"id\":22,\"type\":\"Integer\"},{\"name\":\"nh\",\"description\":\"Next-hop IP Address\",\"id\":23,\"type\":\"String\"},{\"name\":\"nhb\",\"description\":\"BGP Next-hop IP Address\",\"id\":24,\"type\":\"String\"},{\"name\":\"svln\",\"description\":\"Src vlan label\",\"id\":25,\"type\":\"Integer\"},{\"name\":\"dvln\",\"description\":\"Dst vlan label\",\"id\":26,\"type\":\"Integer\"},{\"name\":\"ismc\",\"description\":\"Input Src Mac Addr\",\"id\":27,\"type\":\"String\"},{\"name\":\"odmc\",\"description\":\"Output Dst Mac Addr\",\"id\":28,\"type\":\"String\"},{\"name\":\"idmc\",\"description\":\"Input Dst Mac Addr\",\"id\":29,\"type\":\"String\"},{\"name\":\"osmc\",\"description\":\"Output Src Mac Addr\",\"id\":30,\"type\":\"String\"},{\"name\":\"mpls1\",\"description\":\"MPLS label 1\",\"id\":31,\"type\":\"String\"},{\"name\":\"mpls2\",\"description\":\"MPLS label 2\",\"id\":32,\"type\":\"String\"},{\"name\":\"mpls3\",\"description\":\"MPLS label 3\",\"id\":33,\"type\":\"String\"},{\"name\":\"mpls4\",\"description\":\"MPLS label 4\",\"id\":34,\"type\":\"String\"},{\"name\":\"mpls5\",\"description\":\"MPLS label 5\",\"id\":35,\"type\":\"String\"},{\"name\":\"mpls6\",\"description\":\"MPLS label 6\",\"id\":36,\"type\":\"String\"},{\"name\":\"mpls7\",\"description\":\"MPLS label 7\",\"id\":37,\"type\":\"String\"},{\"name\":\"mpls8\",\"description\":\"MPLS label 8\",\"id\":38,\"type\":\"String\"},{\"name\":\"mpls9\",\"description\":\"MPLS label 9\",\"id\":39,\"type\":\"String\"},{\"name\":\"mpls10\",\"description\":\"MPLS label 10\",\"id\":40,\"type\":\"String\"},{\"name\":\"cl\",\"description\":\"Client latency\",\"id\":41,\"type\":\"Double\"},{\"name\":\"sl\",\"description\":\"Server latency\",\"id\":42,\"type\":\"Double\"},{\"name\":\"al\",\"description\":\"Application latency\",\"id\":43,\"type\":\"Double\"},{\"name\":\"ra\",\"description\":\"Router IP Address\",\"id\":44,\"type\":\"String\"},{\"name\":\"eng\",\"description\":\"Engine Type/ID\",\"id\":45,\"type\":\"String\"},{\"name\":\"exid\",\"description\":\"Exporter ID\",\"id\":46,\"type\":\"Integer\"},{\"name\":\"tr\",\"description\":\"Time the flow was received by the collector\",\"id\":47,\"type\":\"String\"},{\"name\":\"tpkt\",\"description\":\"Total packets of the flow. If the flow is not bidirectional this will be the same as ipkt field.\",\"id\":48,\"type\":\"Integer\"},{\"name\":\"tbyt\",\"description\":\"Total bytes of the flow. If the flow is not bidirectional this will be the same as ibyt field\",\"id\":49,\"type\":\"Double\"},{\"name\":\"cp\",\"description\":\"Check if destination port, is a port used by common services. Its value is 1 if the port is a common used port, 0 otherwise.\",\"id\":50,\"type\":\"Integer\"},{\"name\":\"prtcp\",\"description\":\"Checks if the flow protocol is TCP or not. Value is 1 if it is, 0 otherwise.\",\"id\":51,\"type\":\"Integer\"},{\"name\":\"prudp\",\"description\":\"Checks if the flow protocol is UDP or not. Value is 1 if it is, 0 otherwise.\",\"id\":52,\"type\":\"Integer\"},{\"name\":\"pricmp\",\"description\":\"Checks if the flow protocol is ICMP or not. Value is 1 if it is, 0 otherwise.\",\"id\":53,\"type\":\"Integer\"},{\"name\":\"prigmp\",\"description\":\"Checks if the flow protocol is IGMP or not. Value is 1 if it is, 0 otherwise.\",\"id\":54,\"type\":\"Integer\"},{\"name\":\"prother\",\"description\":\"Checks if the flow protocol is not one of TCP, UDP, ICMP or IGMP. Value is 1 if it is not one of them, 0 otherwise.\",\"id\":55,\"type\":\"Integer\"},{\"name\":\"flga\",\"description\":\"Checks if TCP flag contains A. Value is 1 if it contains this, 0 otherwise.\",\"id\":56,\"type\":\"Integer\"},{\"name\":\"flgs\",\"description\":\"Checks if TCP flag contains S. Value is 1 if it contains this, 0 otherwise.\",\"id\":57,\"type\":\"Integer\"},{\"name\":\"flgf\",\"description\":\"Checks if TCP flag contains F. Value is 1 if it contains this, 0 otherwise.\",\"id\":58,\"type\":\"Integer\"},{\"name\":\"flgr\",\"description\":\"Checks if TCP flag contains R. Value is 1 if it contains this, 0 otherwise.\",\"id\":59,\"type\":\"Integer\"},{\"name\":\"flgp\",\"description\":\"Checks if TCP flag contains P. Value is 1 if it contains this, 0 otherwise.\",\"id\":60,\"type\":\"Integer\"},{\"name\":\"flgu\",\"description\":\"Checks if TCP flag contains U. Value is 1 if it contains this, 0 otherwise.\",\"id\":61,\"type\":\"Integer\"}]}");
            }
        }
    }
}
