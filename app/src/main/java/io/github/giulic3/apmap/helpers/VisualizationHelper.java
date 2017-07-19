package io.github.giulic3.apmap.helpers;


import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

import io.github.giulic3.apmap.activities.MainActivity;
import io.github.giulic3.apmap.models.AccessPointInfoEntry;

public class VisualizationHelper {

    private final int RANGE = 200; // in metres

    public void showAll(ArrayList<Marker> markers){

        for (int i = 0; i < markers.size(); i++) {
            markers.get(i).setVisible(true);
        }
    }

    public void showOnlyOpen(ArrayList<Marker> markers) {

        for (int i = 0; i < markers.size(); i++){
            AccessPointInfoEntry apInfoEntry = (AccessPointInfoEntry) markers.get(i).getTag();
            String securityType = MainActivity.getAccessPointSecurityType(apInfoEntry.getCapabilities());

            if (securityType.equals("open"))
                markers.get(i).setVisible(true);
            else
                markers.get(i).setVisible(false);
        }
    }

    public void showOnlyClosed(ArrayList<Marker> markers) {

        for (int i = 0; i < markers.size(); i++){
            AccessPointInfoEntry apInfoEntry = (AccessPointInfoEntry) markers.get(i).getTag();
            String securityType = MainActivity.getAccessPointSecurityType(apInfoEntry.getCapabilities());

            if (securityType.equals("closed"))
                markers.get(i).setVisible(true);
            else
                markers.get(i).setVisible(false);
        }
    }

    public void showOnlyRange(ArrayList<Marker> markers, double centerLatitude, double centerLongitude) {

        for (int i = 0; i < markers.size(); i++){
            AccessPointInfoEntry apInfoEntry = (AccessPointInfoEntry) markers.get(i).getTag();
            double givenRange = RANGE;
            double apLatitude = apInfoEntry.getEstimatedLatitude();
            double apLongitude = apInfoEntry.getEstimatedLongitude();
            double distance = MathHelper.convertToDistance(apLatitude, apLongitude,
                    centerLatitude, centerLongitude);

            if (distance <= givenRange)
                markers.get(i).setVisible(true);
            else
                markers.get(i).setVisible(false);
        }
    }

    // TODO: modo per combinare due cose? es. togliere i cerchi solo dei marker non visibili?
    public void hideCoverage(ArrayList<Circle> circles) {

        for (int i = 0; i < circles.size(); i++) {
            circles.get(i).setVisible(false);
        }
    }

    public void showCoverage(ArrayList<Circle> circles) {

        for (int i = 0; i < circles.size(); i++) {
            circles.get(i).setVisible(true);
        }
    }

}
