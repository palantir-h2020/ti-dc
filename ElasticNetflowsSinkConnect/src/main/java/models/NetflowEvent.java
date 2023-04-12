package models;

public class NetflowEvent {
    private double duration;
    private String created;
    private String start;
    private String end;

    public NetflowEvent(double duration, String created, String start, String end) {
        this.duration = duration;
        this.created = created;
        this.start = start;
        this.end = end;
    }

    public double getDuration() {
        return duration;
    }

    public String getCreated() {
        return created;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }
}
