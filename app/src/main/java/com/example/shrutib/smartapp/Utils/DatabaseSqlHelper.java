package com.example.shrutib.smartapp.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.example.shrutib.smartapp.BeanObjects.DeviceBean;
import com.example.shrutib.smartapp.BeanObjects.UserBean;

import java.util.ArrayList;

/**
 * Created by shrutib on 4/30/17.
 */

public class DatabaseSqlHelper {

    public static final String msg = "DATA CONTROLLER ::: ";
    public static final String USER_TABLE_NAME = "USER_DETAILS";
    public static final String WEMO_TABLE_NAME = "WEMO_DETAILS";
    public static final String ALARM_TABLE_NAME = "ALARM_DETAILS";
    public static final String KEYS_TABLE_NAME = "PUBNUB_KEYS_DETAILS";
    public static final String VIDEO_TABLE_NAME = "VIDEO_STATUS_DETAILS";

    public static final String USER_CREATE_QUERY = "create table USER_DETAILS (USERNAME text PRIMARY KEY, PASSWORD text not null, PHONENUMBER text, EMAIL text not null, ADDRESS text);";

//    public static final String VIDEO_CREATE_QUERY = "create table VIDEO_STATUS_DETAILS (VIDEO_ID text PRIMARY KEY, VIDEO_STATUS text not null, PHONE_NUMBER text, MESSAGE_SENT text);";
//
//    public static final String KEYS_CREATE_QUERY = "create table PUBNUB_KEYS_DETAILS (PUBLISH_KEYS text PRIMARY KEY, SUBSCRIBE_KEYS text not null);";
//
//    public static final String WEMO_CREATE_QUERY = "create table WEMO_DETAILS (DEVICE_IP text PRIMARY KEY, DEVICE_NAME text not null,DEVICE_STATUS text not null );";
//
//    public static final String ALARM_CREATE_QUERY = "create table ALARM_DETAILS (_id text PRIMARY KEY, DEVICE_NAME text not null,DEVICE_IP text not null, DEVICE_STATUS text not null, ALARM_TIME text not null);";

    public static final String DATABASE_NAME = "PIHOME.db";
    public static final int DATABASE_VERSION = 4;

    public static final String WEMO_DROP_QUERY = "DROP TABLE IF EXISTS WEMO_DETAILS";
    public static final String LED_DROP_QUERY = "DROP TABLE IF EXISTS LED_DETAILS";
    public static final String KEYS_DROP_QUERY = "DROP TABLE IF EXISTS PUBNUB_KEYS_DETAILS";
    public static final String VIDEO_DROP_QUERY = "DROP TABLE IF EXISTS VIDEO_STATUS_DETAILS";

    Context context;
    DataBaseHelper databaseHelper;
    SQLiteDatabase database;
    private UserBean userInfo = null;

    private static class DataBaseHelper extends SQLiteOpenHelper {

        public DataBaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            try {
                db.execSQL(USER_CREATE_QUERY);
//                db.execSQL(WEMO_CREATE_QUERY);
//                db.execSQL(ALARM_CREATE_QUERY);
//                db.execSQL(KEYS_CREATE_QUERY);
//                db.execSQL(VIDEO_CREATE_QUERY);
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
            db.execSQL(USER_CREATE_QUERY);
//            db.execSQL(WEMO_CREATE_QUERY);
//            db.execSQL(ALARM_CREATE_QUERY);
//            db.execSQL(KEYS_CREATE_QUERY);
//            db.execSQL(VIDEO_CREATE_QUERY);
            onCreate(db);
        }

    }

    public DatabaseSqlHelper(Context context) {
        this.context = context;
        databaseHelper = new DataBaseHelper(context);
    }

    public DatabaseSqlHelper open() {
        database = databaseHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        databaseHelper.close();
    }

    public void deleteDatabase() {

        Log.d("Delete User Details : ", "Database");
        database = databaseHelper.getWritableDatabase();
        database.delete(USER_TABLE_NAME, null, null);
        database.close();

    }

    public void registerUser(UserBean userDetails) {

        Log.d("Insert User Details : ", userDetails.toString());
        database = databaseHelper.getWritableDatabase();

        ContentValues userValues = new ContentValues();
        userValues.put("USERNAME", userDetails.getUserName());
        userValues.put("PASSWORD", userDetails.getPassword());
        userValues.put("PHONENUMBER", userDetails.getPhoneNumber());
        userValues.put("EMAIL", userDetails.getEmail());
        userValues.put("ADDRESS", userDetails.getAddress());

        database.insertOrThrow(USER_TABLE_NAME, null, userValues);
        database.close();

        userInfo = userDetails;
        new InsertUserTask().execute();
//        TODO Insert in Dynamo DB also

    }

    public UserBean retrieveUserDetails(UserBean userDetails) {

        Log.d("Retrieve User Data : ", userDetails.toString());

        database = databaseHelper.getWritableDatabase();
        String selectQuery = "SELECT * FROM " + USER_TABLE_NAME + " where USERNAME = \""+userDetails.getUserName() +"\"";
        Cursor cursor      = database.rawQuery(selectQuery, null);
        UserBean retrievedUserDetails = null;
//        ArrayList<IPAddressDetails> deviceDetailsList = new ArrayList<IPAddressDetails>();

        if (cursor.moveToFirst()) {
            do {
                retrievedUserDetails = new UserBean(cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3), cursor.getString(4));
            } while (cursor.moveToNext());
        }
        return retrievedUserDetails;

    }



    public void insertSmartDeviceData(DeviceBean deviceDetails) {

        Log.d("Insert Smart Device : ", "Device Details");
//        database = databaseHelper.getWritableDatabase();
//        if (deviceDetails.getDeviceStatus().equalsIgnoreCase("ON")) {
//            deviceDetails.setDeviceStatus("DEVICE_ON");
//        } else if (deviceDetails.getDeviceStatus().equalsIgnoreCase("OFF")) {
//            deviceDetails.setDeviceStatus("DEVICE_OFF");
//        }
//
//        ContentValues deviceValues = new ContentValues();
//        deviceValues.put("DEVICE_NAME", deviceDetails.getDeviceName());
//        deviceValues.put("DEVICE_IP", deviceDetails.getDeviceIpAddress());
//        deviceValues.put("DEVICE_STATUS", deviceDetails.getDeviceStatus());
//
//        database.insertOrThrow(WEMO_TABLE_NAME, null, deviceValues);
//        database.close();

    }

    public ArrayList<DeviceBean> getAllDeviceDetails(){

        Log.d("Retrieve Device Data : ", "Device Details");
//        database = databaseHelper.getWritableDatabase();
//        String selectQuery = "SELECT * FROM " + WEMO_TABLE_NAME;
//        Cursor cursor      = database.rawQuery(selectQuery, null);
//        ArrayList<IPAddressDetails> deviceDetailsList = new ArrayList<IPAddressDetails>();
//        String[] data      = null;
//
//
//        if (cursor.moveToFirst()) {
//            do {
//                IPAddressDetails deviceDetails = new IPAddressDetails();
//                deviceDetails.setDeviceName(cursor.getString(1));
//                deviceDetails.setDeviceIpAddress(cursor.getString(0));
//                deviceDetails.setDeviceStatus(cursor.getString(2));
//                deviceDetailsList.add(deviceDetails);
//
//            } while (cursor.moveToNext());
//        }
//        Log.d("DATA : ", "LIST :" + deviceDetailsList);
//
//        cursor.close();
//        database.close();
//        return deviceDetailsList;
        return null;

    }

    public void updateStatus(String ipAddr, String switchStatus){

        Log.d("SWITCH","Update Status");

//        database = databaseHelper.getWritableDatabase();
//        if (switchStatus.equalsIgnoreCase("ON")) {
//            switchStatus = "DEVICE_ON";
//        } else if (switchStatus.equalsIgnoreCase("OFF")) {
//            switchStatus = "DEVICE_OFF";
//        }
//
//        ContentValues values=new ContentValues();
//        values.put("DEVICE_STATUS", switchStatus);
//        Log.d(ipAddr,switchStatus);
//
//        // String Update = " UPDATE " +WEMO_TABLE_NAME+ " set DEVICE_STATUS = "+switchStatus+ " where DEVICE_IP = "+ipAddr;
////        database.execSQL(Update);
//        int rowsUpdated = database.update(WEMO_TABLE_NAME, values, " DEVICE_IP = \"" + ipAddr +"\"", null);
//        Log.d("UPDATED", " " + rowsUpdated);
//        database.close();

    }


    public void setAlarmForDevice(String alarmSetTime,String deviceName,String deviceIP, String switchStatus){
        Log.d("DATABASE",deviceName+" "+deviceIP);

        database = databaseHelper.getWritableDatabase();
        if (switchStatus.equalsIgnoreCase("ON")) {
            switchStatus = "DEVICE_ON";
        } else if (switchStatus.equalsIgnoreCase("OFF")) {
            switchStatus = "DEVICE_OFF";
        }

        ContentValues alarmDevice = new ContentValues();
        alarmDevice.put("DEVICE_NAME",deviceName);
        alarmDevice.put("DEVICE_IP", deviceIP);
        alarmDevice.put("DEVICE_STATUS",switchStatus);
        alarmDevice.put("ALARM_TIME",alarmSetTime);

        database.insertOrThrow(ALARM_TABLE_NAME, null, alarmDevice);
        database.close();
    }

//    public void deleteAlarmEntry(DeviceSchedulerBO schedulerDetails) {
//        Log.d("Delete Alarm Entry", schedulerDetails + " ");
//
//        database = databaseHelper.getWritableDatabase();
//        database.delete(ALARM_TABLE_NAME, " DEVICE_IP = \"" + schedulerDetails.getDeviceIP() + "\" and ALARM_TIME = \"" + schedulerDetails.getAlarmTime() + "\"", null);
//        database.close();
//
//    }

//    public ArrayList<DeviceSchedulerBO> viewSchedule(){
//        Log.d("Database "," View all Alarm Entries ");
//
//        database = databaseHelper.getWritableDatabase();
//        String selectQuery = "SELECT * FROM " + ALARM_TABLE_NAME;
//        Cursor cursor      = database.rawQuery(selectQuery, null);
//        ArrayList<DeviceSchedulerBO> deviceSchedulerList = new ArrayList<>();
//        String[] data      = null;
//
//
//        if (cursor.moveToFirst()) {
//            do {
//                //Setting device Scheduler details fetched from the database and adding to the list
//                DeviceSchedulerBO devideSchedulerDetails = new DeviceSchedulerBO();
//                devideSchedulerDetails.setDeviceName(cursor.getString(1));
//                devideSchedulerDetails.setDeviceIP(cursor.getString(2));
//                devideSchedulerDetails.setDeviceStatus(cursor.getString(3));
//                devideSchedulerDetails.setAlarmTime(cursor.getString(4));
//                deviceSchedulerList.add(devideSchedulerDetails);
//
//            } while (cursor.moveToNext());
//        }
//        Log.d("DATA : ", "LIST :" + deviceSchedulerList);
//
//        cursor.close();
//        database.close();
//        return deviceSchedulerList;
//    }

    public String getVideoStatus(){
        Log.d("Database "," Get Video Status Entries ");

        database = databaseHelper.getWritableDatabase();
        String retrieveQuery = "SELECT VIDEO_STATUS FROM " + VIDEO_TABLE_NAME;
        Cursor cursor      = database.rawQuery(retrieveQuery, null);
        String status = "";

        if (cursor.moveToFirst()) {
            do {
                status = cursor.getString(0);
            } while (cursor.moveToNext());
        }
        Log.d("from Main",status);
        cursor.close();
        database.close();
        return status;
    }

    public boolean setDefaultVideoStatus(){
        boolean result = false;
        //Check if any data already exists

        String countQuery = "SELECT  * FROM " + VIDEO_TABLE_NAME;
        database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(countQuery, null);
        int cnt = cursor.getCount();
        cursor.close();

        if(cnt == 1){
            result = false;
        } else {
            ContentValues videoValues = new ContentValues();
            videoValues.put("VIDEO_ID", "01");
            videoValues.put("VIDEO_STATUS", "VIDEO_OFF");

            database.insertOrThrow(VIDEO_TABLE_NAME, null, videoValues);
            result = true;
        }
        database.close();
        return result;

    }

    public boolean setVideoStatus(String videoStatus){
        boolean result = false;
        //Check if any data already exists

        if(videoStatus.equalsIgnoreCase("ON")){
            videoStatus = "VIDEO_ON";
        } else if (videoStatus.equalsIgnoreCase("OFF")){
            videoStatus = "VIDEO_OFF";
        }

        database = databaseHelper.getWritableDatabase();
        String retrieveQuery = "SELECT VIDEO_ID FROM " + VIDEO_TABLE_NAME;
        Cursor cursor      = database.rawQuery(retrieveQuery, null);
        String vid = "";

        if (cursor.moveToFirst()) {
            do {
                vid = cursor.getString(0);
            } while (cursor.moveToNext());
        }

        if(vid != null || !vid.equalsIgnoreCase("")){
            ContentValues videoValues = new ContentValues();
            videoValues.put("VIDEO_STATUS", videoStatus);
            Log.d("video status",videoStatus);

            String Update = " UPDATE " +VIDEO_TABLE_NAME+ " set VIDEO_STATUS = \""+videoStatus+"\" where VIDEO_ID = \""+vid+"\"";
            database.execSQL(Update);
        } else {
            ContentValues videoValues = new ContentValues();
            videoValues.put("VIDEO_ID", "01");
            videoValues.put("VIDEO_STATUS", videoStatus);

            database.insertOrThrow(VIDEO_TABLE_NAME, null, videoValues);
            result = true;
        }
        database.close();
        cursor.close();
        return result;

    }

    public boolean savePhoneNumber(String phoneNumber){
        boolean result = false;
        //Check if any data already exists

        database = databaseHelper.getWritableDatabase();
        String retrieveQuery = "SELECT VIDEO_ID FROM " + VIDEO_TABLE_NAME;
        Cursor cursor      = database.rawQuery(retrieveQuery, null);
        String vid = "";

        if (cursor.moveToFirst()) {
            do {
                vid = cursor.getString(0);
            } while (cursor.moveToNext());
        }

        if(vid != null || !vid.equalsIgnoreCase("")){
            ContentValues videoValues = new ContentValues();
            videoValues.put("PHONE_NUMBER", phoneNumber);
            Log.d("Set phone ",phoneNumber);

            String Update = " UPDATE " +VIDEO_TABLE_NAME+ " set PHONE_NUMBER = \""+phoneNumber+"\" where VIDEO_ID = \""+vid+"\"";

            database.execSQL(Update);
            result = true;
        } else {
            result = false;
        }
        cursor.close();
        database.close();
        return result;

    }

    public String getPhoneNumber(){
        Log.d("DATABASE","get Phone Number");

        database = databaseHelper.getWritableDatabase();
        String retrieveQuery = "SELECT PHONE_NUMBER FROM " + VIDEO_TABLE_NAME;
        Cursor cursor      = database.rawQuery(retrieveQuery, null);
        String phoneNumber = "";

        if (cursor.moveToFirst()) {
            do {
                phoneNumber = cursor.getString(0);
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return phoneNumber;

    }

    public  boolean setMessageSentTime(long time){
        Log.d("DATABASE","set time for send msg");

        boolean result = false;

        //Check if any data already exists
        String retriveQuery = "SELECT MESSAGE_SENT FROM " + VIDEO_TABLE_NAME;
        database = databaseHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery(retriveQuery, null);
        int cnt = cursor.getCount();

        String msg_time = "";

        if(cnt == 1){
            if (cursor.moveToFirst()) {
                do {
                    msg_time = cursor.getString(0);
                } while (cursor.moveToNext());
            }
            if (msg_time != null){
                long storedTime = Long.parseLong(msg_time);
                if (System.currentTimeMillis() > storedTime + 1000*60*30){
                    result = true;
                }

            }
            else {

                ContentValues videoValues = new ContentValues();
                videoValues.put("MESSAGE_SENT", time);

                int rowsUpdated = database.update(VIDEO_TABLE_NAME, videoValues, null, null);
                // database.insertOrThrow(VIDEO_TABLE_NAME, null, videoValues);
                result = true;

            }

//            if (System.currentTimeMillis() > storedTime + 1000*60*30);
//            result = true;
        } else if(cnt == 0){

            ContentValues videoValues = new ContentValues();
            videoValues.put("MESSAGE_SENT", time);

            int rowsUpdated = database.update(VIDEO_TABLE_NAME, videoValues, null, null);
            // database.insertOrThrow(VIDEO_TABLE_NAME, null, videoValues);
            result = true;
        }
        cursor.close();
        database.close();
        return result;


    }

    private class InsertUserTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... voids) {

            DynamoDbHelper.insertUsers(userInfo);

            return null;
        }
    }
}
