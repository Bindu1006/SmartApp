package com.example.shrutib.smartapp.Utils;

import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.example.shrutib.smartapp.BeanObjects.DeviceBean;
import com.example.shrutib.smartapp.BeanObjects.UserBean;
import com.example.shrutib.smartapp.BeanObjects.UserDeviceBean;
import com.example.shrutib.smartapp.RegistrationDetailsActivity;

/**
 * Created by shrutib on 4/30/17.
 */

public class DynamoDbHelper {

    private static final String TAG = "DynamoDbHelper";

    public static void insertUsers(UserBean userDetails) {
        AmazonDynamoDBClient ddb = RegistrationDetailsActivity.clientManager
                .ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        try {

            Log.d(TAG, "Inserting users");
            mapper.save(userDetails);
            Log.d(TAG, "Users are inserted");

        } catch (AmazonServiceException ex) {
            Log.e(TAG, "Error in inserting users");
            RegistrationDetailsActivity.clientManager
                    .wipeCredentialsOnAuthError(ex);
        }
    }

    public static void insertUserDeviceInfo(UserBean userDetails, DeviceBean deviceInfo) {
        AmazonDynamoDBClient ddb = RegistrationDetailsActivity.clientManager
                .ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        try {

            UserDeviceBean userDeviceBean = new UserDeviceBean();
            userDeviceBean.setUserName(userDetails.getUserName());
            userDeviceBean.setDeviceName(deviceInfo.getDeviceName());
            userDeviceBean.setDeviceStatus(deviceInfo.getDeviceStatus());
            userDeviceBean.setVendor(deviceInfo.getVendor());
            userDeviceBean.setDeviceIpAddress(deviceInfo.getDeviceIpAddress());

            Log.d(TAG, "Inserting user & device data");
            mapper.save(userDeviceBean);
            Log.d(TAG, "User & device inserted");

        } catch (AmazonServiceException ex) {
            Log.e(TAG, "Error inserting users");
            RegistrationDetailsActivity.clientManager
                    .wipeCredentialsOnAuthError(ex);
        }
    }
}
