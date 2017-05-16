package com.spectrum.smartapp.SmartDevice;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.spectrum.smartapp.AugmentedReality.CloudManagerAPI;
import com.spectrum.smartapp.BeanObjects.DeviceBean;
import com.spectrum.smartapp.BeanObjects.TargetCollectionDetails;
import com.spectrum.smartapp.BeanObjects.UserBean;
import com.spectrum.smartapp.ConfigureDeviceActivity;
import com.spectrum.smartapp.ContactActivity;
import com.spectrum.smartapp.LightsActivity;
import com.spectrum.smartapp.MainActivity;
import com.spectrum.smartapp.R;
import com.spectrum.smartapp.SettingsActivity;
import com.spectrum.smartapp.Utils.AmazonClientManager;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ConfigureSmartDevice extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri imageUri;
    public final static String APP_PATH_SD_CARD = "/smart_home/";
    public final static String APP_THUMBNAIL_PATH_SD_CARD = "augmented_images";
    private String DEVICE_NAME;
    public static AmazonClientManager clientManager = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_smart_device);
        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        clientManager = new AmazonClientManager(this);

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
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login2, menu);

        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());
        if (databaseHelper.getLoginStatus().equalsIgnoreCase("FALSE")) {
            MenuItem item = menu.findItem(R.id.action_logout);
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
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


    @Override
    protected void onStart() {
        super.onStart();
        final DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());

        final String vendorInfo = getIntent().getStringExtra("VENDOR_INFO");
        final String ipAddress = getIntent().getStringExtra("IP_ADDRESS");

        Button storeDeviceDetails = (Button) findViewById(R.id.store_device);
        storeDeviceDetails.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                EditText deviceNameText = (EditText) findViewById(R.id.device_name);
                String deviceName = deviceNameText.getText().toString();

                if (vendorInfo != null && ipAddress != null) {

                    DeviceBean deviceBean = new DeviceBean();
                    deviceBean.setDeviceIpAddress(ipAddress);
                    deviceBean.setVendor(vendorInfo);
                    deviceBean.setDeviceName(deviceName);

                    deviceBean.setDeviceStatus("OFF");

//                    String status = databaseHelper.getDeviceStatus(ipAddress);
//                    if (status.equalsIgnoreCase("OFF")) {
//                        deviceBean.setDeviceStatus("ON");
//                    } else {
//                        deviceBean.setDeviceStatus("OFF");
//                    }

                    databaseHelper.insertSmartDeviceData(deviceBean);



                }

            }
        });

        RelativeLayout uploadImage = (RelativeLayout) findViewById(R.id.pic_upload);
        uploadImage.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                //Create a target collection
//                CreateTargetCollectionTask task = new CreateTargetCollectionTask();
//                task.execute();

                if (checkCameraHardware(getApplicationContext())) {
                    EditText emailEditText = (EditText) findViewById(R.id.device_name);
                    DEVICE_NAME = emailEditText.getText().toString();
                    takePictureIntent();
                } else {
                    //    TODO Show error dialog Camera not supported by device
                }

            }
        });
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private void takePictureIntent() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedImage = imageUri;
                    getContentResolver().notifyChange(selectedImage, null);
                    ContentResolver cr = getContentResolver();
                    Bitmap bitmap;
                    try {
                        bitmap = android.provider.MediaStore.Images.Media
                                .getBitmap(cr, selectedImage);

                        saveImageToInternalStorage(bitmap);

                        //Upload image on S3
                        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_PATH_SD_CARD + APP_THUMBNAIL_PATH_SD_CARD;
                        AddImageToS3Task addImageToS3Task = new AddImageToS3Task();
                        addImageToS3Task.execute(fullPath);

                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to Save the image", Toast.LENGTH_SHORT)
                                .show();
                        Log.e("Camera", e.toString());
                    }

                }
        }
    }

    public boolean saveImageToInternalStorage(Bitmap image) {
        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_PATH_SD_CARD + APP_THUMBNAIL_PATH_SD_CARD;
        String ipAddress = getIntent().getStringExtra("IP_ADDRESS");

        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            OutputStream fOut = null;
            if (DEVICE_NAME != null && ipAddress != null) {
                File file = new File(fullPath, ipAddress + ".png");
                Log.d(file.getName(), " file ");
                file.createNewFile();
                fOut = new FileOutputStream(file);

                image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();
                fOut.close();

                // 100 means no compression, the lower you go, the stronger the compression
                MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

                return true;
            } else {
//                TODO send alert to say no image found
                return false;
            }

        } catch (Exception e) {
            Log.e("saveToExternalStorage()", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public class AddImageToS3Task extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... imageFile) {

            String ipAddress = getIntent().getStringExtra("IP_ADDRESS");
            Log.d("File: ", imageFile[0] + " add the image on s3: "+ ipAddress);

            // Create an S3 client
            String COGNITO_POOL_ID = "us-west-2:2b9118fd-a78f-435f-bb53-39ad064584e1";
            Regions MY_REGION = Regions.US_WEST_2;
            File file = new File(imageFile[0] + "/" + ipAddress + ".png");

            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    ConfigureSmartDevice.this, // context
                    COGNITO_POOL_ID, // Identity Pool ID
                    MY_REGION // Region
            );
            AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
            TransferUtility transferUtility = new TransferUtility(s3, ConfigureSmartDevice.this);

            TransferObserver observer = transferUtility.upload(
                    "augmented-pool",     /* The bucket to upload to */
                    ipAddress +".png",    /* The key for the uploaded object */
                    file     /* The file where the data to upload exists */
            );

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            AddTargetTask addTargetTask = new AddTargetTask();
            addTargetTask.execute();
        }
    }

    public class AddTargetTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... xyz) {
            // The token to use when connecting to the endpoint
            final String API_TOKEN = "911c630e7b239b3e0ee69d532b93d198";
            // The version of the API we will use
            final int API_VERSION = 2;

            String imageURL = "https://s3-us-west-2.amazonaws.com/augmented-pool/";
            String ipAddress = getIntent().getStringExtra("IP_ADDRESS");
            imageURL = imageURL + ipAddress + ".png";

            final CloudManagerAPI api = new CloudManagerAPI(API_TOKEN, API_VERSION);
            try {
                DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());
                TargetCollectionDetails data = databaseHelper.getWikitudeTargetCollectionData();
                Log.d("SHRUTI",data + " ");


                final JSONObject target = new JSONObject();
                target.put("name", ipAddress);
                target.put("imageUrl", imageURL);
                Log.d("SHRUTI","Full path: " + imageURL);

                final JSONObject createdTarget = api.addTarget(data.getCollectionID(), target);
                System.out.println("________________________");
                System.out.println(" - target id:      " + createdTarget.getString("id"));
                System.out.println(" - target name:    " + createdTarget.getString("name"));
                System.out.println("________________________");

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

            Log.d("Wikitude target", "Target collection created");
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent);
        }

    }

}
