package com.example.shrutib.smartapp.SmartDevice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.shrutib.smartapp.BeanObjects.DeviceBean;
import com.example.shrutib.smartapp.R;
import com.example.shrutib.smartapp.Utils.DatabaseSqlHelper;

import java.util.Calendar;

public class DeviceAlarmActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener  {

    TimePicker alarmTimePicker;
    DatabaseSqlHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_alarm);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());
        getMenuInflater().inflate(R.menu.alarm_menu, menu);

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_submit) {
            setAlarmForDevice();
        }


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

    private void setAlarmForDevice() {
        Bundle bundle = getIntent().getExtras();
        String deviceIpAddress = bundle.getString("DEVICE_IP_ADDRESS");
        Log.d("Alarm page: ",deviceIpAddress+" ");

        //create an instance of calendar
        Calendar calendar = Calendar.getInstance();

        // initializing time picker
        alarmTimePicker = (TimePicker) findViewById(R.id.time_picker);

        calendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
        calendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());

        int hour_picked;
        int minutes_picked;
        hour_picked = alarmTimePicker.getCurrentHour();
        minutes_picked = alarmTimePicker.getCurrentMinute();

        //converts the values from int to string
        String hour_value = String.valueOf(hour_picked);
        String minute_value = String.valueOf(minutes_picked);

        if (hour_picked > 12) {
            hour_value = String.valueOf(hour_picked - 12);
        }

        if (minutes_picked < 10) {
            minute_value = "0" + String.valueOf(minutes_picked);
        }

        Log.d("Time is Set to:",  hour_value + ":" + minute_value);
        String alarmSetTime = hour_value + ":" + minute_value;
        Log.d("Alarm set",calendar.getTimeInMillis()+" ");
        Log.d("Alarm set at", System.currentTimeMillis() + " ");

        Toast.makeText(getApplicationContext(), "Alarm set at "+hour_value +":"+minute_value, Toast.LENGTH_LONG).show();

        databaseHelper = new DatabaseSqlHelper(getApplicationContext());
        DeviceBean deviceBean = databaseHelper.getDeviceDetails(deviceIpAddress);

        final Switch alarm_set_status = (Switch) findViewById(R.id.id_alarmSwitch);
        boolean switchStatus = alarm_set_status.getShowText();
        String switchStatusTxt = "OFF";
        if (switchStatus) {
            switchStatusTxt = "ON";
        }
        Log.d("Alarm SWITCH STATUS",switchStatusTxt + " ");

        AlarmManager service = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(DeviceAlarmActivity.this, AlarmReceiver.class);
        i.putExtra("DEVICEIP", deviceBean.getDeviceIpAddress());
        i.putExtra("SWITCHSTATUS", switchStatusTxt);
        i.putExtra("ALARMTIME",alarmSetTime);
        PendingIntent pending = PendingIntent.getBroadcast(DeviceAlarmActivity.this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        databaseHelper.setAlarmForDevice(alarmSetTime,deviceBean.getDeviceName(),deviceBean.getDeviceIpAddress(),switchStatusTxt, "repeat");

        service.set(AlarmManager.RTC, calendar.getTimeInMillis() , pending);
        //setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 1000 * 20, pending);
    }


}
