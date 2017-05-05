package com.example.shrutib.smartapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.example.shrutib.smartapp.BeanObjects.DeviceBean;
import com.example.shrutib.smartapp.Utils.DatabaseSqlHelper;

import java.util.ArrayList;

public class LightsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lights);
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
    protected void onStart() {
        super.onStart();

        if (checkIfConfiguredDeviceExist()) {

            ListView listView =  (ListView) findViewById(R.id.id_AppliancesList);
            ArrayList<DeviceBean> devicesList = getConnectedSmartDevice();

            LightsListAdapter adapter = new LightsListAdapter(getParent(), this, devicesList);
            listView.setAdapter(adapter);

        } else {
            Intent intent = new Intent(getBaseContext(), ConfigureDeviceActivity.class);
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

    private ArrayList<DeviceBean> getConnectedSmartDevice() {
        Log.d("LightsActivity :: ", "Entered to fetch connected devices");

//        ArrayList<String> deviceslist = (ArrayList<String>) getIntent().getSerializableExtra("DEVICESCHEDULERLIST");
        ArrayList<DeviceBean> deviceslist = new ArrayList<DeviceBean>();
        DeviceBean deviceBean1 = new DeviceBean();
        deviceBean1.ipAddress = "10.0.0.80";
        deviceBean1.nicVendor = "Belkin";

        DeviceBean deviceBean2 = new DeviceBean();
        deviceBean2.ipAddress = "10.0.0.81";
        deviceBean2.nicVendor = "Belkin";

        deviceslist.add(deviceBean1);
        deviceslist.add(deviceBean2);

        Log.d("VIEWDEVICE :", deviceslist + " ");
        return deviceslist;

    }

    private boolean checkIfConfiguredDeviceExist() {
        Log.d("LightsActivity :: ", "check If Configured Device Exist");

        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());
        int result = databaseHelper.getDeviceCount();

        if (result > 0) {
            return true;
        } else {
            return false;
        }



    }

}
