package com.spectrum.smartapp.BeanObjects;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;

/**
 * Created by shrutib on 5/10/17.
 */

@DynamoDBTable(tableName = DatabaseSqlHelper.WIKITUDE_TARGET_COLLECTION_TABLE_NAME)
public class TargetCollectionDetails {

    private String username;

    private String collectionName;

    private String collectionID;

    @DynamoDBHashKey(attributeName = "USERNAME")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @DynamoDBAttribute(attributeName = "TARGET_COLLECTION_NAME")
    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    @DynamoDBAttribute(attributeName = "TARGET_COLLECTION_ID")
    public String getCollectionID() {
        return collectionID;
    }

    public void setCollectionID(String collectionID) {
        this.collectionID = collectionID;
    }

    @Override
    public String toString() {
        return "TargetCollectionDetails{" +
                "username='" + username + '\'' +
                ", collectionName='" + collectionName + '\'' +
                ", collectionID='" + collectionID + '\'' +
                '}';
    }
}
