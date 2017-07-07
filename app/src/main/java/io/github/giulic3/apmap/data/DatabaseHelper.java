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
    // always make sure queries are syntactically correct
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable1 = "CREATE TABLE "+ Database.Table1.TABLE_NAME+ "("
                + Database.Table1.COLUMN_NAME_BSSID+" STRING PRIMARY KEY,"
                //+ Database.Table1.COLUMN_NAME_BSSID+" STRING,"
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
                + Database.Table2.COLUMN_NAME_TIMESTAMP+" BIGINT NOT NULL,"
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
        // last three params are useless since when I insert I don't have these info
       public long insertAp(String bssid, String ssid, String capabilities, int frequency) {

           Log.d("DEBUG", "DatabaseHelper: insertAp()");

            // Gets the data repository in write mode
            SQLiteDatabase db = this.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(Database.Table1.COLUMN_NAME_BSSID, bssid);
            values.put(Database.Table1.COLUMN_NAME_SSID, ssid);
            values.put(Database.Table1.COLUMN_NAME_CAPABILITIES, capabilities);
            values.put(Database.Table1.COLUMN_NAME_FREQUENCY, frequency);
            values.putNull(Database.Table1.COLUMN_NAME_ESTIMATED_LATITUDE);
            values.putNull(Database.Table1.COLUMN_NAME_ESTIMATED_LONGITUDE);
            values.putNull(Database.Table1.COLUMN_NAME_COVERAGE_RADIUS);
            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insertOrThrow(Database.Table1.TABLE_NAME, null, values);

            db.close();
            return newRowId;
       }

        // query2: update entry in AccessPointInfo table
        // returns true if ap was found and updated, each parameter can be null when
        // it doesn't need to be updated (bssid excluded)
        public boolean updateAp(String bssid, String ssid, String capabilities, Double estimatedLatitude,
                                Double estimatedLongitude, Double coverageRadius){

            Log.d("DEBUG", "DatabaseHelper: updateAp()");

            SQLiteDatabase db = this.getWritableDatabase();
            // search an access point by bssid and update
            ContentValues values = new ContentValues();
            // TODO update only when value != null, find better way
            if (ssid != null)
                values.put(Database.Table1.COLUMN_NAME_SSID, ssid);
            if (capabilities != null)
                values.put(Database.Table1.COLUMN_NAME_CAPABILITIES, capabilities);
            if (estimatedLatitude != null)
                values.put(Database.Table1.COLUMN_NAME_ESTIMATED_LATITUDE, estimatedLatitude);
            if (estimatedLongitude != null)
                values.put(Database.Table1.COLUMN_NAME_ESTIMATED_LONGITUDE, estimatedLongitude);
            if (coverageRadius != null)
                values.put(Database.Table1.COLUMN_NAME_COVERAGE_RADIUS, coverageRadius);

            String selection = Database.Table1.COLUMN_NAME_BSSID + " = ?";
            String[] selectionArgs = { bssid };
            db.update(
                    Database.Table1.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);

            db.close();
            return true;
        }

        // query3: join AccessPointInfo table and Scan table on bssid
        // this method is used by query2, so it can be private

        private void performApproximation(){ }

        // query4: insert entry in Scan table
        public long insertScanObject(String bssid, long timestamp,
                                     Double scanLatitude, Double scanLongitude, int level) {

            Log.d("DEBUG", "DatabaseHelper: insertScanObject()");

            // Gets the data repository in write mode
            SQLiteDatabase db = this.getWritableDatabase();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(Database.Table2.COLUMN_NAME_BSSID, bssid);
            values.put(Database.Table2.COLUMN_NAME_TIMESTAMP, timestamp);
            values.put(Database.Table2.COLUMN_NAME_SCAN_LATITUDE, scanLatitude);
            values.put(Database.Table2.COLUMN_NAME_SCAN_LONGITUDE, scanLongitude);
            values.put(Database.Table2.COLUMN_NAME_LEVEL, level);

            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insertOrThrow(Database.Table2.TABLE_NAME, null, values);
            db.close();
            Log.d("DEBUG", "newRowId: "+newRowId);
            return newRowId;
        }

        // helper method
        // TODO: completare
        private Cursor groupByBssid(SQLiteDatabase db, String bssid){

            String selection = Database.Table1.COLUMN_NAME_BSSID + " = ?";
            String[] selectionArgs = { bssid };
            Cursor cursor = db.query(Database.Table2.TABLE_NAME, null, null, null, null, null, null);
            return cursor; // could be null
        };

        // query5: delete entry in Scan table
        // used to clean up Scan table after many scans (otherwise db becomes too big)
        // e.g. once a day, can keeps 3 measures (most recents) max
        // this method returns true when ap is found and deletion is performed

        // need a method to delete aps on map when are no longer found, because have been
        // removed
        // query6: delete entry in AccessPointInfo table

        // query5 and query6 can be grouped in deleteEntry
        public boolean deleteEntry(String bssid, String tableName) {

            SQLiteDatabase db = this.getWritableDatabase();
            String selection = Database.Table1.COLUMN_NAME_BSSID + " = ?";
            String[] selectionArgs = { bssid };
            int result = db.delete(tableName, selection, selectionArgs);

            db.close();
            // result contains number of rows affected
            if (result > 0) return true;
            else return false;
        }

        // TODO completare
        // query7: used to perform database cleaning, called from service(?)
        public int cleanScanTable() {

            // Cursor cursor = groupByBssid(db, bssid);
            return 1;
        }


        // query8: look for a specific bssid in a table
        // returns true if the searched ap is found
        public boolean searchBssid(String tableName, String bssid) {

            Log.d("DEBUG", "DatabaseHelper: searchBssid()");
            SQLiteDatabase db = this.getReadableDatabase();
            String selection = Database.Table1.COLUMN_NAME_BSSID +" =?";
            String[] selectionArgs = { bssid };
            Cursor cursor = db.query(
                    Database.Table1.TABLE_NAME,
                    null,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null);
            int count = cursor.getCount();
            Log.d("DEBUG", "count: "+count);
            // remember to close db AFTER closing cursor
            cursor.close();
            db.close();
            // check cursor size
            if (count > 0) return true;
            else return false;
        }

        // query9: prints all entries of a table (used for debugging)
        public void printAll(String tableName) {
            Log.d("DEBUG", "DatabaseHelper: printAll()");
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query(tableName, null, null, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    Log.d("DEBUG", cursor.getString(0));
                } while (cursor.moveToNext());
            }

            cursor.close();
            db.close();
        }

        public Cursor getAll(String tableName) {
            Log.d("DEBUG", "DatabaseHelper: getAll()");
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query(tableName, null, null, null, null, null, null);

            return cursor;
        }

        // class used to navigate a set of results (after a query)
        // TODO: consider inserting a switch...case and move here all the queries
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

// add method to delete all
// and give user chance to delete (parts of) db if not needed anymore (?)