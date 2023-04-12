package models;

public class NetflowAddress {
    private String ip;
    private int port;
    private double bytes;
    private int packets;
    private String locality;

    public NetflowAddress(String ip, int port, double bytes, int packets, String locality) {
        this.ip = ip;
        this.port = port;
        this.bytes = bytes;
        this.packets = packets;
        this.locality = locality;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public double getBytes() {
        return bytes;
    }

    public int getPackets() {
        return packets;
    }

    public String getLocality() {
        return locality;
    }
}
