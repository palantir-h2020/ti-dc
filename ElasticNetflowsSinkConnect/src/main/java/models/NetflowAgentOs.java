package models;

public class NetflowAgentOs {
    private String kernel;
    private String codename;
    private String name;
    private String family;
    private String type;
    private String version;
    private String platform;

    public NetflowAgentOs(String kernel, String codename, String name, String family, String type, String version, String platform) {
        this.kernel = kernel;
        this.codename = codename;
        this.name = name;
        this.family = family;
        this.type = type;
        this.version = version;
        this.platform = platform;
    }

    public String getKernel() {
        return kernel;
    }

    public String getCodename() {
        return codename;
    }

    public String getName() {
        return name;
    }

    public String getFamily() {
        return family;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getPlatform() {
        return platform;
    }
}
