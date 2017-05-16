package com.spectrum.smartapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.spectrum.smartapp.AugmentedReality.CloudManagerAPI;
import com.spectrum.smartapp.BeanObjects.DeviceBean;
import com.spectrum.smartapp.BeanObjects.TargetCollectionDetails;
import com.spectrum.smartapp.BeanObjects.UserBean;
import com.spectrum.smartapp.Utils.AmazonClientManager;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;
import com.spectrum.smartapp.Utils.PlaceAutocompleteAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyStore;
import java.util.UUID;


public class RegistrationDetailsActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    public static AmazonClientManager clientManager = null;

    private AutoCompleteTextView mAutocompleteView;

    protected GoogleApiClient mGoogleApiClient;

    private PlaceAutocompleteAdapter mAdapter;

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
    DatabaseSqlHelper databaseHelper;

    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));

    private static String TAG = "RegistrationDetails";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        clientManager = new AmazonClientManager(this);

        // Construct a GoogleApiClient for the {@link Places#GEO_DATA_API} using AutoManage
        // functionality, which automatically sets up the API client to handle Activity lifecycle
        // events. If your activity does not extend FragmentActivity, make sure to call connect()
        // and disconnect() explicitly.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addApi(Places.GEO_DATA_API)
                .build();

        // Retrieve the AutoCompleteTextView that will display Place suggestions.
        mAutocompleteView = (AutoCompleteTextView)
                findViewById(R.id.autocomplete_places);

        // Register a listener that receives callbacks when a suggestion has been selected
        mAutocompleteView.setOnItemClickListener(mAutocompleteClickListener);

        // Set up the adapter that will retrieve suggestions from the Places Geo Data API that cover
        // the entire world.
        mAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, BOUNDS_GREATER_SYDNEY,
                null);
        mAutocompleteView.setAdapter(mAdapter);

        InitializeAmazonPubSubTask task = new InitializeAmazonPubSubTask();
        task.execute();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

    }

    @Override
    protected void onStart() {
        super.onStart();

        final AlertDialog alertDialog = new AlertDialog.Builder(RegistrationDetailsActivity.this).create();
        alertDialog.setTitle("Error");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final EditText emailText = (EditText) findViewById(R.id.register_email);
        emailText.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    boolean validEmail = isValidEmail(emailText.getText());
                    if (!validEmail) {
                        alertDialog.setMessage("Please provide a valid email id");
                        alertDialog.show();
                    }
                }

            }
        });

        final EditText phNumberText = (EditText) findViewById(R.id.register_phone);
        phNumberText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < 10) {
                    phNumberText.setBackgroundResource(R.drawable.error_edittext);
                } else {
                    phNumberText.setBackgroundResource(R.drawable.edittext);
                }
            }
        });

//        connectToAWSPubSub();
        Button register = (Button) findViewById(R.id.register);
        register.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                doRegisterUser();

            }
        });
    }

    public boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public final static boolean isValidPhone(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return Patterns.PHONE.matcher(target).matches();
        }
    }

    private void doRegisterUser() {

        Log.d("Shruti","Here");
        EditText emailText = (EditText) findViewById(R.id.register_email);
        EditText phNumberText = (EditText) findViewById(R.id.register_phone);

        AlertDialog alertDialog = new AlertDialog.Builder(RegistrationDetailsActivity.this).create();
        alertDialog.setTitle("Error");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });


        //VALIDATIONS
        if (!isValidEmail(emailText.getText())) {
            alertDialog.setMessage("Please provide a valid email id");
            alertDialog.show();
        } else if (!isValidPhone(phNumberText.getText())) {
            alertDialog.setMessage("Please provide a valid Phone number");
            alertDialog.show();
        } else {
            final ProgressBar deviceProgressBar = (ProgressBar) findViewById(R.id.registerProgressBar);
            deviceProgressBar.setVisibility(View.VISIBLE);

            UserBean userDetails = (UserBean) getIntent().getSerializableExtra("USER_DETAILS");
            EditText emailEditText = (EditText) findViewById(R.id.register_email);
            userDetails.setEmail(emailEditText.getText().toString());
            EditText addressEditText = (EditText) findViewById(R.id.autocomplete_places);
            userDetails.setAddress(addressEditText.getText().toString());
            EditText phoneEditText = (EditText) findViewById(R.id.register_phone);
            userDetails.setPhoneNumber(phoneEditText.getText().toString());

            databaseHelper = new DatabaseSqlHelper(getApplicationContext());
            boolean result = databaseHelper.registerUser(userDetails, true);

            if (result) {

                //Publish Users to Raspberry Pi
                final String topic = "$aws/things/RaspberryPi/shadow/update/test";


                ConnectAmazonPubSubTask connectTask = new ConnectAmazonPubSubTask();
                connectTask.execute(userDetails);

            } else {
                alertDialog.setMessage("Unable to add users at this moment. Please try again later.");
                alertDialog.show();
            }
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

    /**
     * Listener that handles selections from suggestions from the AutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     */
    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */
            final AutocompletePrediction item = mAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            Log.i(TAG, "Autocomplete item selected: " + primaryText);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
             details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            Toast.makeText(getApplicationContext(), "Clicked: " + primaryText,
                    Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Called getPlaceById to get Place details for " + placeId);
        }
    };

    /**
     * Called when the Activity could not connect to Google Play services and the auto manager
     * could resolve the error automatically.
     * In this case the API is not available and notify the user.
     *
     * @param connectionResult can be inspected to determine the cause of the failure
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.e(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);

            Log.i(TAG, "Place details received: " + place.getName());

            places.release();
        }
    };

    public class InitializeAmazonPubSubTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Log.e(TAG, "Initialize Pub Sub AWS");
            clientId = UUID.randomUUID().toString();

            credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(), // context
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

            keystorePath = getApplicationContext().getFilesDir().getPath();
            keystoreName = KEYSTORE_NAME;
            keystorePassword = KEYSTORE_PASSWORD;
            certificateId = CERTIFICATE_ID;

            // To load cert/key from keystore on filesystem
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
                            // Create a new private key and certificate. This call
                            // creates both on the server and returns them to the
                            // device.
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

            Toast.makeText(RegistrationDetailsActivity.this, "Done syncing", Toast.LENGTH_LONG).show();

    }

    }

    public class ConnectAmazonPubSubTask extends AsyncTask<UserBean, Void, Void> {

        private boolean connected = false;

        UserBean userDetails;

        @Override
        protected Void doInBackground(final UserBean... userData) {
            userDetails = userData[0];

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
                                msg.put("username", userData[0].getUserName());
                                msg.put("password", userData[0].getPassword());

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
            CreateTargetCollectionTask task = new CreateTargetCollectionTask();
            task.execute(userDetails.getUserName());

        }

    }

    public class CreateTargetCollectionTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... user) {
            // The token to use when connecting to the endpoint
            final String API_TOKEN = "911c630e7b239b3e0ee69d532b93d198";
            // The version of the API we will use
            final int API_VERSION = 2;

            final CloudManagerAPI api = new CloudManagerAPI(API_TOKEN, API_VERSION);
            try {
                JSONObject createdTargetCollection = api.createTargetCollection(user[0]);
                System.out.println(" - tc id:      " + createdTargetCollection.getString("id"));
                System.out.println(" - tc name:    " + createdTargetCollection.getString("name"));
                TargetCollectionDetails collectionDetails = new TargetCollectionDetails();
                collectionDetails.setCollectionID(createdTargetCollection.getString("id"));
                collectionDetails.setCollectionName(createdTargetCollection.getString("name"));

                databaseHelper = new DatabaseSqlHelper(getApplicationContext());
                databaseHelper.addWikitudeTargetCollectionData(user[0], collectionDetails);

            } catch (JSONException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (CloudManagerAPI.APIException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            ProgressBar deviceProgressBar = (ProgressBar) findViewById(R.id.registerProgressBar);
            deviceProgressBar.setVisibility(View.GONE);

            Log.d("Wikitude target", "Target collection created");
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent);
        }

    }

    public void collectionDynamoFailed() {

        ProgressBar deviceProgressBar = (ProgressBar) findViewById(R.id.registerProgressBar);
        deviceProgressBar.setVisibility(View.GONE);

        UserBean userDetails = (UserBean) getIntent().getSerializableExtra("USER_DETAILS");
        databaseHelper.deleteUserData(userDetails.getUserName());
        databaseHelper.deleteWikitudeTargetCollectionData(userDetails.getUserName());
        AlertDialog alertDialog = new AlertDialog.Builder(RegistrationDetailsActivity.this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage("Unable to add user at this moment Please try again later");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();

    }

}

