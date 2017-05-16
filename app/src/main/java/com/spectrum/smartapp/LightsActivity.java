package com.spectrum.smartapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
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
import com.spectrum.smartapp.BeanObjects.UserBean;
import com.spectrum.smartapp.SmartDevice.AlarmReceiver;
import com.spectrum.smartapp.SmartDevice.ConfigureSmartDevice;
import com.spectrum.smartapp.SmartDevice.ViewAllAlarm;
import com.spectrum.smartapp.SmartDevice.ViewDeviceAlarm;
import com.spectrum.smartapp.Utils.AmazonClientManager;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;
import com.spectrum.smartapp.Utils.DynamoDbHelper;

import org.json.JSONObject;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class LightsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, TextToSpeech.OnInitListener {

    private static String TAG = "LightsActivity";

    DatabaseSqlHelper databaseHelper;
    private TextToSpeech textToSpeech;
    private boolean _ready = false;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

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

    String switchStatus;
    LightsListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lights);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textToSpeech = new TextToSpeech(this.getApplicationContext(), this);
        databaseHelper = new DatabaseSqlHelper(getApplicationContext());

        clientManager = new AmazonClientManager(this);
//        textToSpeech = new TextToSpeech(this.getApplicationContext(), this);
        InitializeAmazonPubSubTask task = new InitializeAmazonPubSubTask();
        task.execute();

//        startVoiceRecognitionActivity();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        UserBean user = databaseHelper.getUserDetails();

        View header=navigationView.getHeaderView(0);
        TextView settingUsernameTxt = (TextView)header.findViewById(R.id.settingsUsername);
        TextView settingEmail = (TextView)header.findViewById(R.id.settingsEmailView);
        settingUsernameTxt.setText(user.getUserName());
        settingEmail.setText(user.getEmail());
    }

    @Override
    public void onInit(int status) {
        System.out.println("I'm in onInit from Speaker");
        if(status == TextToSpeech.SUCCESS){
            textToSpeech.setLanguage(Locale.ENGLISH);
            _ready = true;
        } else{
            _ready = false;
        }
    }

    void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
                .getPackage().getName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        databaseHelper = new DatabaseSqlHelper(getApplicationContext());

        if (checkIfConfiguredDeviceExist()) {

            ListView listView = (ListView) findViewById(R.id.id_AppliancesList);
            ArrayList<DeviceBean> devicesList = getConnectedSmartDevice();

            adapter = new LightsListAdapter(getParent(), this, devicesList);
            listView.setAdapter(adapter);
            registerForContextMenu(listView);


        } else {
            Intent intent = new Intent(getBaseContext(), ConfigureDeviceActivity.class);
            intent.putExtra("CALLED_FROM", "LIGHTS_MAIN_PAGE");
            startActivity(intent);
        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Log.d("Shruti id: ", v.getId() +" ");

        ListView lv = (ListView) v;
        DeviceBean obj = (DeviceBean) lv.getItemAtPosition(acmi.position);
        Log.d("Shruti id: ", obj.getDeviceName() +" ");
        menu.add(0, v.getId(), 0, "Delete");
        menu.add(0, v.getId(), 0, "Edit");
        menu.add(0, v.getId(), 0, "View Alarm");



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.lights_menu, menu);

        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());
        if (databaseHelper.getLoginStatus().equalsIgnoreCase("FALSE")) {
            MenuItem item = menu.findItem(R.id.action_logout);
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AlertDialog alertDialog = new AlertDialog.Builder(LightsActivity.this).create();
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int itemIndex = info.position;
        final DeviceBean device = adapter.getItem(info.position);
        Log.d("Device: ", device.getDeviceIpAddress());

        if (item.getTitle() == "Delete") {


            alertDialog.setTitle("Delete Device");
            alertDialog.setMessage("Are you sure you want to delete the device?");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            DeleteDeviceDetailsTask deleteTask = new DeleteDeviceDetailsTask();
                            deleteTask.execute(device.getDeviceIpAddress());
                        }
                    });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        } else if (item.getTitle() == "Edit") {
            Intent intent = new Intent(LightsActivity.this, ConfigureSmartDevice.class);
            intent.putExtra("VENDOR_INFO", device.getVendor());
            intent.putExtra("IP_ADDRESS", device.getDeviceIpAddress());
            startActivity(intent);
        } else if (item.getTitle() == "View Alarm") {

            if (databaseHelper.viewDeviceAlarm(device.getDeviceIpAddress()).size() > 0) {
                Intent intent = new Intent(LightsActivity.this, ViewDeviceAlarm.class);
                intent.putExtra("IP_ADDRESS", device.getDeviceIpAddress());
                startActivity(intent);
            } else {
                alertDialog.setTitle("View Alarm");
                alertDialog.setMessage("No alarm set for the requested device!!!");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }

        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        AlertDialog alertDialog = new AlertDialog.Builder(LightsActivity.this).create();

        if (id == R.id.action_speech) {
            Log.d("shruti","here");
            startVoiceRecognitionActivity();
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_add_device) {
            Intent intent = new Intent(getBaseContext(), ConfigureDeviceActivity.class);
            intent.putExtra("CALLED_FROM", "ADD_DEVICE");
            startActivity(intent);
        } else if (id == R.id.action_logout) {
            databaseHelper.updateLoginStatus(databaseHelper.getUserDetails().getUserName(), "FALSE");
            Intent intent = new Intent(LightsActivity.this, LoginActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_view_alarm) {

            if (databaseHelper.viewAlarm().size() > 0) {
                Intent intent = new Intent(LightsActivity.this, ViewAllAlarm.class);
                startActivity(intent);
            } else {
                alertDialog.setTitle("View Alarm");
                alertDialog.setMessage("No alarm set for the any device!!!");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }


        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_side_settings) {
            Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_loc_settings) {
            // TODO Handle the location action
        } else if (id == R.id.nav_contact) {
            Intent intent = new Intent(getBaseContext(), ContactActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private ArrayList<DeviceBean> getConnectedSmartDevice() {
        Log.d("LightsActivity :: ", "Entered to fetch connected devices");

        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());
        ArrayList<DeviceBean> deviceslist = databaseHelper.getAllDeviceDetails();

        return deviceslist;

    }

    private boolean checkIfConfiguredDeviceExist() {
        Log.d("LightsActivity :: ", "check If Configured Device Exist");

        int result = databaseHelper.getDeviceCount();

        if (result > 0) {
            return true;
        } else {
            return false;
        }
    }

    public class DeleteDeviceDetailsTask extends AsyncTask<String, Void, Boolean> {

        String data;

        @Override
        protected Boolean doInBackground(String... ipAddress) {

            data = ipAddress[0];
            String username = databaseHelper.getUserDetails().getUserName();
            boolean result = DynamoDbHelper.deleteDeviceDetails(username + "$#$" +ipAddress[0]);

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                databaseHelper.deleteDeviceData(data);
                restartActivity();
            } else {
                Toast.makeText(LightsActivity.this, "Unable to delete device at this moment. Please try again later",
                        Toast.LENGTH_LONG).show();

            }
        }
    }

    public class InitializeAmazonPubSubTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Log.e(TAG, "Initialize Pub Sub AWS");
            clientId = UUID.randomUUID().toString();

            credentialsProvider = new CognitoCachingCredentialsProvider(
                    LightsActivity.this, // context
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

            keystorePath = LightsActivity.this.getFilesDir().getPath();
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
            Log.d("SHRUTI", "AWS initialize done");

            Toast.makeText(LightsActivity.this, "Done syncing", Toast.LENGTH_LONG).show();


        }

    }

    public class ConnectAmazonPubSubTask extends AsyncTask<String, Void, Void> {

        private boolean connected = false;

        @Override
        protected Void doInBackground(final String... deviceName) {

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
                            Log.d("SHRUTI", "AWS CONNECT done");

                            String ipString = databaseHelper.getDeviceIP(deviceName[0]);

                            try {
                                databaseHelper.updateDeviceStatus(ipString, switchStatus);

                                JSONObject msg = new JSONObject();
                                msg.put("ip_address", ipString);
                                msg.put("cmd", switchStatus);


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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String wordStr = null;
        String[] words = null;
        ArrayList<String> matches = null;
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
                && resultCode == RESULT_OK) {
            matches = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Log.d("Voice Message: ", matches.get(0));
            wordStr = matches.get(0);
            words = wordStr.split(" ");
        }
        ArrayList<String> wordsList = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            wordsList.add(words[i]);
        }

        HashMap<String, String> matchedInfo = new HashMap<>();
        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(LightsActivity.this);
        ArrayList<DeviceBean> deviceslist = databaseHelper.getAllDeviceDetails();
        for (DeviceBean device : deviceslist) {
            if (wordsList.equals(device.getDeviceName())) {
                Log.d("Activity", "Found 1");
                matchedInfo.put("ID", device.getDeviceName());
            } else if (matches.get(0).toLowerCase().contains(device.getDeviceName().toLowerCase())) {
                Log.d("match", "Found 2");
                matchedInfo.put("ID", device.getDeviceName());
            }
        }


        if (matchedInfo.get("ID") != null) {
            if (wordsList.contains("on")) {
                switchStatus = "ON";
                LightsActivity.ConnectAmazonPubSubTask connectTask = new LightsActivity.ConnectAmazonPubSubTask();
                connectTask.execute(matchedInfo.get("ID"));
            } else if (wordsList.contains("off")) {
                switchStatus = "OFF";
                ConnectAmazonPubSubTask connectTask = new ConnectAmazonPubSubTask();
                connectTask.execute(matchedInfo.get("ID"));
            }
        }
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
}
