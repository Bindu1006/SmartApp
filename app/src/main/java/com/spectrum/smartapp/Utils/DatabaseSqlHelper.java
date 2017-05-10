package com.spectrum.smartapp.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.spectrum.smartapp.BeanObjects.DeviceAlarmDetails;
import com.spectrum.smartapp.BeanObjects.DeviceBean;
import com.spectrum.smartapp.BeanObjects.TargetCollectionDetails;
import com.spectrum.smartapp.BeanObjects.UserBean;
import com.spectrum.smartapp.LightsActivity;
import com.spectrum.smartapp.MainActivity;

import java.util.ArrayList;

/**
 * Created by shrutib on 4/30/17.
 */

public class DatabaseSqlHelper {

    public static final String msg = "DATA CONTROLLER ::: ";
    public static final String USER_TABLE_NAME = "USER_DETAILS";
    public static final String USER_DEVICE_TABLE_NAME = "USER_DEVICE_DETAILS";
    public static final String DEVICE_TABLE_NAME = "DEVICE_DETAILS";
    public static final String DEVICE_ALARM_TABLE_NAME = "DEVICE_ALARM_DETAILS";
    public static final String KEYS_TABLE_NAME = "PUBNUB_KEYS_DETAILS";
    public static final String VIDEO_TABLE_NAME = "VIDEO_STATUS_DETAILS";
    public static final String WIKITUDE_TARGET_COLLECTION_TABLE_NAME = "WIKITUDE_TARGET_COLLECTION";


    public static final String USER_CREATE_QUERY = "create table USER_DETAILS (USERNAME text PRIMARY KEY, PASSWORD text not null, PHONENUMBER text, EMAIL text not null, ADDRESS text, LOGIN_STATUS text);";

    public static final String WIKITUDE_TARGET_COLLECTION_CREATE_QUERY = "create table WIKITUDE_TARGET_COLLECTION (USERNAME text PRIMARY KEY, TARGET_COLLECTION_NAME text unique, TARGET_COLLECTION_ID text);";

//    public static final String VIDEO_CREATE_QUERY = "create table VIDEO_STATUS_DETAILS (VIDEO_ID text PRIMARY KEY, VIDEO_STATUS text not null, PHONE_NUMBER text, MESSAGE_SENT text);";
//
//    public static final String KEYS_CREATE_QUERY = "create table PUBNUB_KEYS_DETAILS (PUBLISH_KEYS text PRIMARY KEY, SUBSCRIBE_KEYS text not null);";

    public static final String DEVICE_CREATE_QUERY = "create table DEVICE_DETAILS (DEVICE_IP text PRIMARY KEY, DEVICE_NAME text not null,DEVICE_STATUS text not null, VENDOR_NAME text not null );";

    public static final String DEVICE_ALARM_CREATE_QUERY = "create table DEVICE_ALARM_DETAILS (_id text PRIMARY KEY, DEVICE_NAME text not null,DEVICE_IP text not null, DEVICE_STATUS text not null, ALARM_TIME text not null, REPEAT_STATUS text);";

    public static final String DATABASE_NAME = "PIHOME.db";
    public static final int DATABASE_VERSION = 4;

    public static final String DEVICE_DROP_QUERY = "DROP TABLE IF EXISTS DEVICE_DETAILS";

    Context context;
    DataBaseHelper databaseHelper;
    SQLiteDatabase database;
    private UserBean userInfo = null;
    private DeviceBean deviceInfo = null;

    private static class DataBaseHelper extends SQLiteOpenHelper {

        public DataBaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(USER_CREATE_QUERY);
                db.execSQL(DEVICE_CREATE_QUERY);
                db.execSQL(DEVICE_ALARM_CREATE_QUERY);
                db.execSQL(WIKITUDE_TARGET_COLLECTION_CREATE_QUERY);
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
            db.execSQL(DEVICE_CREATE_QUERY);
            db.execSQL(DEVICE_ALARM_CREATE_QUERY);
            db.execSQL(WIKITUDE_TARGET_COLLECTION_CREATE_QUERY);
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

//    public void deleteDatabase() {
//
//        Log.d("Delete User Details : ", "Database");
//        database = databaseHelper.getWritableDatabase();
//        database.delete(USER_TABLE_NAME, null, null);
//        database.close();
//
//    }

    public boolean registerUser(UserBean userDetails, boolean isStoreDynamoDB) {

        Log.d("Insert User Details : ", userDetails.toString());
        boolean result = true;
        database = databaseHelper.getWritableDatabase();

        ContentValues userValues = new ContentValues();
        userValues.put("USERNAME", userDetails.getUserName());
        userValues.put("PASSWORD", userDetails.getPassword());
        userValues.put("PHONENUMBER", userDetails.getPhoneNumber());
        userValues.put("EMAIL", userDetails.getEmail());
        userValues.put("ADDRESS", userDetails.getAddress());
        userValues.put("LOGIN_STATUS", "TRUE");

        try{
            database.insert(USER_TABLE_NAME, null, userValues);
        }catch(SQLiteConstraintException e){
            Log.e("Database Exception",   "This code doesn't show");
            result = false;
        } finally {
            database.close();
        }

        if (result && isStoreDynamoDB) {
            // Insert in Dynamo DB also
            userInfo = userDetails;
            new InsertUserTask().execute();
        }
        return result;

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
        database.close();
        return retrievedUserDetails;

    }

    public UserBean getUserDetails() {

        Log.d("DAtabase : ","get User Data ");

        database = databaseHelper.getWritableDatabase();
        String selectQuery = "SELECT * FROM " + USER_TABLE_NAME;
        Cursor cursor      = database.rawQuery(selectQuery, null);
        UserBean retrievedUserDetails = null;

        if (cursor.moveToFirst()) {
            do {
                retrievedUserDetails = new UserBean(cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3), cursor.getString(4));
            } while (cursor.moveToNext());
        }
        database.close();
        return retrievedUserDetails;

    }

    public int getUserCount() {

        Log.d("DAtabase : ","get User count ");

        database = databaseHelper.getWritableDatabase();
        String selectQuery = "SELECT count(*) FROM " + USER_TABLE_NAME;
        Cursor cursor      = database.rawQuery(selectQuery, null);
        int userCount = 0;

        if (cursor.moveToFirst()) {
            do {
                userCount = cursor.getInt(0);
            } while (cursor.moveToNext());
        }
        database.close();
        return userCount;

    }

    public boolean authenticateUser(String username, String password) {

        Log.d("Authenticate User : ", username);

        database = databaseHelper.getWritableDatabase();
        String authenticateQuery = "SELECT * FROM " + USER_TABLE_NAME + " where USERNAME = \""+username +"\" and PASSWORD = \""+password +"\"";
        Cursor cursor      = database.rawQuery(authenticateQuery, null);
        String retrievedUsername = null;
        String retrievedPassword = null;

        if (cursor.moveToFirst()) {
            do {
                retrievedUsername = cursor.getString(0);
                retrievedPassword = cursor.getString(1);
            } while (cursor.moveToNext());
        }

        Log.d("Shruti","Username: "+retrievedUsername);
        database.close();

        if (username.equals(retrievedUsername) && password.equals(retrievedPassword)) {
            return true;
        } else {
            return false;
        }

    }



    public boolean insertSmartDeviceData(DeviceBean deviceDetails) {

        Log.d("Insert Smart Device : ", " "+deviceDetails);
        boolean result = true;

        database = databaseHelper.getWritableDatabase();
        ContentValues deviceValues = new ContentValues();
        deviceValues.put("DEVICE_NAME", deviceDetails.getDeviceName());
        deviceValues.put("DEVICE_IP", deviceDetails.getDeviceIpAddress());
        deviceValues.put("DEVICE_STATUS", deviceDetails.getDeviceStatus());
        deviceValues.put("VENDOR_NAME", deviceDetails.getVendor());

        try{
            database.insert(DEVICE_TABLE_NAME, null, deviceValues);
        }catch(SQLiteConstraintException e){
            Log.e("Database Exception",   "This code doesn't show");
            result = false;
        } finally {
            database.close();
        }

        if (result) {
            insertDynamoSmartDeviceData(deviceDetails);
        }

        return result;

    }

    public void insertDynamoSmartDeviceData(DeviceBean deviceDetails) {

        Log.d("Insert Smart Device : ", "Dynamo DB");

        userInfo = getUserDetails();
        deviceInfo = deviceDetails;
        new InsertUserDeviceDetails().execute();

    }

    public void deleteSmartDeviceData(DeviceBean deviceDetails) {

        Log.d("Delete Smart Device : ", "Device Details");
        boolean result = true;

        database = databaseHelper.getWritableDatabase();

        try{
            database.delete(DEVICE_TABLE_NAME, "DEVICE_IP = ?",
                    new String[] { String.valueOf(deviceDetails.getDeviceIpAddress()) });
        }catch(SQLiteConstraintException e){
            Log.e("Database Exception",   "This code doesn't show");
            result = false;
        } finally {
            database.close();
        }

    }

    public void deleteUserData(UserBean userDetails) {

        Log.d("Delete User : ", "User : " +userDetails);

        database = databaseHelper.getWritableDatabase();

        try{
            database.delete(USER_TABLE_NAME, "USERNAME = ?",
                    new String[] { String.valueOf(userDetails.getUserName()) });
        }catch(SQLiteConstraintException e){
            Log.e("Database Exception",   "This code doesn't show");
        } finally {
            database.close();
        }
    }

    public String getLoginStatus(){

        Log.d("Get Login Status : ", "Device Details");
        database = databaseHelper.getWritableDatabase();
        String selectQuery = "SELECT LOGIN_STATUS FROM " + USER_TABLE_NAME;

        Cursor cursor      = database.rawQuery(selectQuery, null);
        String status = null;
        if (cursor.moveToFirst()) {
            do {
                status = cursor.getString(0);
            } while (cursor.moveToNext());
        }
        Log.d("Login Status : ", "Status :" + status);
        cursor.close();
        database.close();
        return status;

    }

    public void updateLoginStatus(String username, String status){

        Log.d("Get Login Status : ", " username : "+ username);
        database = databaseHelper.getWritableDatabase();

        ContentValues values=new ContentValues();
        values.put("LOGIN_STATUS", status);

        int rowsUpdated = database.update(USER_TABLE_NAME, values, " username = \"" + username +"\"", null);

        Log.d("UPDATED", " " + rowsUpdated);
        database.close();

    }

    public ArrayList<DeviceBean> getAllDeviceDetails(){

        Log.d("Retrieve Device Data : ", "Device Details");
        database = databaseHelper.getWritableDatabase();
        String countQuery = "SELECT * FROM " + DEVICE_TABLE_NAME;
        Cursor cursor      = database.rawQuery(countQuery, null);
        ArrayList<DeviceBean> deviceslist = new ArrayList<DeviceBean>();
        if (cursor.moveToFirst()) {
            do {
                DeviceBean device = new DeviceBean();
                device.setDeviceIpAddress(cursor.getString(0));
                device.setDeviceName(cursor.getString(1));
                device.setDeviceStatus(cursor.getString(2));
                device.setVendor(cursor.getString(3));
                Log.d("Shruti : ", "device :" + device);
                deviceslist.add(device);
            } while (cursor.moveToNext());
        }
        Log.d("Count data : ", "Count :" + deviceslist.size());
        cursor.close();
        database.close();
        return deviceslist;

    }

    public int getDeviceCount(){

        Log.d("Get Device Count : ", "Device Details");
        database = databaseHelper.getWritableDatabase();
        String countQuery = "SELECT COUNT(*) FROM " + DEVICE_TABLE_NAME;
        Cursor cursor      = database.rawQuery(countQuery, null);
        int result = 0;
        if (cursor.moveToFirst()) {
            do {
                result = cursor.getInt(0);
            } while (cursor.moveToNext());
        }
        Log.d("Count data : ", "Count :" + result);
        cursor.close();
        database.close();
        return result;

    }

    public String getDeviceStatus(String ipAddress){

        Log.d("Get Device Status : ", "Device Details");
        database = databaseHelper.getWritableDatabase();
        String countQuery = "SELECT DEVICE_STATUS FROM " + DEVICE_TABLE_NAME + " where DEVICE_IP  = \""+ ipAddress +"\"";

        Cursor cursor      = database.rawQuery(countQuery, null);
        String status = null;
        if (cursor.moveToFirst()) {
            do {
                status = cursor.getString(0);
            } while (cursor.moveToNext());
        }
        Log.d("Status data : ", "Status :" + status);
        cursor.close();
        database.close();
        return status;

    }

    public DeviceBean getDeviceDetails(String ipAddress){

        Log.d("Get Device Details: ", ipAddress);
        database = databaseHelper.getWritableDatabase();
        String countQuery = "SELECT * FROM " + DEVICE_TABLE_NAME + " where DEVICE_IP  = \""+ ipAddress +"\"";

        Cursor cursor      = database.rawQuery(countQuery, null);
        DeviceBean deviceBean = new DeviceBean();
        if (cursor.moveToFirst()) {
            do {
                deviceBean.setDeviceIpAddress(cursor.getString(0));
                deviceBean.setDeviceName(cursor.getString(1));
                deviceBean.setDeviceStatus(cursor.getString(2));
                deviceBean.setVendor(cursor.getString(3));
            } while (cursor.moveToNext());
        }
        Log.d("Device data : ", "Device :" + deviceBean);
        cursor.close();
        database.close();
        return deviceBean;

    }

    public void updateDeviceStatus(String ipAddress, String status){

        Log.d("Get Device Status : ", "Device Details");
        database = databaseHelper.getWritableDatabase();

        ContentValues values=new ContentValues();
        values.put("DEVICE_STATUS", status);

        int rowsUpdated = database.update(DEVICE_TABLE_NAME, values, " DEVICE_IP = \"" + ipAddress +"\"", null);

        Log.d("UPDATED", " " + rowsUpdated);
        database.close();

    }

    public void setAlarmForDevice(String alarmSetTime,String deviceName,String deviceIP, String switchStatus, String repeatStatus){
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

        database.insertOrThrow(DEVICE_ALARM_TABLE_NAME, null, alarmDevice);
        database.close();
    }

    public void deleteAlarmEntry(DeviceAlarmDetails schedulerDetails) {
        Log.d("Delete Alarm Entry", schedulerDetails + " ");

        database = databaseHelper.getWritableDatabase();
        database.delete(DEVICE_ALARM_TABLE_NAME, " DEVICE_IP = \"" + schedulerDetails.getDeviceIP() + "\" and ALARM_TIME = \"" + schedulerDetails.getAlarmTime() + "\"", null);
        database.close();

    }

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

    public TargetCollectionDetails getWikitudeTargetCollectionData(String username){
        Log.d("DATABASE","get target collection");

        database = databaseHelper.getWritableDatabase();
        String retrieveQuery = "SELECT * FROM " + WIKITUDE_TARGET_COLLECTION_TABLE_NAME;
        Cursor cursor      = database.rawQuery(retrieveQuery, null);
        TargetCollectionDetails collectionDetails = new TargetCollectionDetails();

        if (cursor.moveToFirst()) {
            do {
                collectionDetails.setCollectionID(cursor.getString(2));
                collectionDetails.setCollectionName(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return collectionDetails;

    }

    private class InsertUserTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            return DynamoDbHelper.insertUsers(userInfo);

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(context, "Successfully created user.",
                        Toast.LENGTH_LONG).show();
            } else {
                deleteUserData(userInfo);
                Toast.makeText(context, "Cannot add user at this time. Please try again later.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private class InsertUserDeviceDetails extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            boolean result = DynamoDbHelper.insertUserDeviceInfo(userInfo, deviceInfo);

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(context, "Successfully created device.",
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(context, LightsActivity.class);
                context.startActivity(intent);
            } else {
                deleteSmartDeviceData(deviceInfo);
                Toast.makeText(context, "Cannot add a device at this time. Please try again later.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
