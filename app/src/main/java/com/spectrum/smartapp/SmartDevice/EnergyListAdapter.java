package com.spectrum.smartapp.SmartDevice;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.spectrum.smartapp.BeanObjects.DeviceAlarmDetails;
import com.spectrum.smartapp.BeanObjects.EnergyBean;
import com.spectrum.smartapp.R;

import java.util.ArrayList;

/**
 * Created by shrutib on 5/16/17.
 */

public class EnergyListAdapter extends ArrayAdapter<EnergyBean> {
    String TAG = "EnergyListAdapter";
    private Context thisContext;

    public EnergyListAdapter(Activity act, Context context, ArrayList<EnergyBean> users) {
        super(context, 0, users);
        thisContext = context;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Return the completed view to render on screen

        final EnergyBean energy = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.energy_list_layout, parent, false);
        }

        TextView deviceName = (TextView) convertView.findViewById(R.id.device_name);
        deviceName.setText(energy.getDevice_name());

        TextView alarmStatus = (TextView) convertView.findViewById(R.id.device_usage);
        alarmStatus.setText(energy.getUsage());

        TextView alarmTime = (TextView) convertView.findViewById(R.id.device_status);
        alarmTime.setText(energy.getStatus());
        return convertView;

    }
}
