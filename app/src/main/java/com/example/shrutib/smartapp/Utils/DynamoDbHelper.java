package com.example.shrutib.smartapp.Utils;

import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.util.ImmutableMapParameter;
import com.example.shrutib.smartapp.BeanObjects.DeviceBean;
import com.example.shrutib.smartapp.BeanObjects.UserBean;
import com.example.shrutib.smartapp.BeanObjects.UserDeviceBean;
import com.example.shrutib.smartapp.RegistrationDetailsActivity;
import com.example.shrutib.smartapp.SmartDevice.ConfigureSmartDevice;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by shrutib on 4/30/17.
 */

public class DynamoDbHelper {

    private static final String TAG = "DynamoDbHelper";

    public static boolean insertUsers(UserBean userDetails) {
        AmazonDynamoDBClient ddb = RegistrationDetailsActivity.clientManager
                .ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression();
        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put("USERNAME",
                new ExpectedAttributeValue().withExists(false));

        saveExpression.setExpected(expected);

        boolean result = true;

        try {

            Log.d(TAG, "Inserting users");
            mapper.save(userDetails, saveExpression);
            Log.d(TAG, "Users are inserted");

        } catch (ConditionalCheckFailedException e) {
            Log.e(TAG, "ConditionalCheckFailedException in inserting users");
            result = false;
        } catch (AmazonServiceException ex) {
            Log.e(TAG, "Error in inserting users");
            RegistrationDetailsActivity.clientManager
                    .wipeCredentialsOnAuthError(ex);
            result = false;
        }
        return result;
    }

    public static boolean insertUserDeviceInfo(UserBean userDetails, DeviceBean deviceInfo) {
        AmazonDynamoDBClient ddb = ConfigureSmartDevice.clientManager
                .ddb();
        DynamoDBMapper mapper = new DynamoDBMapper(ddb);

        boolean result = true;

        try {

            UserDeviceBean userDeviceBean = new UserDeviceBean();
            userDeviceBean.setUserName(userDetails.getUserName()+"$#$"+deviceInfo.getDeviceIpAddress());
            userDeviceBean.setDeviceName(deviceInfo.getDeviceName());
            userDeviceBean.setDeviceStatus(deviceInfo.getDeviceStatus());
            userDeviceBean.setVendor(deviceInfo.getVendor());
            userDeviceBean.setDeviceIpAddress(deviceInfo.getDeviceIpAddress());

            Log.d(TAG, "Inserting user & device data");
            mapper.save(userDeviceBean);
            Log.d(TAG, "User & device inserted");

        } catch (AmazonServiceException ex) {
            Log.e(TAG, "Error inserting users");
            ConfigureSmartDevice.clientManager
                    .wipeCredentialsOnAuthError(ex);
            result = false;
        }
        return result;
    }
}
