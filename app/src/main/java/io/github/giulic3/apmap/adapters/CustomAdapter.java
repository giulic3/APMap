package io.github.giulic3.apmap.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

import io.github.giulic3.apmap.R;
import io.github.giulic3.apmap.models.ListItem;
import io.github.giulic3.apmap.helpers.DisplayValueHelper;


public class CustomAdapter extends ArrayAdapter<ListItem> {

    Context context;
    DisplayValueHelper mDisplayValueHelper;

    public CustomAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mDisplayValueHelper = new DisplayValueHelper();

    }

    public CustomAdapter(Context context, int resource, List<ListItem> items) {
        super(context, resource, items);
        mDisplayValueHelper = new DisplayValueHelper();

    }
    /** Get a View that displays the data at the specified position in the data set. */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.ap_list_row, null);
        }

        ListItem item = getItem(position);

        if (item != null) {
            TextView ssidTv = (TextView) view.findViewById(R.id.list_ssid);
            TextView bssidTv = (TextView) view.findViewById(R.id.list_bssid);
            TextView levelTv = (TextView) view.findViewById(R.id.list_level);

            if (ssidTv != null) {
                ssidTv.setText(item.getSsid());
            }

            if (bssidTv != null) {
                bssidTv.setText(item.getBssid());
            }

            if (levelTv != null) {
                int dbm = item.getLevel();
                int signalQuality = mDisplayValueHelper.convertLevelToPercentage(dbm);

                levelTv.setText(String.valueOf(signalQuality)+"%");
            }
        }
        return view;
    }

}