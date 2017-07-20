package io.github.giulic3.apmap.helpers;

import java.text.DecimalFormat;
import java.text.NumberFormat;

 /** This class contains methods used to display values relative to the access point in the
  *  textviews or listview in a human readable format */

public class DisplayValueHelper {

    public DisplayValueHelper(){ }

    /** Takes as input a signal level in dBm and returns an integer which represents signal quality in
     percentage */
    public int convertLevelToPercentage(int signalLevel) {

        int quality = 2* (signalLevel + 100);
        if (quality > 100) return 100; // formula is approximate
        else return quality;
    }
    /** Takes lon or lat as input and format value */
    public String formatCoordinate(double coordinate) {

        NumberFormat formatter = new DecimalFormat("#00.00");
        return (formatter.format(coordinate));
    }

    public String getReadableSecurityType(String capabilities) {
        if (capabilities.contains("WEP"))
            return "WEP";
        else if (capabilities.contains("WPA2"))
            return "WPA2";
        else if (capabilities.contains("WPA"))
            return "WPA";
        else if (capabilities.contains("ESS"))
            return "ESS";
        else return "open";

    }
}
