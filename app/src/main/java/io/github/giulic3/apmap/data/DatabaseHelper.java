package io.github.giulic3.apmap.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "AccessPointMap.db";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    //TODO: check consistency between sql data types and java data types
    // always make sure queries are syntactically correct
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable1 = "CREATE TABLE "+ Database.Table1.TABLE_NAME+ "("
                //+ Database.Table1.COLUMN_NAME_BSSID+" STRING PRIMARY KEY,"
                + Database.Table1.COLUMN_NAME_BSSID+" STRING,"
                + Database.Table1.COLUMN_NAME_SSID+" STRING NOT NULL,"
                + Database.Table1.COLUMN_NAME_CAPABILITIES+" STRING NOT NULL,"
                + Database.Table1.COLUMN_NAME_FREQUENCY+" INT NOT NULL,"
                + Database.Table1.COLUMN_NAME_ESTIMATED_LATITUDE+" DOUBLE,"
                + Database.Table1.COLUMN_NAME_ESTIMATED_LONGITUDE+" DOUBLE,"
                + Database.Table1.COLUMN_NAME_COVERAGE_RADIUS+" DOUBLE)";
        db.execSQL(createTable1);

        String createTable2 = "CREATE TABLE "+ Database.Table2.TABLE_NAME+ "("
                + Database.Table2.COLUMN_NAME_BSSID+" STRING NOT NULL REFERENCES "
                + Database.Table1.TABLE_NAME+"("+Database.Table2.COLUMN_NAME_BSSID+"),"
                + Database.Table2.COLUMN_NAME_TIMESTAMP+" TIMESTAMP NOT NULL,"
                + Database.Table2.COLUMN_NAME_SCAN_LATITUDE+" DOUBLE NOT NULL,"
                + Database.Table2.COLUMN_NAME_SCAN_LONGITUDE+" DOUBLE NOT NULL,"
                + Database.Table2.COLUMN_NAME_LEVEL+" INT NOT NULL,"
                +"PRIMARY KEY("+ Database.Table2.COLUMN_NAME_BSSID+","+ Database.Table2.COLUMN_NAME_TIMESTAMP+"))";
        db.execSQL(createTable2);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

       // query1: insert entry in AccessPointInfo table
       //TODO: check methods definitions (return types, visibility...)
       public long insertAp(String bssid, String ssid, String capabilities, String frequency,
                            String estimatedLatitude, String estimatedLongitude,
                            String coverageRadius) {

            // Gets the data repository in write mode
            SQLiteDatabase db = this.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(Database.Table1.COLUMN_NAME_BSSID, bssid);
            values.put(Database.Table1.COLUMN_NAME_SSID, ssid);
            values.put(Database.Table1.COLUMN_NAME_CAPABILITIES, capabilities);
            values.put(Database.Table1.COLUMN_NAME_FREQUENCY, frequency);
            values.put(Database.Table1.COLUMN_NAME_ESTIMATED_LATITUDE, estimatedLatitude); // can be null initially
            values.put(Database.Table1.COLUMN_NAME_ESTIMATED_LONGITUDE, estimatedLongitude); // can be null
            values.put(Database.Table1.COLUMN_NAME_COVERAGE_RADIUS, coverageRadius); // can be null

            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insertOrThrow(Database.Table1.TABLE_NAME, null, values);

            return newRowId;
       }

        // query2: update entry in AccessPointInfo table

        // returns true if Ap was found and updated, each parameter can be null when
        // it doesn't need to be updated (bssid excluded)
        public boolean updateAp(String bssid, String capabilities, String estimatedLatitude,
                                String estimatedLongitude, Double coverageRadius){

            // search an access point by bssid and update


            return true;
        }

        // query3: join AccessPointInfo table and Scan table on bssid
        // this method is used by query2, so it can be private

        private void performApproximation(){

        }

        // query4: insert entry in Scan table
        public long insertScanObject(String bssid, String timestamp,
                                     String scanLatitude, String scanLongitude, String level) {

            // Gets the data repository in write mode
            SQLiteDatabase db = this.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(Database.Table2.COLUMN_NAME_TIMESTAMP, timestamp);
            values.put(Database.Table2.COLUMN_NAME_SCAN_LATITUDE, scanLatitude);
            values.put(Database.Table2.COLUMN_NAME_SCAN_LONGITUDE, scanLongitude);
            values.put(Database.Table2.COLUMN_NAME_LEVEL, level);

            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insert(Database.Table2.TABLE_NAME, null, values);

            return newRowId;
        }

        // query5: delete entry in Scan table
        // used to clean up Scan table after many scans (otherwise db becomes too big)
        // e.g. once a day, can keeps 3 measures (most recents) max
        // this method returns true when ap is found and deletion is performed
        public boolean deleteScan(String bssid){
            return true;
        }

        // need a method to delete aps on map when are no longer found

        // class used to navigate a set of results (after a query)
        public Cursor query() {

            Cursor cursor = null;
            try {
                SQLiteDatabase db = this.getReadableDatabase();
                //cursor = db.query(AccessPointInfo, null, null, null, null, null, null, null);
            }
            catch(SQLiteException sqle) {
                return null;
            }
            return cursor;
        }
}

