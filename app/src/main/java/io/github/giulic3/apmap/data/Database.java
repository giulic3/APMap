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
        public static final String COLUMN_NAME_COVERAGE_CENTER = "coverageCenter";
        public static final String COLUMN_NAME_COVERAGE_RADIUS = "coverageRadius";

    }

    public static class Table2 implements BaseColumns {
        public static final String TABLE_NAME = "Scan";
        public static final String COLUMN_NAME_BSSID = "bssid";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_SCAN_LATITUDE = "scanLatitude";
        public static final String COLUMN_NAME_SCAN_LONGITUDE = "scanLongitude";
        public static final String COLUMN_NAME_LEVEL = "level";

    }


private class DatabaseHelper extends SQLiteOpenHelper {

        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "AccessPointMap.db";

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        //TODO: check consistency sql data types and java data types
        @Override
        public void onCreate(SQLiteDatabase db) {
          String createTable1 = "CREATE TABLE "+Table1.TABLE_NAME+ "("
                  +Table1.COLUMN_NAME_BSSID+" STRING PRIMARY KEY,"
                  +Table1.COLUMN_NAME_SSID+" STRING NOT NULL,"
                  +Table1.COLUMN_NAME_CAPABILITIES+" STRING NOT NULL,"
                  +Table1.COLUMN_NAME_FREQUENCY+" INT NOT NULL,"
                  +Table1.COLUMN_NAME_ESTIMATED_LATITUDE+" DOUBLE,"
                  +Table1.COLUMN_NAME_ESTIMATED_LONGITUDE+" DOUBLE,"
                  +Table1.COLUMN_NAME_COVERAGE_CENTER+" DOUBLE,"
                  +Table1.COLUMN_NAME_COVERAGE_RADIUS+" DOUBLE)";
            db.execSQL(createTable1);

            String createTable2 = "CREATE TABLE "+Table2.TABLE_NAME+ "("
                    +Table2.COLUMN_NAME_BSSID+" STRING NOT NULL REFERENCES AccessPointInfo(bssid),"
                    +Table2.COLUMN_NAME_TIMESTAMP+" TIMESTAMP NOT NULL,"
                    +Table2.COLUMN_NAME_SCAN_LATITUDE+" DOUBLE NOT NULL,"
                    +Table2.COLUMN_NAME_SCAN_LONGITUDE+" DOUBLE NOT NULL,"
                    +Table2.COLUMN_NAME_LEVEL+" INT NOT NULL,"
                    +"PRIMARY KEY("+Table2.COLUMN_NAME_BSSID+","+ Table2.COLUMN_NAME_TIMESTAMP+")";
            db.execSQL(createTable2);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public class DbManager {

        private DatabaseHelper mDbHelper;

        public DbManager(Context context) {
            mDbHelper = new DatabaseHelper(context);
        }


        // query1: insert entry in AccessPointInfo table


        //TODO: check methods definitions (return types, visibility...)
        public long insertAp(String bssid, String ssid, String capabilities, String frequency,
                        String estimatedLatitude, String estimatedLongitude,
                        String coverageCenter, String coverageRadius) {

            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(Table1.COLUMN_NAME_BSSID, bssid);
            values.put(Table1.COLUMN_NAME_SSID, ssid);
            values.put(Table1.COLUMN_NAME_CAPABILITIES, capabilities);
            values.put(Table1.COLUMN_NAME_FREQUENCY, frequency);
            values.put(Table1.COLUMN_NAME_ESTIMATED_LATITUDE, estimatedLatitude); // can be null initially
            values.put(Table1.COLUMN_NAME_ESTIMATED_LONGITUDE, estimatedLongitude); // can be null
            values.put(Table1.COLUMN_NAME_COVERAGE_CENTER, coverageCenter);
            values.put(Table1.COLUMN_NAME_COVERAGE_RADIUS, coverageRadius);

            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insert(Table1.TABLE_NAME, null, values);

            return newRowId;
        }

        // query2: update entry in AccessPointInfo table

        //TODO:

        // query3: join AccessPointInfo table and Scan table on bssid

        //TODO:

        // query4: insert entry in Scan table
        public long insertScanObject(String bssid, String timestamp,
                             String scanLatitude, String scanLongitude, String level) {

            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(Table2.COLUMN_NAME_TIMESTAMP, timestamp);
            values.put(Table2.COLUMN_NAME_SCAN_LATITUDE, scanLatitude);
            values.put(Table2.COLUMN_NAME_SCAN_LONGITUDE, scanLongitude);
            values.put(Table2.COLUMN_NAME_LEVEL, level);


            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insert(Table2.TABLE_NAME, null, values);

            return newRowId;
        }


        //TODO:

        // query5: delete entry in Scan table
        // used to clean up Scan table after many scans (otherwise db becomes too big)
        // e.g. once a day, can keeps 3 measures (most recents) max

        // class used to navigate a set of results (after a query)
        public Cursor query() {

            Cursor cursor = null;
            try {
                SQLiteDatabase db = mDbHelper.getReadableDatabase();
                //cursor = db.query(AccessPointInfo, null, null, null, null, null, null, null);
            }
            catch(SQLiteException sqle) {
                return null;
            }
            return cursor;
        }
    }
}
