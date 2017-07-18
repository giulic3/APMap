package io.github.giulic3.apmap.models;

public class AccessPointInfoEntry {

    private String bssid;
    private String ssid;
    private String capabilities;
    private int frequency;
    private double estimatedLatitude;
    private double estimatedLongitude;
    private double coverageRadius;

    public AccessPointInfoEntry(String bssid, String ssid, String capabilities, int frequency,
                                double estimatedLatitude, double estimatedLongitude, double coverageRadius) {
        this.bssid = bssid;
        this.ssid = ssid;
        this.capabilities = capabilities;
        this.frequency = frequency;
        this.estimatedLatitude = estimatedLatitude;
        this.estimatedLongitude = estimatedLongitude;
        this.coverageRadius = coverageRadius;
    }

    public String getBssid() {
        return bssid;
    }

    public String getSsid() {
        return ssid;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public int getFrequency() {
        return frequency;
    }

    public double getEstimatedLatitude() {
        return estimatedLatitude;
    }

    public double getEstimatedLongitude() {
        return estimatedLongitude;
    }

    public double getCoverageRadius() {
        return coverageRadius;
    }
}
