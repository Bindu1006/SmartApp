package com.example.shrutib.smartapp.SmartDevice;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.shrutib.smartapp.BeanObjects.DeviceBean;
import com.example.shrutib.smartapp.BeanObjects.UserBean;
import com.example.shrutib.smartapp.LightsActivity;
import com.example.shrutib.smartapp.R;
import com.example.shrutib.smartapp.Utils.DatabaseSqlHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ConfigureSmartDevice extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri imageUri;
    public final static String APP_PATH_SD_CARD = "/smart_home/";
    public final static String APP_THUMBNAIL_PATH_SD_CARD = "augmented_images";
    private String DEVICE_NAME;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_smart_device);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    protected void onStart() {
        super.onStart();

        final String vendorInfo = getIntent().getStringExtra("VENDOR_INFO");
        final String ipAddress = getIntent().getStringExtra("IP_ADDRESS");

        Button storeDeviceDetails = (Button) findViewById(R.id.store_device);
        storeDeviceDetails.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                EditText deviceNameText = (EditText) findViewById(R.id.device_name);
                String deviceName = deviceNameText.getText().toString();

                if (vendorInfo != null && ipAddress != null) {
                    DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());

                    DeviceBean deviceBean = new DeviceBean();
                    deviceBean.setDeviceIpAddress(ipAddress);
                    deviceBean.setVendor(vendorInfo);
                    deviceBean.setDeviceName(deviceName);

                    String status = databaseHelper.getDeviceStatus(ipAddress);
                    if (status.equalsIgnoreCase("OFF")) {
                        deviceBean.setDeviceStatus("ON");
                    } else {
                        deviceBean.setDeviceStatus("OFF");
                    }

                    databaseHelper.insertSmartDeviceData(deviceBean);
                }

            }
        });

        RelativeLayout uploadImage = (RelativeLayout) findViewById(R.id.pic_upload);
        uploadImage.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

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

        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            OutputStream fOut = null;
            if (DEVICE_NAME != null) {
                File file = new File(fullPath, DEVICE_NAME + ".png");
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
}
