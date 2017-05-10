package com.spectrum.smartapp.SmartDevice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
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
import com.spectrum.smartapp.BeanObjects.DeviceAlarmDetails;
import com.spectrum.smartapp.BeanObjects.DeviceBean;
import com.spectrum.smartapp.RegistrationDetailsActivity;
import com.spectrum.smartapp.Utils.AmazonClientManager;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;

import org.json.JSONObject;

import java.security.KeyStore;
import java.util.UUID;

public class AlarmReceiver extends BroadcastReceiver {

    DatabaseSqlHelper databaseHelper;

    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a1hazupsm4spyo.iot.us-west-2.amazonaws.com";
    private static final String COGNITO_POOL_ID = "us-west-2:2b9118fd-a78f-435f-bb53-39ad064584e1";
    private static final String AWS_IOT_POLICY_NAME = "thing_policy";
    private static final Regions MY_REGION = Regions.US_WEST_2;
    private static final String KEYSTORE_NAME = "iot_keystore";
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String CERTIFICATE_ID = "default";

    AWSIotMqttManager mqttManager;
    String clientId;
    AWSIotClient mIotAndroidClient;
    String keystorePath;
    String keystoreName;
    String keystorePassword;
    String certificateId;
    KeyStore clientKeyStore = null;
    public static AmazonClientManager clientManager = null;
    CognitoCachingCredentialsProvider credentialsProvider;

    String ipString;
    String switchStatus;

    Context mContext;


    private static String TAG = "AlarmReceiver";


    public AlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.d("We are in Receiver ", "Hurray!!");
        mContext = context;
        clientManager = new AmazonClientManager(context);

        String deviceIP = intent.getStringExtra("DEVICEIP");
        switchStatus = intent.getStringExtra("SWITCHSTATUS");
        String alarmTime = intent.getStringExtra("ALARMTIME");
        Log.d("DEVICE",deviceIP+" "+switchStatus);

        ipString = deviceIP;

        InitializeAmazonPubSubTask task = new InitializeAmazonPubSubTask();
        task.execute();


        databaseHelper = new DatabaseSqlHelper(context.getApplicationContext());
        if(switchStatus.equalsIgnoreCase("ON")){
            String switchType = "ON";
            Log.d("SWITCH",switchType);
            databaseHelper.updateDeviceStatus(deviceIP, switchType);

            ConnectAmazonPubSubTask connectTask = new ConnectAmazonPubSubTask();
            connectTask.execute();

            //TODO publish to aws
            Toast.makeText(context, "Turned ON", Toast.LENGTH_LONG).show();
        } else {
            String switchType = "OFF";
            Log.d("SWITCH",switchType);
            databaseHelper.updateDeviceStatus(deviceIP, switchType);

            ConnectAmazonPubSubTask connectTask = new ConnectAmazonPubSubTask();
            connectTask.execute();

            //TODO publish to aws
            Toast.makeText(context, "Turned OFF", Toast.LENGTH_LONG).show();
        }
        DeviceBean deviceDetails = databaseHelper.getDeviceDetails(deviceIP);

        DeviceAlarmDetails schedulerDetails = new DeviceAlarmDetails();
        schedulerDetails.setAlarmTime(alarmTime);
        schedulerDetails.setDeviceName(deviceDetails.getDeviceName());
        schedulerDetails.setDeviceIP(deviceDetails.getDeviceIpAddress());
        schedulerDetails.setDeviceStatus(switchStatus);
        databaseHelper.deleteAlarmEntry(schedulerDetails);
    }


    public class InitializeAmazonPubSubTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Log.e(TAG, "Initialize Pub Sub AWS");
            clientId = UUID.randomUUID().toString();

            credentialsProvider = new CognitoCachingCredentialsProvider(
                    mContext, // context
                    COGNITO_POOL_ID, // Identity Pool ID
                    MY_REGION // Region
            );
            Region region = Region.getRegion(MY_REGION);

            mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

            mqttManager.setKeepAlive(10);

            AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                    "Android client lost connection", AWSIotMqttQos.QOS0);
            mqttManager.setMqttLastWillAndTestament(lwt);

            mIotAndroidClient = new AWSIotClient(credentialsProvider);
            mIotAndroidClient.setRegion(region);
            mIotAndroidClient.setEndpoint("iot.us-west-2.amazonaws.com");

            keystorePath = mContext.getFilesDir().getPath();
            keystoreName = KEYSTORE_NAME;
            keystorePassword = KEYSTORE_PASSWORD;
            certificateId = CERTIFICATE_ID;

            try {
                if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                    if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                            keystoreName, keystorePassword)) {
                        Log.i(TAG, "Certificate " + certificateId
                                + " found in keystore - using for MQTT.");
                        // load keystore from file into memory to pass on connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);
                    } else {
                        Log.i(TAG, "Key/cert " + certificateId + " not found in keystore.");
                    }
                } else {
                    Log.i(TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
                }
            } catch (Exception e) {
                Log.e(TAG, "An error occurred retrieving cert/key from keystore.", e);
            }

            if (clientKeyStore == null) {
                Log.i(TAG, "Cert/key was not found in keystore - creating new key and certificate.");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                    new CreateKeysAndCertificateRequest();
                            createKeysAndCertificateRequest.setSetAsActive(true);
                            final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                            createKeysAndCertificateResult =
                                    mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                            Log.i(TAG,
                                    "Cert ID: " +
                                            createKeysAndCertificateResult.getCertificateId() +
                                            " created.");

                            AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                    createKeysAndCertificateResult.getCertificatePem(),
                                    createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                    keystorePath, keystoreName, keystorePassword);

                            clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                    keystorePath, keystoreName, keystorePassword);

                            AttachPrincipalPolicyRequest policyAttachRequest =
                                    new AttachPrincipalPolicyRequest();
                            policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                            policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                    .getCertificateArn());
                            mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e(TAG, "set enabled is true");
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG,
                                    "Exception occurred when generating new private key and certificate.",
                                    e);
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            Log.d("SHRUTI","AWS initialize done");

            Toast.makeText(mContext, "Done syncing", Toast.LENGTH_LONG).show();


        }

    }

    public class ConnectAmazonPubSubTask extends AsyncTask<Void, Void, Void> {

        private boolean connected = false;

        @Override
        protected Void doInBackground(Void... params) {

            //        Connect to Amazon IOS
            try {
                mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(TAG, "Status = " + String.valueOf(status));

                        if (status == AWSIotMqttClientStatus.Connected) {
                            Log.e(TAG, "Connected");
                            connected = true;
                            final String topic = "$aws/things/RaspberryPi/shadow/update/test";
                            Log.d("SHRUTI","AWS CONNECT done");

                            try {

                                JSONObject msg = new JSONObject();
                                    msg.put("ip_address", ipString);

                                    if (switchStatus.equalsIgnoreCase("OFF")) {
                                        msg.put("cmd", "ON");
                                    } else {
                                        msg.put("cmd", "OFF");
                                    }

                                mqttManager.publishString(msg.toString(), topic, AWSIotMqttQos.QOS0);
                            } catch (Exception e) {
                                Log.e(TAG, "Publish error.", e);
                            }
                        }

                    }
                });
            } catch (final Exception e) {
                Log.e(TAG, "Connection error.", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

        }

    }

}
