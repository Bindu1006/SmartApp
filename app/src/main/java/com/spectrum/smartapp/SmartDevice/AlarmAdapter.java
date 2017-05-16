package com.spectrum.smartapp.SmartDevice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.spectrum.smartapp.BeanObjects.DeviceAlarmDetails;
import com.spectrum.smartapp.BeanObjects.DeviceBean;
import com.spectrum.smartapp.R;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by shrutib on 5/13/17.
 */

public class AlarmAdapter  extends ArrayAdapter<DeviceAlarmDetails> {
    String TAG = "AlarmAdapter";
    private Context thisContext;

    public AlarmAdapter(Activity act, Context context, ArrayList<DeviceAlarmDetails> users) {
        super(context, 0, users);
        thisContext = context;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Return the completed view to render on screen

        final DeviceAlarmDetails alarmDetails = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.devices_list_layout, parent, false);
        }

        TextView deviceName = (TextView) convertView.findViewById(R.id.device_name);
        deviceName.setText(alarmDetails.getDeviceName());

        TextView alarmStatus = (TextView) convertView.findViewById(R.id.alarm_status);
        alarmStatus.setText(alarmDetails.getDeviceStatus());

        TextView alarmTime = (TextView) convertView.findViewById(R.id.alarm_time);
        alarmTime.setText(alarmDetails.getAlarmTime());
        return convertView;

    }


}
