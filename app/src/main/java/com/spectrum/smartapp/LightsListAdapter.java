package com.spectrum.smartapp;

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
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.spectrum.smartapp.AugmentedReality.AugmentedMainActivity;
import com.spectrum.smartapp.BeanObjects.DeviceBean;
import com.spectrum.smartapp.SmartDevice.DeviceAlarmActivity;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;

import org.json.JSONObject;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by shrutib on 4/13/17.
 */

public class LightsListAdapter extends ArrayAdapter<DeviceBean> {

    String LOG_TAG = "LightsListAdapter";

    private Context thisContext;
    private Activity thisActivity;

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a1hazupsm4spyo.iot.us-west-2.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "us-west-2:2b9118fd-a78f-435f-bb53-39ad064584e1";

    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "thing_policy";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_WEST_2;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStoref
    private static final String CERTIFICATE_ID = "default";

    AWSIotMqttManager mqttManager;
    String clientId;
    AWSIotClient mIotAndroidClient;
    String keystorePath;
    String keystoreName;
    String keystorePassword;

    String certificateId;
    KeyStore clientKeyStore = null;
    CognitoCachingCredentialsProvider credentialsProvider;


    public LightsListAdapter(Activity act, Context context, ArrayList<DeviceBean> users) {
        super(context, 0, users);
        thisContext = context;
        thisActivity = act;
        clientId = UUID.randomUUID().toString();

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );
        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);
        mIotAndroidClient.setEndpoint("iot.us-west-2.amazonaws.com");

        keystorePath = getContext().getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e(LOG_TAG, "set enabled is true");
                            }
                        });
                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final String ipString = getItem(position).getDeviceIpAddress() ;
        final String deviceName = getItem(position).getDeviceName();
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.devices_list_layout, parent, false);
        }

//        Connect to Amazon IOS
        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                Log.e(LOG_TAG, "Connecting...");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                Log.e(LOG_TAG, "Connected");

                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                Log.e(LOG_TAG, "Reconnecting");
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                Log.e(LOG_TAG, "Disconnected");
                            } else {
                                Log.e(LOG_TAG, "Disconnected");

                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
        }

        // Lookup view for data population
        TextView ipAddress = (TextView) convertView.findViewById(R.id.device_name);

        // Populate the data into the template view using the data object
        ipAddress.setText(deviceName);


        final ImageView imageView = (ImageView) convertView.findViewById(R.id.control_device);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(thisContext);
                String status = databaseHelper.getDeviceStatus(ipString);
                final String topic = "$aws/things/RaspberryPi/shadow/update/test";

                try {

                    JSONObject msg = new JSONObject();
                    msg.put("ip_address", ipString);

                    if (status.equalsIgnoreCase("OFF")) {
                        msg.put("cmd", "ON");
                    } else {
                        msg.put("cmd", "OFF");
                    }

                    mqttManager.publishString(msg.toString(), topic, AWSIotMqttQos.QOS0);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Publish error.", e);
                }

                if (status.equalsIgnoreCase("OFF")) {
                    Drawable id = getContext().getResources().getDrawable(R.drawable.switch_on);
                    imageView.setImageDrawable(id);
                    databaseHelper.updateDeviceStatus(ipString, "ON");
                } else {
                    Drawable id = getContext().getResources().getDrawable(R.drawable.switch_off);
                    imageView.setImageDrawable(id);
                    databaseHelper.updateDeviceStatus(ipString, "OFF");
                }

                Toast.makeText(getContext(), "Data Published", Toast.LENGTH_LONG).show();
            }

        });

        final ImageView alarmImageView = (ImageView) convertView.findViewById(R.id.alarm_device);
        alarmImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), DeviceAlarmActivity.class);

                intent.putExtra("DEVICE_IP_ADDRESS", ipString);
                getContext().startActivity(intent);

            }
        });


        // Return the completed view to render on screen
        return convertView;

    }

}
