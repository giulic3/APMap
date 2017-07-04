package io.github.giulic3.apmap.data;

// helper class type
public class AccessPoint {

    // ap info
    private String bssid;
    private String ssid;
    private String capabilities;
    private int frequency;
    private int level;
    private long timestamp;

    // ap methods (getters)

    public AccessPoint(String bssid, String ssid, String capabilities, int frequency, int level,
                        long timestamp){

        this.bssid = bssid;
        this.ssid = ssid;
        this.capabilities = capabilities;
        this.frequency = frequency;
        this.level = level;
        this.timestamp = timestamp;
    }

}
