package io.github.giulic3.apmap.activities;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import io.github.giulic3.apmap.R;
import io.github.giulic3.apmap.models.ListItem;
import io.github.giulic3.apmap.adapters.CustomAdapter;
import io.github.giulic3.apmap.services.ApService;
import io.github.giulic3.apmap.services.LocationService;

public class ScanResultsActivity extends ListActivity {

    private ArrayList<ListItem> scanResults;
    private ArrayList<String> scanResultSsids;
    private ArrayList<String> scanResultBssids;
    private ArrayList<Integer> scanResultLevels;

    private CustomAdapter listAdapter;
    private FloatingActionButton syncButton;
    private Animation rotation;


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
        scanResultBssids = intent.getStringArrayListExtra("scanResultBssids");
        scanResultLevels = intent.getIntegerArrayListExtra("scanResultLevels");


        //register broadcast receiver
        LocalBroadcastManager.getInstance(ScanResultsActivity.this).registerReceiver(mDatabaseUpdatesReceiver,
                new IntentFilter("DatabaseUpdates"));
        // setup scanresults arraylist
        if (scanResultSsids != null) {
            for (int i = 0; i < scanResultSsids.size(); i++) {
                scanResults.add(new ListItem(scanResultSsids.get(i), scanResultBssids.get(i), scanResultLevels.get(i)));
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

        syncButton = (FloatingActionButton) findViewById(R.id.sync_fab_id);
        if (syncButton != null) {
            syncButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Log.d("DEBUG", "ScanResultsActivity syncing");
                    startService(new Intent(ScanResultsActivity.this, ApService.class));
                    rotation = AnimationUtils.loadAnimation(ScanResultsActivity.this, R.anim.rotation);
                    syncButton.startAnimation(rotation);

                }
            });
        }
    }

    @Override
    protected void onStop(){
        Log.d("DEBUG", "ScanResultsActivity: onStop()");
        super.onStop();
        LocalBroadcastManager.getInstance(ScanResultsActivity.this).unregisterReceiver(mDatabaseUpdatesReceiver);

    }
    // listens for broadcast intent from UpdateDbTask, updating list when new scan results are available
    private BroadcastReceiver mDatabaseUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("DEBUG", "ScanResultsActivity: mDatabaseUpdatesReceiver onReceive");

            scanResultSsids = intent.getStringArrayListExtra("scanResultSsids");
            scanResultBssids = intent.getStringArrayListExtra("scanResultBssids");
            scanResultLevels = intent.getIntegerArrayListExtra("scanResultLevels");

            if (scanResultSsids != null) {
                listAdapter.clear();

                for (int i = 0; i < scanResultSsids.size(); i++) {
                    scanResults.add(new ListItem(scanResultSsids.get(i), scanResultBssids.get(i),
                            scanResultLevels.get(i)));
                }
                // notify adapter that content changed
                listAdapter.notifyDataSetChanged();
                if (syncButton != null) {
                    syncButton.clearAnimation();
                }
            }
        }
    };

}
