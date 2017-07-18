package io.github.giulic3.apmap.models;

public class CustomMap {

    private String ssid;
    private int level;

    public CustomMap(String ssid, int level) {
        this.ssid = ssid;
        this.level = level;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}