package io.github.giulic3.apmap.models;

public class ListItem {

    private String ssid;
    private String bssid;
    private int level;

    public ListItem(String ssid, String bssid, int level) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.level = level;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}