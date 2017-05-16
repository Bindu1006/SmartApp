package com.spectrum.smartapp.NetworkDiscovery;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;

import com.spectrum.smartapp.ConfigureDeviceActivity;
import com.spectrum.smartapp.R;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;
import com.spectrum.smartapp.Utils.Db;
import com.spectrum.smartapp.BeanObjects.DeviceBean;
import com.spectrum.smartapp.Utils.Prefs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by shrutib on 4/29/17.
 */

public abstract class AbstractDiscovery extends AsyncTask<Void, DeviceBean, Void> {

    //private final String TAG = "AbstractDiscovery";

    protected int hosts_done = 0;
    final protected WeakReference<ConfigureDeviceActivity> mDiscover;

    protected long ip;
    protected long start = 0;
    protected long end = 0;
    protected long size = 0;

    public AbstractDiscovery(ConfigureDeviceActivity discover) {
        mDiscover = new WeakReference<ConfigureDeviceActivity>(discover);
    }

    public void setNetwork(long ip, long start, long end) {
        this.ip = ip;
        this.start = start;
        this.end = end;
    }

    abstract protected Void doInBackground(Void... params);

    @Override
    protected void onPreExecute() {
        size = (int) (end - start + 1);
        if (mDiscover != null) {
            final ConfigureDeviceActivity discover = mDiscover.get();
            if (discover != null) {
                discover.setProgress(0);
            }
        }
    }

    @Override
    protected void onProgressUpdate(DeviceBean... host) {
        if (mDiscover != null) {
            final ConfigureDeviceActivity discover = mDiscover.get();
            if (discover != null) {
                ArrayList<String> ipList = discover.getAllDevices();
                if (!isCancelled()) {
                    if (host[0] != null) {

                        String[] mac = host[0].hardwareAddress.split(":");
                        String SELECT = "SELECT vendor FROM 'oui' where mac=?";

                        String name = null;
                        Cursor c = null;
                        try {
                            SQLiteDatabase data = Db.openDb(Db.DB_OUI);
                            c = data.rawQuery(SELECT, new String[] { (mac[0]+mac[1]+mac[2]).toUpperCase() });
                            if (c.moveToFirst()) {
                                name = c.getString(0);
                                Log.d("Shruti NAME Discover: ", c.getString(0));


//                                if (!ipList.contains(host[0].ipAddress)) {
                                    if (name.toLowerCase().contains("belkin") || name.toLowerCase().contains("xerox")) {
                                        discover.addHost(host[0], name);
                                    }
//                                }

                            }
                        } catch (SQLiteException e) {
                            Log.e(ConfigureDeviceActivity.TAG, e.getMessage());
                        } catch (IllegalStateException e) {
                            Log.e(ConfigureDeviceActivity.TAG, e.getMessage());
                        } finally {
                            if (c != null) {
                                c.close();
                            }
                        }

                    }
                    if (size > 0) {
                        discover.setProgress((int) (hosts_done * 10000 / size));
                    }
                }

            }
        }
    }

    @Override
    protected void onPostExecute(Void unused) {
        if (mDiscover != null) {
            final ConfigureDeviceActivity discover = mDiscover.get();
            if (discover != null) {
                if (discover.prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH,
                        Prefs.DEFAULT_VIBRATE_FINISH) == true) {
                    Vibrator v = (Vibrator) discover.getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(ConfigureDeviceActivity.VIBRATE);
                }
                discover.makeToast(R.string.discover_finished);
                discover.stopDiscovering();
            }
        }
    }

    @Override
    protected void onCancelled() {
        if (mDiscover != null) {
            final ConfigureDeviceActivity discover = mDiscover.get();
            if (discover != null) {
                discover.makeToast(R.string.discover_canceled);
                discover.stopDiscovering();
            }
        }
        super.onCancelled();
    }
}
