package models;

import org.json.JSONObject;

public class NetflowZeek {
    private String score;

    public NetflowZeek() {
        this.score = "-";
    }

    public NetflowZeek(String score) {
        this.score = score;
    }

    public String getScore() {
        return score;
    }
}
