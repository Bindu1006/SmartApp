package com.spectrum.smartapp;

import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.spectrum.smartapp.BeanObjects.UserBean;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

    }

    @Override
    protected void onStart() {
        super.onStart();

        Button registrationDetails = (Button) findViewById(R.id.next_register);
        registrationDetails.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                EditText passwordEditText = (EditText) findViewById(R.id.register_password);
                EditText confirmPasswordEditText = (EditText) findViewById(R.id.confirmPassword);
                String password = passwordEditText.getText().toString();
                String confirmPassword = confirmPasswordEditText.getText().toString();

                if (!password.equals(confirmPassword)) {
                    AlertDialog.Builder errorDialog = new AlertDialog.Builder(getApplicationContext());
                    errorDialog.setMessage("Passwords are not matched. Please re-enter");
                    errorDialog.setCancelable(false);

                    errorDialog.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    errorDialog.show();
                } else {
                    UserBean userDetails = new UserBean();
                    EditText userNameEditText = (EditText) findViewById(R.id.register_name);
                    userDetails.setUserName(userNameEditText.getText().toString());
                    userDetails.setPassword(password);
                    Intent intent = new Intent(getBaseContext(), RegistrationDetailsActivity.class);
                    intent.putExtra("USER_DETAILS", userDetails);
                    startActivity(intent);

                }



//                Intent intent = new Intent(getBaseContext(), MainActivity.class);
//                startActivity(intent);
            }
        });
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
}
