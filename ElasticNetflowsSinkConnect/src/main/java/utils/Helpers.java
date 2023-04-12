package utils;

import com.rapid7.communityid.CommunityIdGenerator;
import com.rapid7.communityid.Protocol;
import models.*;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Logger logger;

    /**
     * List with all IPs that are considered internals
     */
    private ArrayList<String> internalIps;

    /**
     * Generator for network community ID
     */
    private CommunityIdGenerator networkCommunityIdGenerator;

    public Helpers() {
        this.logger = Logger.getLogger(Helpers.class);
        this.internalIps = loadInternalSubnets();
        this.networkCommunityIdGenerator = new CommunityIdGenerator();
    }

    /**
     * Retrieves hostname.
     *
     * @return String The retrieved hostname.
    */
    public String getHostname() {
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
     * Retrieves the IP address of the host. If any error occurs, it returns localhost address (127.0.0.1).
     *
     * @return String The IP address of the host.
    */
    public String getIpAddress() {
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
     * Retrieves information from System, about the OS. The following properties are retrieved:
     * OS family, OS name, OS codename, OS platform, OS version, OS type and OS kernel. If any of these
     * properties cannot be found, its value is set to "Unknown". All retrieved properties are saved
     * in a JSON object.
     *
     * @return JSONObject A json object that contains all the retrieved information about the host's OS.
     */
    public NetflowAgentOs getOsInfo() {
        return new NetflowAgentOs(
                System.getProperty("os.kernel", "Unknown"), System.getProperty("os.codename", "Unknown"),
                System.getProperty("os.name", "Unknown"), System.getProperty("os.family", "Unknown"),
                System.getProperty("os.type", "Unknown"), System.getProperty("os.version", "Unknown"),
                System.getProperty("os.platform", "Unknown")
        );
    }

    /**
     * Checks if the connector is running inside a container or not. The check is happening reading the file
     * /proc/1/cgroup and searching for a line that contains the value /docker. If this value is not found, or
     * any error occurs, it is considered that the connector is not running inside a container.
     *
     * @return Boolean True if the app runs in container, False otherwise.
     */
    public Boolean isContainerized() {
        try (Stream<String> stream =
                     Files.lines(Paths.get("/proc/1/cgroup"))) {
            return stream.anyMatch(line -> line.contains("/docker"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Retrieves the MAC address of the host. If any error occurs, it returns a default MAC address (00:00:00:00:00:00).
     *
     * @return String The MAC address of the host.
     */
    public String getMacAddress() {
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
    public String getHostArch() {
        return System.getProperty("os.arch", "Unknown");
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
    public String checkIpLocality(String ipAddr) {
        if (internalIps.contains(ipAddr)) {
            return "internal";
        }
        return "external";
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

    /**
     * Creates and returns the network community ID of the netflow. The
     * community ID is an identifier for a netflow, which is produced using
     * some netflow fields. More specifically it uses source IP address,
     * destination IP address, source and destination ports and the netflow
     * protocol (UDP, TCP). The flow community ID is generated using the open-source
     * community-id-java library (https://github.com/rapid7/community-id-java).
     *
     * @param srcIp    String Source IP address.
     * @param dstIp    String Destination IP address.
     * @param srcPort  Int Source port.
     * @param dstPort  Int Destination port.
     * @param protocol String Protocol of the netflow (TCP, UDP).
     * @return String A netflow hash, which follows the open Community ID
     * (https://github.com/corelight/community-id-spec) netflow hashing standard.
     */
    public String getNetworkCommunityId(String srcIp, String dstIp, int srcPort, int dstPort, String protocol) {
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
     * Extract tenant ID from record key. If anything goes wrong, set tenant id=-1
     *
     * @param recordKey String The record key, that contains the tenant ID.
     * @return tenantId Int The ID of the tenant.
     */
    public int extractTenantId(String recordKey) {
        try {
            return Integer.parseInt(recordKey.split("_")[0]);
        }
        catch (Exception e) {
            logger.error("Cannot extract ID for message: " + recordKey + ". Returning -1!");
            return -1;
        }
    }

    /**
     * Converts a given timestamp in format "YYYY-MM-DD hh:mm:ss" to milliseconds.
     * If ParseException is thrown, 0 (equal to Jan 1, 1970 @ 00:00:00.000) will be returned.
     *
     * @param timestamp Timestamp to be converted in format "YYYY-MM-DD hh:mm:ss".
     * @return Long Milliseconds, that represent the given timestamp.
     */
    public long convertDatetimeToMillis(String timestamp) {
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
}
