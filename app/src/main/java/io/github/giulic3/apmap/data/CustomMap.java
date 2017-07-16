package io.github.giulic3.apmap.data;

/**
 * Created by giulia on 16/07/17.
 */

public class CustomMap {

    private String ssid;
    private int level;
    // constructor
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