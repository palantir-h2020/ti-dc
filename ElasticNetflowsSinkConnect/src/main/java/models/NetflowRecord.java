package models;

import org.json.JSONObject;
import utils.ColumnsIndex;
import utils.Helpers;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NetflowRecord {
    private Netflow netflow;
    private NetflowAgent agent;
    private NetflowAddress source;
    private NetflowAddress destination;
    private NetflowNetwork network;
    private NetflowEvent event;
    private NetflowInput input;
    private NetflowZeek zeek;
    private NetflowTenant tenant;
    private Helpers helpers;

    public NetflowRecord(String recordKey, String recordValue, NetflowAgent agent, NetflowInput input) {
        helpers = new Helpers();

        String[] data = recordValue.split(",");

        String srcIp = data[ColumnsIndex.SRC_IP_ADDR];
        int srcPort = Integer.parseInt(data[ColumnsIndex.SRC_PORT]);
        double srcBytes = Double.parseDouble(data[ColumnsIndex.SRC_BYT]);
        int srcPackets = Integer.parseInt(data[ColumnsIndex.SRC_PKT]);
        String dstIp = data[ColumnsIndex.DST_IP_ADDR];
        int dstPort = Integer.parseInt(data[ColumnsIndex.DST_PORT]);
        double dstBytes = Double.parseDouble(data[ColumnsIndex.DST_BYT]);
        int dstPackets = Integer.parseInt(data[ColumnsIndex.DST_PKT]);

        double totalBytes = srcBytes + dstBytes;
        int totalPackets = dstPackets + dstPackets;

        String networkProtocol = data[ColumnsIndex.NETWORK_PROTOCOL];
        String tcpFlags = data[ColumnsIndex.TCP_FLAGS];

        this.netflow = new Netflow(
                new NetflowExporter(
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(System.currentTimeMillis())
                )
        );

        this.agent = agent;
        this.source = new NetflowAddress(
                srcIp, srcPort, srcBytes, srcPackets, helpers.checkIpLocality(data[ColumnsIndex.SRC_IP_ADDR])
        );
        this.destination = new NetflowAddress(
                dstIp, dstPort, dstBytes, dstPackets, helpers.checkIpLocality(data[ColumnsIndex.DST_IP_ADDR])
        );
        this.network = new NetflowNetwork(
                helpers.getNetworkCommunityId(srcIp, dstIp, srcPort, dstPort, networkProtocol), totalBytes,
                tcpFlags, recordValue,
                networkProtocol, totalPackets
        );
        this.event = new NetflowEvent(
                Double.parseDouble(data[ColumnsIndex.EVENT_DURATION]), data[ColumnsIndex.EVENT_CREATED],
                data[ColumnsIndex.EVENT_STARTED], data[ColumnsIndex.EVENT_ENDED]
        );
        this.input = input;
        this.zeek = new NetflowZeek(
                data[ColumnsIndex.ZEEK_SCORE]
        );
        this.tenant = new NetflowTenant(
                helpers.extractTenantId(recordKey)
        );
    }

    public Netflow getNetflow() {
        return netflow;
    }

    public NetflowAgent getAgent() {
        return agent;
    }

    public NetflowAddress getSource() {
        return source;
    }

    public NetflowAddress getDestination() {
        return destination;
    }

    public NetflowNetwork getNetwork() {
        return network;
    }

    public NetflowEvent getEvent() {
        return event;
    }

    public NetflowInput getInput() {
        return input;
    }

    public NetflowZeek getZeek() {
        return zeek;
    }

    public NetflowTenant getTenant() {
        return tenant;
    }
}
