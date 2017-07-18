package io.github.giulic3.apmap.activities;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import io.github.giulic3.apmap.R;
import io.github.giulic3.apmap.models.CustomMap;
import io.github.giulic3.apmap.adapters.CustomAdapter;

public class ScanResultsActivity extends ListActivity {

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
        // retrieve data from intent that started this activity (then wait for onReceive())
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


            if (scanResultSsids != null) {
                listAdapter.clear();

                for (int i = 0; i < scanResultSsids.size(); i++) {
                    scanResults.add(new CustomMap(scanResultSsids.get(i), scanResultLevels.get(i)));
                }

                listAdapter.notifyDataSetChanged();

            }
        }
    };

}
