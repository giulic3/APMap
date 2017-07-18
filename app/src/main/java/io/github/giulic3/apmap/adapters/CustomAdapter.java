package io.github.giulic3.apmap.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import io.github.giulic3.apmap.R;
import io.github.giulic3.apmap.models.CustomMap;
import io.github.giulic3.apmap.helpers.DisplayValueHelper;


public class CustomAdapter extends ArrayAdapter<CustomMap> {

    Context context;
    DisplayValueHelper mDisplayValueHelper;

    public CustomAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mDisplayValueHelper = new DisplayValueHelper();

    }

    public CustomAdapter(Context context, int resource, List<CustomMap> items) {
        super(context, resource, items);
        mDisplayValueHelper = new DisplayValueHelper();

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.ap_list_row, null);
        }

        CustomMap item = getItem(position);

        if (item != null) {
            TextView ssidTv = (TextView) view.findViewById(R.id.list_ssid);
            TextView levelTv = (TextView) view.findViewById(R.id.list_level);

            if (ssidTv != null) {
                ssidTv.setText(item.getSsid());
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