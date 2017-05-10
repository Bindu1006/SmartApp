package com.spectrum.smartapp.Utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

/**
 * Created by shrutib on 4/29/17.
 */

public class Db {

    private final static String TAG = "Db";
    private Context ctxt = null;

    // Databases information
    public static final String PATH = "/sdcard/myDB/";
    public static final String DB_SERVICES = "services.db";
    public static final String DB_PROBES = "probes.db";
    public static final String DB_NIC = "nic.db";
    public static final String DB_OUI = "oui.db";
    public static final String DB_SAVES = "saves.db";

    public Db(Context ctxt) {
        this.ctxt = ctxt;
        // new File(PATH).mkdirs();
    }

    public static SQLiteDatabase openDb(String db_name) {
        return openDb(db_name, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    }

    public static SQLiteDatabase openDb(String db_name, int flags) {
        try {
            return SQLiteDatabase.openDatabase(PATH + db_name, null, flags);
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

}
