package io.github.giulic3.apmap.activities;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import io.github.giulic3.apmap.R;
import io.github.giulic3.apmap.data.CustomMap;
import io.github.giulic3.apmap.data.Database;
import io.github.giulic3.apmap.helpers.CustomAdapter;

public class ScanResultsActivity extends ListActivity {

    private ListView listView;
    private ArrayList<CustomMap> scanResults;
    private CustomAdapter listAdapter;
    ArrayList<String> scanResultSsids;
    ArrayList<Integer> scanResultLevels;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("DEBUG", "ScanResultsActivity: onCreate()");

        super.onCreate(savedInstanceState);
        // inflate layout
        setContentView(R.layout.activity_scan_results);
        ListView listView = (ListView) findViewById(android.R.id.list);
        scanResults = new ArrayList<>();
        //mixing two approaches: intent + broadcast receiver
        // retrieve data from intent that started this activity
        Intent intent = getIntent();
        scanResultSsids = intent.getStringArrayListExtra("scanResultSsids");
        scanResultLevels = intent.getIntegerArrayListExtra("scanResultLevels");


        //register broadcast receiver
        LocalBroadcastManager.getInstance(ScanResultsActivity.this).registerReceiver(mDatabaseUpdatesReceiver,
                new IntentFilter("DatabaseUpdates"));
        // setup scanresults arraylist
        if (scanResultSsids != null) {
            for (int i = 0; i < scanResultSsids.size(); i++) {
                scanResults.add(new CustomMap(scanResultSsids.get(i), scanResultLevels.get(i)));
            }
        }
        // setup adapter
        if (scanResults != null) {
            listAdapter = new CustomAdapter(getApplicationContext(), R.layout.ap_list_row, scanResults);
            listView.setAdapter(listAdapter);
        }
        // setting listener on list items
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("DEBUG", "ScanResultsActivity: onItemClick()");
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });
        /*
        // setup scanresults arraylist
        if (scanResultSsids != null) {
            for (int i = 0; i < scanResultSsids.size(); i++){
                scanResults.add( new CustomMap(scanResultSsids.get(i), scanResultLevels.get(i)));
            }


            // TODO: ci starebbe bene un blocco try/catch


        }
        */
    }

    @Override
    protected void onStop(){
        super.onStop();
        LocalBroadcastManager.getInstance(ScanResultsActivity.this).unregisterReceiver(mDatabaseUpdatesReceiver);

    }

    private BroadcastReceiver mDatabaseUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanResultSsids = intent.getStringArrayListExtra("scanResultSsids");
            scanResultLevels = intent.getIntegerArrayListExtra("scanResultLevels");

            listAdapter.clear(); // i clear all data

            if (scanResultSsids != null) {
                for (int i = 0; i < scanResultSsids.size(); i++) {
                    scanResults.add(new CustomMap(scanResultSsids.get(i), scanResultLevels.get(i)));
                }

                listAdapter.notifyDataSetChanged(); // first change
                //listAdapter.notifyDataSetChanged();  // second change
                // TODO: così funziona ma sta troppo tempo ad aspettare la lista...
            }
        }
    };

}
