package models;

public class Netflow {
    private NetflowExporter exporter;

    public Netflow(NetflowExporter exporter) {
        this.exporter = exporter;
    }

    public NetflowExporter getExporter() {
        return exporter;
    }
}
