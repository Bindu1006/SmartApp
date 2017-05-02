package com.example.shrutib.smartapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shrutib.smartapp.NetworkDiscovery.AbstractDiscovery;
import com.example.shrutib.smartapp.NetworkDiscovery.ActivityNet;
import com.example.shrutib.smartapp.NetworkDiscovery.DefaultDiscovery;
import com.example.shrutib.smartapp.NetworkDiscovery.NetInfo;
import com.example.shrutib.smartapp.SmartDevice.ConfigureSmartDevice;
import com.example.shrutib.smartapp.BeanObjects.DeviceBean;
import com.example.shrutib.smartapp.Utils.Prefs;


import java.util.ArrayList;
import java.util.List;

public class ConfigureDeviceActivity extends ActivityNet implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = "ConfigureDeviceActivity";
    public static final String PKG = "com.example.shrutib.smartapp";

    public final static long VIBRATE = (long) 250;
    public final static int SCAN_PORT_RESULT = 1;
    private static LayoutInflater mInflater;
    private int currentNetwork = 0;
    private long network_ip = 0;
    private long network_start = 0;
    private long network_end = 0;
    private List<DeviceBean> hosts = null;
    private HostsAdapter adapter;
    private Button btn_discover;
    private AbstractDiscovery mDiscoveryTask = null;

    // private SlidingDrawer mDrawer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_device);
        mInflater = LayoutInflater.from(ctxt);

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
        btn_discover = (Button) findViewById(R.id.configureDevice);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("here","here");
                startDiscovering();
            }
        });

        // Hosts list
        adapter = new HostsAdapter(ctxt);
        ListView list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View view, int i, long l) {
                String ipAddress =((TextView)view.findViewById(R.id.mac)).getText().toString();
                String vendorName =((TextView)view.findViewById(R.id.vendor)).getText().toString();

                setSmartDevice(ipAddress, vendorName);


                Toast.makeText(ConfigureDeviceActivity.this, "myPos " + i + " " + ipAddress, Toast.LENGTH_LONG).show();
            }
        });
        list.setEmptyView(findViewById(R.id.list_empty));

    }

    private void setSmartDevice(String ipAddress, String vendorInfo) {
        Intent intent = new Intent(getBaseContext(), ConfigureSmartDevice.class);
        intent.putExtra("VENDOR_INFO", vendorInfo);
        intent.putExtra("IP_ADDRESS", ipAddress);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
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

    protected void setInfo() {
        // Info
//        ((TextView) findViewById(R.id.info_ip)).setText(info_ip_str);
//        ((TextView) findViewById(R.id.info_in)).setText(info_in_str);
//        ((TextView) findViewById(R.id.info_mo)).setText(info_mo_str);

        // Scan button state
        if (mDiscoveryTask != null) {
            setButton(btn_discover, R.drawable.cancel, false);
            btn_discover.setText(R.string.btn_discover_cancel);
            btn_discover.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    cancelTasks();
                }
            });
        }

        if (currentNetwork != net.hashCode()) {
            Log.i(TAG, "Network info has changed");
            currentNetwork = net.hashCode();

            // Cancel running tasks
            cancelTasks();
        } else {
            return;
        }

        // Get ip information
        network_ip = NetInfo.getUnsignedLongFromIp(net.ip);
        if (prefs.getBoolean(Prefs.KEY_IP_CUSTOM, Prefs.DEFAULT_IP_CUSTOM)) {
            // Custom IP
            network_start = NetInfo.getUnsignedLongFromIp(prefs.getString(Prefs.KEY_IP_START,
                    Prefs.DEFAULT_IP_START));
            network_end = NetInfo.getUnsignedLongFromIp(prefs.getString(Prefs.KEY_IP_END,
                    Prefs.DEFAULT_IP_END));
        } else {
            // Custom CIDR
            if (prefs.getBoolean(Prefs.KEY_CIDR_CUSTOM, Prefs.DEFAULT_CIDR_CUSTOM)) {
                net.cidr = Integer.parseInt(prefs.getString(Prefs.KEY_CIDR, Prefs.DEFAULT_CIDR));
            }
            // Detected IP
            int shift = (32 - net.cidr);
            if (net.cidr < 31) {
                network_start = (network_ip >> shift << shift) + 1;
                network_end = (network_start | ((1 << shift) - 1)) - 1;
            } else {
                network_start = (network_ip >> shift << shift);
                network_end = (network_start | ((1 << shift) - 1));
            }
            // Reset ip start-end (is it really convenient ?)
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(Prefs.KEY_IP_START, NetInfo.getIpFromLongUnsigned(network_start));
            edit.putString(Prefs.KEY_IP_END, NetInfo.getIpFromLongUnsigned(network_end));
            edit.commit();
        }
    }

    protected void setButtons(boolean disable) {
        if (disable) {
            setButtonOff(btn_discover, R.drawable.disabled);
        } else {
            setButtonOn(btn_discover, R.drawable.discover);
        }
    }

    protected void cancelTasks() {
        if (mDiscoveryTask != null) {
            mDiscoveryTask.cancel(true);
            mDiscoveryTask = null;
        }
    }

    // Listen for Activity results
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SCAN_PORT_RESULT:
                if (resultCode == RESULT_OK) {
                    // Get scanned ports
                    if (data != null && data.hasExtra(DeviceBean.EXTRA)) {
                        DeviceBean host = data.getParcelableExtra(DeviceBean.EXTRA);
                        if (host != null) {
                            hosts.set(host.position, host);
                        }
                    }
                }
            default:
                break;
        }
    }

    static class ViewHolder {
        TextView host;
        TextView mac;
        TextView vendor;
        ImageView logo;
    }

    // Custom ArrayAdapter
    private class HostsAdapter extends ArrayAdapter<Void> {
        public HostsAdapter(Context ctxt) {
            super(ctxt, R.layout.list_device_discovery, R.id.list);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_device_discovery, null);
                holder = new ViewHolder();
                holder.host = (TextView) convertView.findViewById(R.id.list);
                holder.mac = (TextView) convertView.findViewById(R.id.mac);
                holder.vendor = (TextView) convertView.findViewById(R.id.vendor);
                holder.logo = (ImageView) convertView.findViewById(R.id.logo);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final DeviceBean host = hosts.get(position);

            holder.logo.setImageResource(R.drawable.wemo_logo);
            holder.mac.setText(host.ipAddress);
            holder.vendor.setText(host.nicVendor);

            return convertView;
        }
    }

    /**
     * Discover hosts
     */
    private void startDiscovering() {

        Log.d("Shruti","Start discovery");
        mDiscoveryTask = new DefaultDiscovery(ConfigureDeviceActivity.this);

        mDiscoveryTask.setNetwork(network_ip, network_start, network_end);
        mDiscoveryTask.execute();
        btn_discover.setText(R.string.btn_discover_cancel);
        setButton(btn_discover, R.drawable.cancel, false);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cancelTasks();
            }
        });
        makeToast(R.string.start_discovery);
        initList();
    }

    public void stopDiscovering() {
        Log.e(TAG, "stopDiscovering()");
        mDiscoveryTask = null;
        setButtonOn(btn_discover, R.drawable.discover);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDiscovering();
            }
        });
        btn_discover.setText(R.string.discover);
    }

    private void initList() {
        // setSelectedHosts(false);
        adapter.clear();
        hosts = new ArrayList<DeviceBean>();
    }

    public void addHost(DeviceBean host, String vendor) {
        host.position = hosts.size();
        host.nicVendor = vendor;
        hosts.add(host);
        adapter.add(null);
    }

    public void makeToast(int msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void setButton(Button btn, int res, boolean disable) {
        if (disable) {
            setButtonOff(btn, res);
        } else {
            setButtonOn(btn, res);
        }
    }

    private void setButtonOff(Button b, int drawable) {
        b.setClickable(false);
        b.setEnabled(false);
        b.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
    }

    private void setButtonOn(Button b, int drawable) {
        b.setClickable(true);
        b.setEnabled(true);
        b.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
    }
}
