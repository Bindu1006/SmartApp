package com.spectrum.smartapp;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.spectrum.smartapp.Utils.AmazonClientManager;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;
import com.spectrum.smartapp.Utils.PubNubUtil;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        TextToSpeech.OnInitListener {

    private static String TAG = "MainActivity";

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    final static String TARGET_BASE_PATH = "/sdcard/myDB/";

    DatabaseSqlHelper databaseHelper;
    private TextToSpeech textToSpeech;
    private boolean _ready = false;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToSpeech = new TextToSpeech(this.getApplicationContext(), this);

        PubNubUtil util = new PubNubUtil();
        util.initPubNub(MainActivity.this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        databaseHelper = new DatabaseSqlHelper(getApplicationContext());

        InitializeAmazonPubSubTask task = new InitializeAmazonPubSubTask();
        task.execute();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        UserBean user = databaseHelper.getUserDetails();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


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

        copyFile();

        databaseHelper = new DatabaseSqlHelper(getApplicationContext());
        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("This will redirect to your default browser. Do you want to continue?");

        if (databaseHelper.getLocationSettingsData() == null) {
            databaseHelper.setSettingsData(databaseHelper.getUserDetails().getUserName(), "FALSE", "FALSE");
        }

        Button loginButton = (Button) findViewById(R.id.lightBtn);
        loginButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), LightsActivity.class);
                startActivity(intent);
            }
        });

        Button augmentedButton = (Button) findViewById(R.id.augmentedBtn);
        augmentedButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), AugmentedMainActivity.class);
                startActivity(intent);
            }
        });

        Button settingsButton = (Button) findViewById(R.id.settingsBtn);
        settingsButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        Button dashboardButton = (Button) findViewById(R.id.dashboardBtn);
        dashboardButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.justsmartthings.com/TestWebProject/"));
                                startActivity(browserIntent);

                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();


            }
        });

        Button videoButton = (Button) findViewById(R.id.videoBtn);
        videoButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://10.0.0.83/stream.html"));
                                startActivity(browserIntent);

                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });



    }

    @Override
    public void onBackPressed() {
        Log.d("SHRUTI","Call finish");
        MainActivity.this.finishAffinity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());
        getMenuInflater().inflate(R.menu.login2, menu);

        if (databaseHelper.getLoginStatus().equalsIgnoreCase("TRUE")) {
            MenuItem item = menu.findItem(R.id.action_logout);
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());

        if (id == R.id.action_speech) {
            startVoiceRecognitionActivity();
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_logout) {
            databaseHelper.updateLoginStatus(databaseHelper.getUserDetails().getUserName(), "FALSE");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }  else if (id == R.id.action_profile) {
            Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
            startActivity(intent);
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

    private void copyFile() {

        InputStream in = getResources().openRawResource(R.raw.oui);
        FileOutputStream out = null;

        byte[] buff = new byte[1024];
        int read = 0;
        try {
            out = new FileOutputStream(TARGET_BASE_PATH + "/oui.db");
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Log.d("SHRUTI","here");
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String wordStr = null;
        String[] words = null;
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
                && resultCode == RESULT_OK) {
            ArrayList<String> matches = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Log.d("Voice Message: ", matches.get(0));
            wordStr = matches.get(0);
            words = wordStr.split(" ");
        }
        ArrayList<String> wordsList = new ArrayList<>();
        if (words != null) {
            for (int i = 0; i < words.length; i++) {
                wordsList.add(words[i]);
            }
        }

        if (wordsList.contains("lights") || wordsList.contains("light") ||
                wordsList.contains("device") || wordsList.contains("smart") ||
                wordsList.contains("smart")) {
            if (wordsList.contains("open") || wordsList.contains("show")) {

                try {
                    Intent intent = new Intent(getBaseContext(), LightsActivity.class);
                    textToSpeech.speak("Opened Lights Activity", TextToSpeech.QUEUE_FLUSH, null);
                    this.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Exception: ", e.getMessage());
                }
            } else if (wordsList.contains("close") || wordsList.contains("quit") || wordsList.contains("cancel")
                    || wordsList.contains("exit") || wordsList.contains("finish")) {
                try {
                    Log.d("SHRUTI","Call finish");
                    textToSpeech.speak("Closed Activity", TextToSpeech.QUEUE_FLUSH, null);
                    MainActivity.this.finishAffinity();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Exception: ", e.getMessage());
                }
            }
        } else if (wordsList.contains("surveillance") || wordsList.contains("video") ||
                wordsList.contains("security") || wordsList.contains("secure") ||
                wordsList.contains("motion")) {
            if (wordsList.contains("open") || wordsList.contains("show")) {
                try {
//                    Intent intent = new Intent(getBaseContext(), Surveillance.class);
                    textToSpeech.speak("Opened Lights Activity", TextToSpeech.QUEUE_FLUSH, null);
//                    this.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Exception: ", e.getMessage());
                }
            } else if (wordsList.contains("close") || wordsList.contains("quit") || wordsList.contains("cancel")
                    || wordsList.contains("exit") || wordsList.contains("finish")) {
                try {
                    Log.d("SHRUTI","Call finish");
                    textToSpeech.speak("Closed Activity", TextToSpeech.QUEUE_FLUSH, null);
                    MainActivity.this.finishAffinity();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Exception: ", e.getMessage());
                }
            }
        } else if (wordsList.contains("augmented") || wordsList.contains("virtual") ||
                wordsList.contains("camera") || wordsList.contains("control") ||
                wordsList.contains("AR")) {
            if (wordsList.contains("open") || wordsList.contains("show")) {
                try {
                    Intent intent = new Intent(getBaseContext(), AugmentedMainActivity.class);
                    textToSpeech.speak("Opened Augmented Activity", TextToSpeech.QUEUE_FLUSH, null);
                    this.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Exception: ", e.getMessage());
                }
            } else if (wordsList.contains("close") || wordsList.contains("quit") || wordsList.contains("cancel")
                    || wordsList.contains("exit") || wordsList.contains("finish")) {
                try {
                    Log.d("SHRUTI","Call finish");
                    textToSpeech.speak("Closed Activity", TextToSpeech.QUEUE_FLUSH, null);
                    MainActivity.this.finishAffinity();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Exception: ", e.getMessage());
                }
            }
        } else if (wordsList.contains("analytics") || wordsList.contains("dashboard") ||
                wordsList.contains("view") || wordsList.contains("visualize") ||
                wordsList.contains("data")) {
            if (wordsList.contains("open") || wordsList.contains("show")) {
                try {
//                    Intent intent = new Intent(getBaseContext(), dash.class);
                    textToSpeech.speak("Opened Dashboard Activity", TextToSpeech.QUEUE_FLUSH, null);
//                    this.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Exception: ", e.getMessage());
                }
            } else if (wordsList.contains("close") || wordsList.contains("quit") || wordsList.contains("cancel")
                    || wordsList.contains("exit") || wordsList.contains("finish")) {
                try {
                    Log.d("SHRUTI", "Call finish");
                    textToSpeech.speak("Closed Activity", TextToSpeech.QUEUE_FLUSH, null);
                    MainActivity.this.finishAffinity();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Exception: ", e.getMessage());
                }
            }
        }


        ArrayList<String> matches = null;
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
                && resultCode == RESULT_OK) {
            matches = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Log.d("Voice Message: ", matches.get(0));
            wordStr = matches.get(0);
            words = wordStr.split(" ");
        }
        for (int i = 0; i < words.length; i++) {
            wordsList.add(words[i]);
        }

        HashMap<String, String> matchedInfo = new HashMap<>();
        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(MainActivity.this);
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

        String switchStatus = "OFF";
        String deviceData = "";
        if (matchedInfo.get("ID") != null) {
            if (wordsList.contains("on")) {
                switchStatus = "ON";
                deviceData = matchedInfo.get("ID") + "$#$" + switchStatus;
                ConnectAmazonPubSubTask connectTask = new ConnectAmazonPubSubTask();
                connectTask.execute(deviceData);
            } else if (wordsList.contains("off")) {
                switchStatus = "OFF";
                deviceData = matchedInfo.get("ID") + "$#$" + switchStatus;
                ConnectAmazonPubSubTask connectTask = new ConnectAmazonPubSubTask();
                connectTask.execute(deviceData);
            }
        }
    }

    public class ConnectAmazonPubSubTask extends AsyncTask<String, Void, Void> {

        private boolean connected = false;

        @Override
        protected Void doInBackground(final String... deviceData) {

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

                            String deviceName = (deviceData[0].split("$#$"))[0];
                            String switchStatus = (deviceData[0].split("$#$"))[1];
                            String ipString = databaseHelper.getDeviceIP(deviceName);

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


    public class InitializeAmazonPubSubTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Log.e(TAG, "Initialize Pub Sub AWS");
            clientId = UUID.randomUUID().toString();

            credentialsProvider = new CognitoCachingCredentialsProvider(
                    MainActivity.this, // context
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

            keystorePath = MainActivity.this.getFilesDir().getPath();
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

            Toast.makeText(MainActivity.this, "Done syncing", Toast.LENGTH_LONG).show();


        }

    }

}
