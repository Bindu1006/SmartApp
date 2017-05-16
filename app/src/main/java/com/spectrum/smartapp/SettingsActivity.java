package com.spectrum.smartapp;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.spectrum.smartapp.Utils.DatabaseSqlHelper;

public class SettingsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DatabaseSqlHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        databaseHelper = new DatabaseSqlHelper(getApplicationContext());
        final String locSettings = databaseHelper.getLocationSettingsData();
        final String motionSettings = databaseHelper.getMotionSettingsData();

        final ImageView location = (ImageView) findViewById(R.id.loc_id);
        final TextView txtLocation = (TextView) findViewById(R.id.loc_settings);
        if (locSettings.equalsIgnoreCase("true")) {
            Drawable id = SettingsActivity.this.getResources().getDrawable(R.drawable.home1);
            txtLocation.setText("Back Home? Set below..");
            location.setImageDrawable(id);
        } else {
            Drawable id = SettingsActivity.this.getResources().getDrawable(R.drawable.home2);
            txtLocation.setText("Leaving Home? Set below..");
            location.setImageDrawable(id);
        }

        final ImageView motion = (ImageView) findViewById(R.id.video_id);
        if (motionSettings.equalsIgnoreCase("true")) {
            Drawable id = SettingsActivity.this.getResources().getDrawable(R.drawable.video_on);
            motion.setImageDrawable(id);
        } else {
            Drawable id = SettingsActivity.this.getResources().getDrawable(R.drawable.videocancel);
            motion.setImageDrawable(id);
        }

        location.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String locSettings = databaseHelper.getLocationSettingsData();
                if (locSettings.equalsIgnoreCase("TRUE")) {
                    Log.d("Shruti"," here: "+locSettings);
                    Drawable id = SettingsActivity.this.getResources().getDrawable(R.drawable.home2);
                    txtLocation.setText("Leaving Home? Set below..");
                    location.setImageDrawable(id);
                    databaseHelper.updateLocationSettings("FALSE");
                } else if (locSettings.equalsIgnoreCase("FALSE")) {
                    Drawable id = SettingsActivity.this.getResources().getDrawable(R.drawable.home1);
                    location.setImageDrawable(id);
                    txtLocation.setText("Back Home? Set below..");
                    databaseHelper.updateLocationSettings("TRUE");
                }
            }
        });

        motion.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String motionSettings = databaseHelper.getMotionSettingsData();
                if (motionSettings.equalsIgnoreCase("true")) {
                    Drawable id = SettingsActivity.this.getResources().getDrawable(R.drawable.videocancel);
                    motion.setImageDrawable(id);
                    databaseHelper.updateLocationSettings("FALSE");

                } else if (motionSettings.equalsIgnoreCase("false")) {
                    Drawable id = SettingsActivity.this.getResources().getDrawable(R.drawable.video_on);
                    motion.setImageDrawable(id);
                    databaseHelper.updateMotionSettings("TRUE");
                }
            }
        });
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
}
