package com.spectrum.smartapp.Utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.android.gms.common.api.PendingResult;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.spectrum.smartapp.BeanObjects.UserBean;
import com.spectrum.smartapp.MainActivity;

/**
 * Created by shrutib on 5/14/17.
 */

public class PubNubUtil {

    private Pubnub mPubnub;
    public static final String CHANNEL = "phue";
    Context mContext;

    public void initPubNub(Context context) {
        mPubnub = new Pubnub ("pub-c-29b57268-f02e-4c5b-8839-84ff5374dd60","sub-c-192f74c2-391d-11e7-9361-0619f8945a4f");
        mPubnub.setUUID("phue");
        mContext = context;
        subscribe();

    }

    public void subscribe(){
        try {
            mPubnub.subscribe(CHANNEL, new Callback() {
                @Override
                public void connectCallback(String channel, Object message) {
                    Log.d("PUBNUB", "SUBSCRIBE : CONNECT on channel:" + channel
                            + " : " + message.getClass() + " : "
                            + message.toString());
                }

                @Override
                public void disconnectCallback(String channel, Object message) {
                    Log.d("PUBNUB","SUBSCRIBE : DISCONNECT on channel:" + channel
                            + " : " + message.getClass() + " : "
                            + message.toString());
                }

                public void reconnectCallback(String channel, Object message) {
                    Log.d("PUBNUB","SUBSCRIBE : RECONNECT on channel:" + channel
                            + " : " + message.getClass() + " : "
                            + message.toString());
                }

                @Override
                public void successCallback(String channel, Object message) {
                    Log.d("PUBNUB","SUBSCRIBE : " + channel + " : "
                            + message.getClass() + " : " + message.toString());
                    String smsMmessage = null;
                    if (message.toString().contains("MOTION_DETECTED")) {
                        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(mContext);
                        UserBean user = databaseHelper.getUserDetails();
                        if (user.getPhoneNumber().length()>0) {
                            smsMmessage = "Alert!!! Motion has been detected.";
                            if (databaseHelper.getMotionSettingsData().equalsIgnoreCase("TRUE")) {
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(user.getPhoneNumber(), null, smsMmessage, null, null);
                            }
                        }

                    }
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.d("PUBNUB","SUBSCRIBE : ERROR on channel " + channel
                            + " : " + error.toString());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
