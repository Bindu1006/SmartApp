package com.example.shrutib.smartapp.Utils;

import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.example.shrutib.smartapp.BeanObjects.UserBean;
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
            Log.d(TAG, "Users inserted");

        } catch (AmazonServiceException ex) {
            Log.e(TAG, "Error inserting users");
            RegistrationDetailsActivity.clientManager
                    .wipeCredentialsOnAuthError(ex);
        }
    }
}
