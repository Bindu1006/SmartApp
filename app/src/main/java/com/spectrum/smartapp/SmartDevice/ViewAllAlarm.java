package com.spectrum.smartapp.SmartDevice;

import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.spectrum.smartapp.BeanObjects.DeviceAlarmDetails;
import com.spectrum.smartapp.BeanObjects.UserBean;
import com.spectrum.smartapp.ContactActivity;
import com.spectrum.smartapp.EditProfileActivity;
import com.spectrum.smartapp.LoginActivity;
import com.spectrum.smartapp.R;
import com.spectrum.smartapp.SettingsActivity;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;

import java.util.ArrayList;


public class ViewAllAlarm extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DatabaseSqlHelper databaseHelper;
    AlarmAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_device_alarm);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        databaseHelper = new DatabaseSqlHelper(getApplicationContext());

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
    protected void onStart() {
        super.onStart();

        databaseHelper = new DatabaseSqlHelper(getApplicationContext());

        ListView listView = (ListView) findViewById(R.id.id_AlarmList);
        ArrayList<DeviceAlarmDetails> deviceAlarmList = databaseHelper.viewAlarm();

        adapter = new AlarmAdapter(getParent(), this, deviceAlarmList);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());

//        if (id == R.id.action_speech) {
//            startVoiceRecognitionActivity();
//        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_logout) {
            databaseHelper.updateLoginStatus(databaseHelper.getUserDetails().getUserName(), "FALSE");
            Intent intent = new Intent(ViewAllAlarm.this, LoginActivity.class);
            startActivity(intent);
        }  else if (id == R.id.action_profile) {
            Intent intent = new Intent(ViewAllAlarm.this, EditProfileActivity.class);
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


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
