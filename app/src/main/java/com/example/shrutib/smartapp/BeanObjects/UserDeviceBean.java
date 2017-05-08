package com.example.shrutib.smartapp.BeanObjects;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.example.shrutib.smartapp.Utils.DatabaseSqlHelper;

import java.io.Serializable;

/**
 * Created by shrutib on 5/6/17.
 */

@DynamoDBTable(tableName = DatabaseSqlHelper.USER_DEVICE_TABLE_NAME)
public class UserDeviceBean implements Serializable{

    private String userName;

    private String deviceIpAddress;

    private String vendor;

    private String deviceName;

    private String deviceStatus;

    @DynamoDBHashKey(attributeName = "USERNAME")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @DynamoDBAttribute(attributeName = "DEVICE_IP")
    public String getDeviceIpAddress() {
        return deviceIpAddress;
    }

    public void setDeviceIpAddress(String deviceIpAddress) {
        this.deviceIpAddress = deviceIpAddress;
    }

    @DynamoDBAttribute(attributeName = "VENDOR")
    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    @DynamoDBAttribute(attributeName = "DEVICE_NAME")
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @DynamoDBAttribute(attributeName = "DEVICE_STATUS")
    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }
}
