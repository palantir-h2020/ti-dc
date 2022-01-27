package utils;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Helpers class. Contains static methods
 * that may be used from all classes.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class Helpers {
    /**
     * Logger Instance.
     */
    private static Logger logger = Logger.getLogger(Helpers.class);

    /**
     * Convert a csv record to a JSON object. Read mapping between columns index
     * and names from loaded mapping file.
     *
     * @param record      String The csv line to be converted
     * @param mappingFile HashMap(String, String) A file that contains mapping
     *                    between column indexes and names.
     * @return JSONObject A json object, where keys are the columns' names from
     * mapping file and values the columns from record.
     */
    public static JSONObject parseCsvToJson(String record, HashMap<String, String> mappingFile) {
        JSONObject jsonRecord = new JSONObject();

        String fields[] = record.split(",");
        for (int i = 0; i < fields.length; i++) {
            jsonRecord.put(mappingFile.get(Integer.toString(i)), fields[i]);
        }

        return jsonRecord;
    }
}
