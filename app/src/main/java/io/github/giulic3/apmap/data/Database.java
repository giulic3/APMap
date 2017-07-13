package io.github.giulic3.apmap.data;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class Database {
    // Inner classes that define tables contents
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
        public static final String TABLE_NAME = "ScanResult"; // TODO CHANGE NAME AND RESET DATABASE, maybe "scanobject"
        public static final String COLUMN_NAME_BSSID = "bssid";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_SCAN_LATITUDE = "scanLatitude";
        public static final String COLUMN_NAME_SCAN_LONGITUDE = "scanLongitude";
        public static final String COLUMN_NAME_LEVEL = "level";

    }
}

