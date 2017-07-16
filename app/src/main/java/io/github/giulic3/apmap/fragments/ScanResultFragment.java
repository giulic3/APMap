package io.github.giulic3.apmap.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import io.github.giulic3.apmap.R;
import io.github.giulic3.apmap.data.AccessPoint;
import io.github.giulic3.apmap.data.CustomMap;

import io.github.giulic3.apmap.helpers.CustomAdapter;

import java.util.ArrayList;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ScanResultFragment extends ListFragment {


    private ListView listView;
    private ArrayList<CustomMap> scanResults;
    private CustomAdapter mAdapter;
    private OnListFragmentInteractionListener mListener;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ScanResultFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_scanresult_list,
                    container, false);
            listView = (ListView) rootView.findViewById(android.R.id.list);

            return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mDatabaseUpdatesReceiver,
                new IntentFilter("DatabaseUpdates"));

    }

    @Override
    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mDatabaseUpdatesReceiver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private BroadcastReceiver mDatabaseUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            ArrayList<String> scanResultSsids = intent.getStringArrayListExtra("scanResultSsids");
            ArrayList<Integer> scanResultLevels = intent.getIntegerArrayListExtra("scanResultLevels");
            if (scanResultSsids != null) {
                int size = scanResultSsids.size();
                for (int i = 0; i < size; i++) {
                    scanResults.add(new CustomMap(scanResultSsids.get(i), scanResultLevels.get(i)));
                }
                if (scanResults != null) {
                    mAdapter = new CustomAdapter(getActivity(), android.R.id.list, scanResults);
                    listView.setAdapter(mAdapter);
                }

            }

        }
    };

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(AccessPoint item);
    }
}
