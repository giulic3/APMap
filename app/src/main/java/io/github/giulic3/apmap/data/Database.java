package io.github.giulic3.apmap.data;

import android.provider.BaseColumns;

public class Database {

    public static class Table1 implements BaseColumns {
        public static final String TABLE_NAME = "AccessPointInfo";
        public static final String COLUMN_NAME_BSSID = "bssid";
        public static final String COLUMN_NAME_SSID = "ssid";
        public static final String COLUMN_NAME_CAPABILITIES = "capabilities";
        public static final String COLUMN_NAME_FREQUENCY = "frequency";
        public static final String COLUMN_NAME_ESTIMATED_LATITUDE = "estimatedLatitude";
        public static final String COLUMN_NAME_ESTIMATED_LONGITUDE = "estimatedLongitude";
        public static final String COLUMN_NAME_COVERAGE_RADIUS = "coverageRadius";

    }

    public static class Table2 implements BaseColumns {
        public static final String TABLE_NAME = "ScanResult";
        public static final String COLUMN_NAME_BSSID = "bssid";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_SCAN_LATITUDE = "scanLatitude";
        public static final String COLUMN_NAME_SCAN_LONGITUDE = "scanLongitude";
        public static final String COLUMN_NAME_LEVEL = "level";

    }
}

