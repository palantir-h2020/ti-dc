package models;

import java.text.SimpleDateFormat;

public class NetflowExporter {
    private String timestamp;

    public NetflowExporter(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
