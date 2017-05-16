package com.spectrum.smartapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.spectrum.smartapp.AugmentedReality.AugmentedMainActivity;
import com.spectrum.smartapp.BeanObjects.UserBean;
import com.spectrum.smartapp.Utils.AmazonClientManager;
import com.spectrum.smartapp.Utils.DatabaseSqlHelper;
import com.spectrum.smartapp.Utils.DynamoDbHelper;
import com.spectrum.smartapp.Utils.PubNubUtil;

public class LoginActivity extends AppCompatActivity {

    public static AmazonClientManager clientManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        clientManager = new AmazonClientManager(this);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

    }

    @Override
    protected void onStart() {
        super.onStart();

        //Delete Database
//        DatabaseSqlHelper db = new DatabaseSqlHelper(getApplicationContext());
//        db.deleteDatabase();
        final DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());

        String loginStatus = databaseHelper.getLoginStatus();
        if (loginStatus != null && !loginStatus.equalsIgnoreCase("FALSE")) {
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            startActivity(intent);
        } else {

            Button signUpButton = (Button) findViewById(R.id.sign_up);

            int userCount = databaseHelper.getUserCount();
            if (userCount > 0) {
                signUpButton.setVisibility(View.GONE);
            } else {
                signUpButton.setVisibility(View.VISIBLE);
            }


            Button loginButton = (Button) findViewById(R.id.login);
            loginButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create();
                    alertDialog.setTitle("Error");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                    EditText userNameText = (EditText) findViewById(R.id.loginUsername);
                    String userName = userNameText.getText().toString();

                    EditText passwordText = (EditText) findViewById(R.id.password);
                    String password = passwordText.getText().toString();

                    if (userName.equals("") || password.equals("")) {
                        alertDialog.setMessage("Please provide a username and password");
                        alertDialog.show();

                    } else {
                        Log.d("SHRUTI", "User exists : " + databaseHelper.getUserDetails());
                        int userCount = databaseHelper.getUserCount();
                        if (userCount > 0) {
                            if (true) {
//                            if (databaseHelper.authenticateUser(userName, password)) {
                                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                                startActivity(intent);
                            } else {
                                alertDialog.setMessage("Please provide a username and password");
                                alertDialog.show();
                            }
                        } else {
                            final ProgressBar deviceProgressBar = (ProgressBar) findViewById(R.id.deviceProgressBar);
                            deviceProgressBar.setVisibility(View.VISIBLE);

                            String username = userNameText.getText().toString();
                            new RetriveUserTask().execute(username);
                        }

                    }


//                else if (databaseHelper.authenticateUser(userName, password)) {
//                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
//                    startActivity(intent);
//                databaseHelper.updateLoginStatus(userName, "TRUE");
//                } else {
//
//                    alertDialog.setTitle("Login Failed");
//                    alertDialog.setMessage("Username or password does not match. Please re-enter");
//                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
//                            new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.dismiss();
//                                }
//                            });
//                    alertDialog.show();
//                }


                }
            });

            signUpButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(getBaseContext(), RegisterActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private class RetriveUserTask extends AsyncTask<String, Void, UserBean> {

        @Override
        protected UserBean doInBackground(String... username) {

            UserBean user = DynamoDbHelper.retrieveUserInfo(username[0]);

            return user;

        }

        @Override
        protected void onPostExecute(UserBean user) {
            final ProgressBar  deviceProgressBar = (ProgressBar) findViewById(R.id.deviceProgressBar);
            deviceProgressBar.setVisibility(View.GONE);
            if (user == null) {
                Toast.makeText(LoginActivity.this, "Cannot add user at this time. Please try again later.",
                        Toast.LENGTH_LONG).show();
            } else {
                EditText passwordText = (EditText) findViewById(R.id.password);
                if (user.getPassword().equals(passwordText.getText().toString())) {
                    DatabaseSqlHelper databaseHelper = new DatabaseSqlHelper(getApplicationContext());
                    databaseHelper.registerUser(user, false);
                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
                    startActivity(intent);
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create();
                    alertDialog.setTitle("Error");
                    alertDialog.setMessage("Your password does not match. Please retry!!!");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }

            }
        }
    }
}
