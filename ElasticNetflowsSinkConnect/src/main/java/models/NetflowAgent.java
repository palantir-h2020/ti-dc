package models;

public class NetflowAgent {
    private String hostname;
    private String ip;
    private String type;
    private String version;
    private NetflowAgentOs os;
    private boolean containerized;
    private String mac;
    private String architecture;

    public NetflowAgent(String hostname, String ip, String type, String version, NetflowAgentOs os, boolean containerized, String mac, String architecture) {
        this.hostname = hostname;
        this.ip = ip;
        this.type = type;
        this.version = version;
        this.os = os;
        this.containerized = containerized;
        this.mac = mac;
        this.architecture = architecture;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public NetflowAgentOs getOs() {
        return os;
    }

    public boolean isContainerized() {
        return containerized;
    }

    public String getMac() {
        return mac;
    }

    public String getArchitecture() {
        return architecture;
    }
}
