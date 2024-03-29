package utils;

import com.rapid7.communityid.CommunityIdGenerator;
import com.rapid7.communityid.Protocol;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

/**
 * ElasticJsonObject class. Converts a Map object, which contains all info for a
 * csv record (column name, column value) into a JSON object, which follows the
 * proper schema in order to be ingested to Elastic. The mapping between the input
 * object and the output JSON object is happening using a .json file. This file contains
 * the category name and category sub-name, which each csv column is mapped to in order
 * for the Elastic JSON object to be created. This class, also contains some helper
 * function to convert the input record.
 *
 * @author Space Hellas S.A.
 * @version 0.1-SNAPSHOT
 * @since 0.1-SNAPSHOT
 */
public class ElasticJsonObject {
    /**
     * Logger Instance.
     */
    private static Logger logger = Logger.getLogger(ElasticJsonObject.class);

    /**
     * Boolean If the .json mapping file for raw syslogs will be loaded. (Default: False)
     */
    private boolean rawNetflowMappingFile = true;
    /**
     * A JSON object, which will store all the information about the mapping between the
     * csv record and the generated Elastic JSON object.
     */
    private JSONObject mappingJson = null;

    /**
     * An Arraylist, that contains which IPs will be labeled as internals and externals.
     */
    private ArrayList<String> internalIps = null;

    /**
     * Hostname of the computer that converts the csv records to Elastic JSON objects
     */
    private String hostname = null;
    /**
     * Host ID of the computer that converts the csv records to Elastic JSON objects.
     * This ID is produced by hashing the hostname.
     */
    private String hostId = null;
    /**
     * A JSON object containing information about the OS of this host.
     */
    private JSONObject osInfoJson = null;
    /**
     * A boolean value, showing if this host is running inside a container or not.
     */
    private boolean containerized = false;
    /**
     * IP address of this host.
     */
    private String hostIp = null;
    /**
     * MAC address of this host.
     */
    private String hostMac = null;
    /**
     * OS architecture of this host.
     */
    private String hostArch = null;
    /**
     * A JSON object, which stores information about the process that ingest data to Elastic.
     */
    private JSONObject agentJson = null;

    /**
     * A generator, which generates a community ID of each flow, based on its properties.
     */
    private CommunityIdGenerator networkCommunityIdGenerator = null;

    /**
     * Constructor
     *
     * @param raw Boolean If this object is responsible to convert and ingest raw syslog data or not.
     *            If this is set to false, then this object is responsible to convert and ingest
     *            preprocessed syslog records.
     */
    public ElasticJsonObject(boolean raw) {
        this.hostname = getHostname();
        this.hostId = hashHostname();
        this.osInfoJson = getOsInfo();
        this.containerized = isContainerized();
        this.hostIp = getIpAddress();
        this.hostMac = getMacAddress();
        this.hostArch = getHostArch();
        this.agentJson = initializeAgentJson();

        this.rawNetflowMappingFile = raw;
        // Load Mapping file
        this.mappingJson = loadMappingFile(this.rawNetflowMappingFile);
        this.internalIps = loadInternalSubnets();

        this.ianaProtocolNumbers = new IanaProtocolNumbers();
        this.networkCommunityIdGenerator = new CommunityIdGenerator();

        logger.info("Raw Netflows: " + raw);
        logger.info(mappingJson);
    }

    /**
     * Checks if the connector is running inside a container or not. The check is happening reading the file
     * /proc/1/cgroup and searching for a line that contains the value /docker. If this value is not found, or
     * any error occurs, it is considered that the connector is not running inside a container.
     *
     * @return Boolean True if the app runs in container, False otherwise.
     */
    public static Boolean isContainerized() {
        try (Stream<String> stream =
                     Files.lines(Paths.get("/proc/1/cgroup"))) {
            return stream.anyMatch(line -> line.contains("/docker"));
        } catch (IOException e) {
            return false;
        }
    }


    public JSONObject createRecord(HashMap<String, Object> csvMap) throws NullPointerException {
        JSONObject metadataJson = new JSONObject();
        metadataJson.put("beat", "filebeat");
        metadataJson.put("type", "_doc");
        metadataJson.put("version", "7.13.3");

        JSONObject flowJson = new JSONObject();
        flowJson.put("id", generateFlowId());
        flowJson.put("locality", "external");

        JSONObject sourceJson = new JSONObject();
        sourceJson.put("ip", csvMap.get(getJsonSubKey("source", "ip")).toString());
        sourceJson.put("locality", checkIpLocality(csvMap.get(getJsonSubKey("source", "ip")).toString()));
        sourceJson.put("port", Integer.parseInt(csvMap.get(getJsonSubKey("source", "port")).toString()));
        sourceJson.put("bytes", Double.parseDouble(csvMap.get(getJsonSubKey("source", "bytes")).toString()));
        sourceJson.put("packets", Integer.parseInt(csvMap.get(getJsonSubKey("source", "packets")).toString()));

        JSONArray relatedJsonArray = new JSONArray();
        relatedJsonArray.put(csvMap.get(getJsonSubKey("source", "ip")).toString());
        relatedJsonArray.put(csvMap.get(getJsonSubKey("destination", "ip")).toString());

        JSONObject relatedJson = new JSONObject();
        relatedJson.put("ip", relatedJsonArray);

        JSONObject syslogExporterJson = new JSONObject();
        syslogExporterJson.put("timestamp", convertDatetimeToMillis(csvMap.get(getJsonSubKey("timestamp", "start")).toString()));
        syslogExporterJson.put("uptime_millis", ((Long) ManagementFactory.getRuntimeMXBean().getUptime()));
        syslogExporterJson.put("address", hostIp);
        syslogExporterJson.put("engine_type", 1);
        syslogExporterJson.put("engine_id", 0);
        syslogExporterJson.put("sampling_interval", 0);
        syslogExporterJson.put("version", 5);

        JSONObject syslogJson = new JSONObject();
        syslogJson.put("destination_ipv4_prefix_length", Integer.parseInt(csvMap.get(getJsonSubKey("destination", "mask")).toString()));
        syslogJson.put("destination_transport_port", Integer.parseInt(csvMap.get(getJsonSubKey("destination", "port")).toString()));
        syslogJson.put("source_ipv4_address", csvMap.get(getJsonSubKey("source", "ip")).toString());
        syslogJson.put("type", "syslog_flow");
        syslogJson.put("destination_ipv4_address", csvMap.get(getJsonSubKey("destination", "ip")).toString());
        syslogJson.put("flow_start_sys_up_time", (
                (Long) ManagementFactory.getRuntimeMXBean().getUptime())
                - Math.round(Double.parseDouble(csvMap.get(getJsonSubKey("timestamp", "duration")).toString()))
        );
        syslogJson.put("octet_delta_count", Double.parseDouble(csvMap.get(getJsonSubKey("destination", "bytes")).toString()));
        syslogJson.put("ingress_interface", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "ingress")).toString()));
        syslogJson.put("exporter", syslogExporterJson);
        syslogJson.put("bgp_destination_as_number", csvMap.get(getJsonSubKey("syslog", "dstAs")).toString());
        syslogJson.put("bgp_source_as_number", csvMap.get(getJsonSubKey("syslog", "srcAs")).toString());
        syslogJson.put("egress_interface", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "egress")).toString()));
        syslogJson.put("source_ipv4_prefix_length", Integer.parseInt(csvMap.get(getJsonSubKey("source", "mask")).toString()));
        syslogJson.put("source_transport_port", Integer.parseInt(csvMap.get(getJsonSubKey("source", "port")).toString()));
        syslogJson.put("ip_next_hop_ipv4_address", csvMap.get(getJsonSubKey("syslog", "nextHop")).toString());
        syslogJson.put("tcp_control_bits", encodeTcpFlags(csvMap.get(getJsonSubKey("syslog", "tcpFlags")).toString()));
        syslogJson.put("protocol_identifier", ianaProtocolNumbers.getIanaNum(csvMap.get(getJsonSubKey("network", "protocol")).toString().toUpperCase()));
        syslogJson.put("ip_class_of_service", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "ipClassService")).toString()));
        syslogJson.put("flow_end_sys_up_time", (Long) ManagementFactory.getRuntimeMXBean().getUptime());
        syslogJson.put("packet_delta_count", Integer.parseInt(csvMap.get(getJsonSubKey("destination", "packets")).toString()));

        if (!rawNetflowMappingFile) {
            syslogJson.put("packet_total", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "packets")).toString()));
            syslogJson.put("bytes_total", Double.parseDouble(csvMap.get(getJsonSubKey("syslog", "bytes")).toString()));
            syslogJson.put("is_common_port", Integer.parseInt(csvMap.get(getJsonSubKey("destination", "commonPort")).toString()));
            syslogJson.put("is_protocol_tcp", Integer.parseInt(csvMap.get(getJsonSubKey("network", "protocolTcp")).toString()));
            syslogJson.put("is_protocol_udp", Integer.parseInt(csvMap.get(getJsonSubKey("network", "protocolUdp")).toString()));
            syslogJson.put("is_protocol_icmp", Integer.parseInt(csvMap.get(getJsonSubKey("network", "protocolIcmp")).toString()));
            syslogJson.put("is_protocol_igmp", Integer.parseInt(csvMap.get(getJsonSubKey("network", "protocolIgmp")).toString()));
            syslogJson.put("is_protocol_other", Integer.parseInt(csvMap.get(getJsonSubKey("network", "protocolOther")).toString()));
            syslogJson.put("has_flag_A", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "flagA")).toString()));
            syslogJson.put("has_flag_S", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "flagS")).toString()));
            syslogJson.put("has_flag_F", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "flagF")).toString()));
            syslogJson.put("has_flag_R", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "flagR")).toString()));
            syslogJson.put("has_flag_P", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "flagP")).toString()));
            syslogJson.put("has_flag_U", Integer.parseInt(csvMap.get(getJsonSubKey("syslog", "flagU")).toString()));
        }

        JSONObject inputJson = new JSONObject();
        inputJson.put("type", "syslog");

        JSONArray hostIpJsonArray = new JSONArray();
        hostIpJsonArray.put(hostIp);

        JSONArray hostMacJsonArray = new JSONArray();
        hostMacJsonArray.put(hostMac);

        JSONObject hostJson = new JSONObject();
        hostJson.put("os", osInfoJson);
        hostJson.put("id", hostId);
        hostJson.put("containerized", containerized);
        hostJson.put("ip", hostIpJsonArray);
        hostJson.put("mac", hostMacJsonArray);
        hostJson.put("hostname", hostname);
        hostJson.put("architecture", hostArch);
        hostJson.put("name", hostname);

        JSONArray eventTypeJsonArray = new JSONArray();
        eventTypeJsonArray.put("connection");

        JSONArray eventCategoryJsonArray = new JSONArray();
        eventCategoryJsonArray.put("network_traffic");
        eventCategoryJsonArray.put("network");

        JSONObject eventJson = new JSONObject();
        eventJson.put("action", "syslog_flow");
        eventJson.put("type", eventTypeJsonArray);
        eventJson.put("created", csvMap.get(getJsonSubKey("timestamp", "start")).toString());
        eventJson.put("start", csvMap.get(getJsonSubKey("timestamp", "start")).toString());
        eventJson.put("duration", Double.parseDouble(csvMap.get(getJsonSubKey("timestamp", "duration")).toString()));
        eventJson.put("end", convertMillisToDatetime(convertDatetimeToMillis(
                csvMap.get(getJsonSubKey("timestamp", "end")).toString()) + ((Double) eventJson.getDouble("duration")).longValue())
        );
        eventJson.put("kind", "event");
        eventJson.put("category", eventCategoryJsonArray);

        JSONObject observerJson = new JSONObject();
        observerJson.put("ip", hostIp);

        JSONObject ecsJson = new JSONObject();
        ecsJson.put("version", "1.8.0");

        JSONObject destinationJson = new JSONObject();
        destinationJson.put("locality", checkIpLocality(csvMap.get(getJsonSubKey("destination", "ip")).toString()));
        destinationJson.put("port", Integer.parseInt(csvMap.get(getJsonSubKey("destination", "port")).toString()));
        destinationJson.put("ip", csvMap.get(getJsonSubKey("destination", "ip")).toString());

        JSONObject networkJson = new JSONObject();
        networkJson.put("iana_number", ianaProtocolNumbers.getIanaNum(csvMap.get(getJsonSubKey("network", "protocol")).toString().toUpperCase()));
        networkJson.put("bytes",
                Double.parseDouble((csvMap.get(getJsonSubKey("source", "bytes")).toString().trim()))
                    +Double.parseDouble((csvMap.get(getJsonSubKey("destination", "bytes")).toString().trim()))
        );
        networkJson.put("packets",
                Integer.parseInt(csvMap.get(getJsonSubKey("source", "packets")).toString())
                        + Integer.parseInt(csvMap.get(getJsonSubKey("destination", "packets")).toString())
        );
        networkJson.put("direction", getNetflowDirection(
                csvMap.get(getJsonSubKey("source", "ip")).toString(),
                csvMap.get(getJsonSubKey("destination", "ip")).toString()
        ));
        networkJson.put("community_id", getNetworkCommunityId(
                csvMap.get(getJsonSubKey("source", "ip")).toString(),
                csvMap.get(getJsonSubKey("destination", "ip")).toString(),
                Integer.parseInt(csvMap.get(getJsonSubKey("source", "port")).toString()),
                Integer.parseInt(csvMap.get(getJsonSubKey("destination", "port")).toString()),
                csvMap.get(getJsonSubKey("network", "protocol")).toString()
        ));
        networkJson.put("transport", csvMap.get(getJsonSubKey("network", "protocol")).toString().toLowerCase().trim());

        JSONObject elasticJson = new JSONObject();
        elasticJson.put("@timestamp", Long.toString(System.currentTimeMillis()));
        elasticJson.put("@metadata", metadataJson);
        elasticJson.put("flow", flowJson);
        elasticJson.put("source", sourceJson);
        elasticJson.put("related", relatedJson);
        elasticJson.put("syslog", syslogJson);
        elasticJson.put("input", inputJson);
        elasticJson.put("host", hostJson);
        elasticJson.put("event", eventJson);
        elasticJson.put("observer", observerJson);
        elasticJson.put("agent", agentJson);
        elasticJson.put("ecs", ecsJson);
        elasticJson.put("destination", destinationJson);
        elasticJson.put("network", networkJson);

        return elasticJson;
    }

    /**
     * Return a sub-category key from .json file, which is used for mapping the csv input record
     * with the Elastic output json one. The result is the csv column name, which will be used.
     *
     * @param key    String The mapping category name.
     * @param subkey String The mapping sub-category name.
     * @return String The name of the csv column to use in order to extract this information.
     */
    private String getJsonSubKey(String key, String subkey) {
        return mappingJson.getJSONObject(key).getString(subkey);
    }

    /**
     * Converts a given timestamp in format "YYYY-MM-DD hh:mm:ss.SSS" to format "YYYY-MM-DDThh:mm:ss.SSSZ".
     * i.e. The timestamp 2021-10-27 10:29:05.123 will be converted and return as 2021-10-27T10:29:05.123Z
     *
     * @param timestamp Timestamp to be converted in format "YYYY-MM-DD hh:mm:ss.SSS".
     * @return String Timestamp in format "YYYY-MM-DDThh:mm:ss.SSSZ".
     */
    private String convertTimestamp(String timestamp) {
        return timestamp.replace(" ", "T") + "Z";
    }

    /**
     * Converts a given timestamp in format "YYYY-MM-DD hh:mm:ss" to milliseconds.
     * If ParseException is thrown, 0 (equal to Jan 1, 1970 @ 00:00:00.000) will be returned.
     *
     * @param timestamp Timestamp to be converted in format "YYYY-MM-DD hh:mm:ss".
     * @return Long Milliseconds, that represent the given timestamp.
     */
    private long convertDatetimeToMillis(String timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(timestamp);
            long millis = date.getTime();

            return millis;
        }
        catch(ParseException e) {
            logger.error("Datetime " + timestamp + " cannot be converted to ms. Returning 0");
            logger.error(e.getMessage());
            logger.error(e.getCause());

            return 0L;
        }
    }

    /**
     * Converts a given timestamp in millis to datetime in format "YYYY-MM-DD hh:mm:ss".
     *
     * @param millis Timestamp to be converted in millis.
     * @return String Converted timestamp in datetime format "YYYY-MM-DD hh:mm:ss".
     */
    private String convertMillisToDatetime(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String datetime = sdf.format(new Date(millis));

        return datetime;
    }

    /**
     * Checks if a given IP address is internal or external. The checking is taking place using a file
     * with all internal IP addresses. If the given IP is found in this list, then it is considered as
     * internal IP. Otherwise, it is considered as external IP address.
     *
     * @param ipAddr String The IP address to check if it is internal or not.
     * @return String Returns "internal" if the given IP is found in list with internal IP addresses
     * and "external" otherwise.
     */
    private String checkIpLocality(String ipAddr) {
        if (internalIps.contains(ipAddr)) {
            return "internal";
        }
        return "external";
    }

    /**
     * Generates a random UUID.
     *
     * @return String The generated UUID.
     */
    private String getRandomUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates an ID for the syslog. The generated ID is a random UUID.
     *
     * @return String The generated Flow ID.
     */
    private String generateFlowId() {
        return getRandomUuid();
    }

    /**
     * Retrieves information from System, about the OS. The following properties are retrieved:
     * OS family, OS name, OS codename, OS platform, OS version, OS type and OS kernel. If any of these
     * properties cannot be found, its value is set to "Unknown". All retrieved properties are saved
     * in a JSON object.
     *
     * @return JSONObject A json object that contains all the retrieved information about the host's OS.
     */
    private JSONObject getOsInfo() {
        JSONObject osJson = new JSONObject();
        osJson.put("family", System.getProperty("os.family", "Unknown"));
        osJson.put("name", System.getProperty("os.name", "Unknown"));
        osJson.put("kernel", System.getProperty("os.kernel", "Unknown"));
        osJson.put("codename", System.getProperty("os.codename", "Unknown"));
        osJson.put("type", System.getProperty("os.type", "Unknown"));
        osJson.put("platform", System.getProperty("os.platform", "Unknown"));
        osJson.put("version", System.getProperty("os.version", "Unknown"));

        return osJson;
    }

    /**
     * Retrieves hostname.
     *
     * @return String The retrieved hostname.
     */
    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("UnknownHostException. Cannot retrieve hostname. Return default ElasticSinkHost.");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            return "ElasticSinkHost";
        }
    }

    /**
     * Hashes the hostname using SHA-1 algorithm and returns the hashed value.
     *
     * @return String The SHA-1 hash of the hostname.
     */
    private String hashHostname() {
        return DigestUtils.sha1Hex(getHostname());
    }

    /**
     * Retrieves the IP address of the host. If any error occurs, it returns localhost address (127.0.0.1).
     *
     * @return String The IP address of the host.
     */
    private String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("UnknownHostException. Cannot retrieve localhost. Return default IP 127.0.0.1.");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            return "127.0.0.1";
        }
    }

    /**
     * Retrieves the MAC address of the host. If any error occurs, it returns a default MAC address (00:00:00:00:00:00).
     *
     * @return String The MAC address of the host.
     */
    private String getMacAddress() {
        try {
            byte[] hardwareAddress = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
            StringBuilder macStringBuilder = new StringBuilder();
            for (int i = 0; i < hardwareAddress.length; i++) {
                macStringBuilder.append(String.format(
                        "%02X%s", hardwareAddress[i],
                        (i < hardwareAddress.length - 1) ? "-" : ""));
            }
            return macStringBuilder.toString().replaceAll("-", ":").toLowerCase();
        } catch (SocketException e) {
            logger.error("SocketException. Cannot retrieve localhost. Return default MAC 00:00:00:00:00:00.");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            return "00:00:00:00:00:00";
        } catch (UnknownHostException e) {
            logger.error("UnknownHostException. Cannot retrieve localhost. Return default MAC 00:00:00:00:00:00.");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            return "00:00:00:00:00:00";
        }
    }

    /**
     * Returns host's OS architecture. If the OS architecture cannot retrieved the value "Unknown" is returned.
     *
     * @return String OS architecture
     */
    private String getHostArch() {
        return System.getProperty("os.arch", "Unknown");
    }

    /**
     * Retrieves some information about the agent, making the conversion. More specific it returns
     * a JSON object containing the following inforamtion: Some IDs of the agent (ephemeral_id, id),
     * the agent's name (which is the same as hostname), agent's type and version and the hostname.
     *
     * @return JSONObject A JSON object, which contains information about the agent making the conversion
     * from input csv record to Elastic JSON record
     */
    private JSONObject initializeAgentJson() {
        JSONObject agentJson = new JSONObject();
        agentJson.put("ephemeral_id", getRandomUuid());
        agentJson.put("id", getRandomUuid());
        agentJson.put("name", hostname);
        agentJson.put("type", "kafka-connect");
        agentJson.put("version", "2.8.0");
        agentJson.put("hostname", hostname);

        return agentJson;
    }

    /**
     * Encode TCP flags to an integer number, which is created for the sum of each
     * flag's representation as an integer number. More specifically they are encoded
     * as follows: F-1, S-2, R-4, P-8, A-16, U-32. X, if exists means that all
     * other flags exist, so it is encoded as the sum of all, which is equal to 63.
     *
     * @param flags String A string, which contains all TCP flags.
     * @return int The sum of each existing flag's representation.
     */
    private int encodeTcpFlags(String flags) {
        // https://www.iana.org/assignments/ipfix/ipfix.xhtml

        int tcpControlBits = 0;

        if (flags.contains("X")) {
            // If X found, it contains F, S, R, P, A, U
            tcpControlBits = 1 + 2 + 4 + 8 + 16 + 32;
        } else {
            if (flags.contains("F")) {
                tcpControlBits = tcpControlBits + 1;
            }
            if (flags.contains("S")) {
                tcpControlBits = tcpControlBits + 2;
            }
            if (flags.contains("R")) {
                tcpControlBits = tcpControlBits + 4;
            }
            if (flags.contains("P")) {
                tcpControlBits = tcpControlBits + 8;
            }
            if (flags.contains("A")) {
                tcpControlBits = tcpControlBits + 16;
            }
            if (flags.contains("U")) {
                tcpControlBits = tcpControlBits + 32;
            }
        }

        return tcpControlBits;
    }

    /**
     * Loads a JSON file, that contains some mapping details between the csv column
     * name and the category and sub-category of the new Elastic JSON object. If any
     * error occurs while loading the mapping file, a default JSON mapping will
     * be returned.
     *
     * @param raw Boolean Declares if the mapping file for raw syslog data must be
     *            loaded or not.
     * @return JSONObject A JSON object, which contains all mapping details between
     * the csv records and the created JSON object.
     */
    private JSONObject loadMappingFile(boolean raw) {
        // Preprocessed syslog data schema
        String resourceName = "csv-json-mapping-preprocessed.json";
        if (raw) {
            // Raw syslog data schema
            resourceName = "csv-json-mapping-raw.json";
        }

        try {
            InputStream is = new FileInputStream(new File(resourceName));

            JSONTokener tokener = new JSONTokener(is);
            JSONObject mapping = new JSONObject(tokener);

            return mapping;
        } catch (Exception e) {
            logger.error("FileNotFoundException. Something went wrong loading mapping file (raw=" + raw + "). Returning a default predefined csv schema.");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            if (raw) {
                // Return default csv schema for raw syslog
                return new JSONObject("{\"syslog\":{\"dstAs\":\"das\",\"ingress\":\"in\",\"nextHop\":\"nh\",\"tcpFlags\":\"flg\",\"ipClassService\":\"stos\",\"egress\":\"out\",\"srcAs\":\"sas\"},\"destination\":{\"port\":\"dp\",\"bytes\":\"obyt\",\"ip\":\"da\",\"packets\":\"opkt\",\"mask\":\"dmk\"},\"source\":{\"port\":\"sp\",\"bytes\":\"ibyt\",\"ip\":\"sa\",\"packets\":\"ipkt\",\"mask\":\"smk\"},\"timestamp\":{\"duration\":\"td\",\"start\":\"ts\",\"collected\":\"tr\",\"end\":\"te\"},\"network\":{\"protocol\":\"pr\"}}");
            } else {
                // Return default csv schema for preprocessed syslog
                return new JSONObject("{\"syslog\":{\"dstAs\":\"das\",\"nextHop\":\"nh\",\"flagU\":\"flgu\",\"tcpFlags\":\"flg\",\"flagS\":\"flgs\",\"flagR\":\"flgr\",\"flagP\":\"flgp\",\"packets\":\"tpkt\",\"egress\":\"out\",\"ingress\":\"in\",\"flagF\":\"flgf\",\"bytes\":\"tbyt\",\"flagA\":\"flga\",\"ipClassService\":\"stos\",\"srcAs\":\"sas\"},\"destination\":{\"commonPort\":\"cp\",\"port\":\"dp\",\"bytes\":\"obyt\",\"ip\":\"da\",\"packets\":\"opkt\",\"mask\":\"dmk\"},\"source\":{\"port\":\"sp\",\"bytes\":\"ibyt\",\"ip\":\"sa\",\"packets\":\"ipkt\",\"mask\":\"smk\"},\"timestamp\":{\"duration\":\"td\",\"start\":\"ts\",\"collected\":\"tr\",\"end\":\"te\"},\"network\":{\"protocolTcp\":\"prtcp\",\"protocolUdp\":\"prudp\",\"protocolIcmp\":\"pricmp\",\"protocol\":\"pr\",\"protocolOther\":\"prother\",\"protocolIgmp\":\"prigmp\"}}");
            }
        }
    }

    /**
     * Checks if the given syslog is internal, external, inbound or outbound.
     * If it cannot be defined, it returns the label "unknown". This decision
     * is based on the source and destination IP of the syslog. A syslog will
     * be labeled as internal if both source and destination IPs are internal.
     * If both IPs are not internal the syslog will be labeled as external.
     * A syslog will be labeled as outbound if source IP is internal and
     * destination IP is external. On the other hand, if the destination IP is
     * internal and source IP is external, the flow will be labeled as inbound.
     * If the syslog direction cannot be defined, or if any other error occurs,
     * the syslog will be labeled as unknown. A list with internal IPs will be
     * used in order to decide if the IP is internal or external.
     *
     * @param srcIp String Source IP address.
     * @param dstIp String Destination IP address.
     * @return String A string, that show the syslog direction. It will be one of
     * internal, external, inbound, outbound, unknown.
     */
    private String getNetflowDirection(String srcIp, String dstIp) {
        boolean srcIpInternal = checkIpLocality(srcIp).equals("internal");
        boolean dstIpInternal = checkIpLocality(dstIp).equals("internal");
        if (srcIpInternal && dstIpInternal) {
            return "internal";
        } else if (srcIpInternal && !dstIpInternal) {
            return "outbound";
        } else if (!srcIpInternal && dstIpInternal) {
            return "inbound";
        } else if (!srcIpInternal && !dstIpInternal) {
            return "external";
        } else {
            return "unknown";
        }
    }

    /**
     * Creates and returns the network community ID of the syslog. The
     * community ID is an identifier for a syslog, which is produced using
     * some syslog fields. More specifically it uses source IP address,
     * destination IP address, source and destination ports and the syslog
     * protocol (UDP, TCP). The flow community ID is generated using the open-source
     * community-id-java library (https://github.com/rapid7/community-id-java).
     *
     * @param srcIp    String Source IP address.
     * @param dstIp    String Destination IP address.
     * @param srcPort  Int Source port.
     * @param dstPort  Int Destination port.
     * @param protocol String Protocol of the syslog (TCP, UDP).
     * @return String A syslog hash, which follows the open Community ID
     * (https://github.com/corelight/community-id-spec) syslog hashing standard.
     */
    private String getNetworkCommunityId(String srcIp, String dstIp, int srcPort, int dstPort, String protocol) {
        Protocol pr = Protocol.UDP;
        if (protocol.toUpperCase().trim().equals("TCP")) {
            pr = Protocol.TCP;
        }

        try {
            return this.networkCommunityIdGenerator.generateCommunityId(
                    pr,
                    InetAddress.getByName(srcIp),
                    srcPort,
                    InetAddress.getByName(dstIp),
                    dstPort
            );
        } catch (UnknownHostException e) {
            logger.error("UnknownHostException. Cannot create network community ID. Returning default 1:unknown.");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            return "1:unknown";
        }
    }

    /**
     * Loads all subnets, that their IPs must be defined as internal IPs. The list
     * of these subnets must be written in a txt file, in the following format: IP/Mask.
     * (For example: 192.168.2.0/24). All IPs of these subnets will be a loaded in a
     * list and used for defining if an IP is internal (belonging in one of these subnets)
     * or external. If the file with the subnets cannot be loaded, some default subnets will
     * be returned. These default subnets are the following: 10.10.5.0/24, 10.10.11.0/24,
     * 192.168.1.0/24, 192.168.2.0/24.
     *
     * @return ArrayList(String) A list which contains all IPs that will be marked as internals.
     */
    private ArrayList<String> loadInternalSubnets() {
        ArrayList<String> internalAddresses = new ArrayList<String>();
        ArrayList<String> subnets = new ArrayList<String>();
        String subnetsFile = "subnets.txt";

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(subnetsFile)));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                subnets.add(line.trim());
            }
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException. Something went wrong loading subnets file. Initializing with default subnets values:");
            logger.error("10.10.5.0/24, 10.10.11.0/24, 192.168.1.0/24, 192.168.2.0/24");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            subnets.clear();
            subnets.add("10.10.5.0/24");
            subnets.add("10.10.11.0/24");
            subnets.add("192.168.1.0/24");
            subnets.add("192.168.2.0/24");
        } catch (IOException e) {
            logger.error("IOException. Something went wrong loading subnets file. Initializing with default subnets values:");
            logger.error("10.10.5.0/24, 10.10.11.0/24, 192.168.1.0/24, 192.168.2.0/24");
            logger.error(e.getCause());
            logger.error(e.getMessage());

            subnets.clear();
            subnets.add("10.10.5.0/24");
            subnets.add("10.10.11.0/24");
            subnets.add("192.168.1.0/24");
            subnets.add("192.168.2.0/24");
        }

        subnets.forEach(subnet -> {
            SubnetUtils subnetUtils = new SubnetUtils(subnet.trim());
            internalAddresses.addAll(Arrays.asList(subnetUtils.getInfo().getAllAddresses()));
        });

        return internalAddresses;
    }
}
