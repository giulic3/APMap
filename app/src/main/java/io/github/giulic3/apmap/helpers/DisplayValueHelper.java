package io.github.giulic3.apmap.helpers;

import java.text.DecimalFormat;
import java.text.NumberFormat;

// this class is used to display values relative to the access point in the textviews or listview
// in a readable format
public class DisplayValueHelper {

    public DisplayValueHelper(){

    }
    // takes as input a signal level in dBm and returns an integer which represents a percentage of signalQuality
    public int convertLevelToPercentage(int signalLevel) {

        int quality = 2* (signalLevel + 100);
        if (quality > 100) return 100; // formula is approximate
        else return quality;
    }
    // takes lon or lat as input an format value
    public String formatCoordinate(double coordinate) {

        NumberFormat formatter = new DecimalFormat("#00.00");
        return (formatter.format(coordinate));
    }

    public String getReadableSecurityType(String capabilities) {
        if (capabilities.contains("WEP"))
            return "WEP";
        else if (capabilities.contains("WPA"))
            return "WPA";
        else if (capabilities.contains("WPA2"))
            return "WPA2";
        else if (capabilities.contains("ESS"))
            return "ESS";
        else return "open";


    }
}
