package be.kuleuven.gt.todolist;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * The login page is the first page of the app shown to the user
 * The user can login or create a new user.
 * On new user creation, a prompt to enter the user's birthday is shown
 */

public class LoginPage extends AppCompatActivity implements IJSONResponseListener {
    private EditText userNameText;
    private EditText passwordText;
    private EditText birthdayText;
    private User user;
    private ToDoListDatabaseConnection databaseConnection;

    /**
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        userNameText = findViewById(R.id.userNameText);
        passwordText = findViewById(R.id.passwordText);
        birthdayText = findViewById(R.id.birthdayText);
        user = null;
        databaseConnection = new ToDoListDatabaseConnection(this);
    }

    /**
     * checks if password matches username from database, sets user for which tasks are shown
     */
    public void onLoginButton_Clicked(View Caller)
    {
        String userName = userNameText.getText().toString();
        String password = passwordText.getText().toString();

//        requests the given user from the database
        databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/getUser/"+userName+"/"+password,
                "");

    }

    /**
     * first time create user button is clicked, asks for birthday
     * second time creates new/updates existing user and password in database, opens task list for this (new) user
     */
    public void onNewUserButton_Clicked(View Caller){
        if (!(birthdayText.getVisibility() ==View.VISIBLE)){
            birthdayText.setVisibility(View.VISIBLE);
            findViewById(R.id.birthdayTitle).setVisibility(View.VISIBLE);
        }
        else {
            String userName = userNameText.getText().toString();
            String password = passwordText.getText().toString();
            String birthday = birthdayText.getText().toString();

//        uses a stored procedure to insert a user into and select the new user from the database
            databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/createUser/"+userName+"/"+password+"/"+birthday,
                    "");
        }
    }

    /**
     * processes JSON response to create User object. If user is provided, opens list page of app
     * @param response is the JSONArray obtained from the api
     * @param actionPerformed is a String that is used to define what method needs to be executed depending on the information obtained
     */
    public void processResponse(JSONArray response, String actionPerformed) {
        boolean foundUser = false;
        try {
            user = new User(response.getJSONObject(0));
            foundUser = true;
            Toast.makeText(
                    LoginPage.this,
                    "Welcome "+user.getUserName(),
                    Toast.LENGTH_SHORT).show();

            triggerNotification();

            Intent intent = new Intent(LoginPage.this, CalendarPage.class);
            intent.putExtra("user", user);
            startActivity(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (!foundUser) {
            Toast.makeText(
                    LoginPage.this,
                    "user name and password incorrect",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void errorResponse(String error) {
        Toast.makeText(
                this,
                "Unable to communicate with the server",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public Context getContext() {
        return this;
    }

    /**
    triggers a repeating alarm that triggers a broadcast received by the ShowNotification broadcast receiver
     */
    public void triggerNotification(){
        Intent notificationIntent = new Intent(this, ShowNotification.class);
        notificationIntent.putExtra("user", user);
        PendingIntent pending = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending);
//        alarm triggered several seconds after the method is called, then once per day after that
        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), AlarmManager.INTERVAL_DAY, pending);
    }

}