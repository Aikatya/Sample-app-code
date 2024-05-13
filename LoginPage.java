package be.kuleuven.gt.todolist;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

public class LoginPage extends AppCompatActivity {
    private EditText userNameText;
    private EditText passwordText;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        userNameText = findViewById(R.id.userNameText);
        passwordText = findViewById(R.id.passwordText);
        user = null;
    }

//    checks if password matches username from database, sets user for which tasks are shown
    public void onLoginButton_Clicked(View Caller){
        String userName = userNameText.getText().toString();
        String password = passwordText.getText().toString();

//        this SQL query requests the given user from the database
        contactUserDatabase("https://studev.groept.be/api/a23PT205/getUser/"+userName+"/"+password);

    }

//    creates new/updates existing user and password in database, opens task list for this (new) user
    public void onNewUserButton_Clicked(View Caller){
        String userName = userNameText.getText().toString();
        String password = passwordText.getText().toString();

//        this SQL query uses a stored procedure to insert into and select from the database
        contactUserDatabase("https://studev.groept.be/api/a23PT205/createUser/"+userName+"/"+password);
    }

    private void contactUserDatabase(String GET_URL) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest queueRequest = new JsonArrayRequest(
                Request.Method.GET,
                GET_URL,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        getInfoFromJSON(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(
                                LoginPage.this,
                                "Unable to communicate with the server",
                                Toast.LENGTH_LONG).show();
                    }
                });
        requestQueue.add(queueRequest);
    }

//    first processes JSON response to create User object, then checks if it is null. If not, opens list page of app
    private void getInfoFromJSON(JSONArray response) {
        boolean foundUser = false;
        try {
            user = new User(response.getJSONObject(0));
            if (user!=null){
                foundUser = true;
                Toast.makeText(
                        LoginPage.this,
                        "Welcome "+user.getUserName(),
                        Toast.LENGTH_SHORT).show();

                triggerNotification();

                Intent intent = new Intent(LoginPage.this, CalendarPage.class);
                intent.putExtra("user", user);
                startActivity(intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
//      only gets executed when user was not found, should try to find a better solution than the boolean
        if (!foundUser) {
            Toast.makeText(
                    LoginPage.this,
                    "user name and password incorrect",
                    Toast.LENGTH_SHORT).show();
        }
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
        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), AlarmManager.INTERVAL_DAY, pending);
    }
}