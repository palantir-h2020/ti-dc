package models;

public class NetflowNetwork {
    private String community_id;
    private double bytes;
    private String flags;
    private String raw;
    private String transport;
    private int packets;

    public NetflowNetwork(String community_id, double bytes, String flags, String raw, String transport, int packets) {
        this.community_id = community_id;
        this.bytes = bytes;
        this.flags = flags;
        this.raw = raw;
        this.transport = transport;
        this.packets = packets;
    }

    public String getCommunity_id() {
        return community_id;
    }

    public double getBytes() {
        return bytes;
    }

    public String getFlags() {
        return flags;
    }

    public String getRaw() {
        return raw;
    }

    public String getTransport() {
        return transport;
    }

    public int getPackets() {
        return packets;
    }
}
