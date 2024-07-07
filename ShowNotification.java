package be.kuleuven.gt.todolist;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.LocalDate;
import java.util.ArrayList;

public class ShowNotification extends BroadcastReceiver implements IJSONResponseListener{
    private User user;
    private boolean birthday;
    private Context context;
    private ArrayList<String> tasksForTheDay = new ArrayList<>();

    /**
     * When the alarm from the Login page is triggered, it is received here and the list of tasks for the current day is fetched
     * A happy birthday message is displayed on the current user's birthday
     */
    public void onReceive(Context context, Intent intent) {
        user = intent.getParcelableExtra("user");
        ToDoListDatabaseConnection databaseConnection = new ToDoListDatabaseConnection(this);
        this.context = context;
        @SuppressLint({"NewApi", "LocalSuppress"}) LocalDate lt = LocalDate.now();
        String day = lt.toString();
        birthday = (user.getBirthday().regionMatches(4, day, 4, 6));
        databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/getTasksByDay/"+user.getUserID()+"/"+day,
                "");
    }

    /**
     * Sets up a notification channel and specifies the contents and intent of the created notification
     */
    public void sendNotification(){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "to-do_alarm";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "To-do notification", NotificationManager.IMPORTANCE_DEFAULT);
            // Configure the notification channel.
            notificationChannel.setDescription("to-do list alarm");
            notificationChannel.setVibrationPattern(new long[]{0, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Intent nextIntent = new Intent(context, CalendarPage.class);
        nextIntent.putExtra("user", user);
        nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setSmallIcon(R.drawable.app_logo)
                .setContentIntent(pendingIntent)
                .setContentTitle("To-do list reminder")
                .setContentText(this.getShortSummary())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(this.taskListToString()))
                .build();
        notificationManager.notify(1, notificationBuilder.build());
    }

    /**
     * Creates a string that is displayed when the notification is expanded, contains full list of task titles for the current day
     */
    private String taskListToString(){
        if (!tasksForTheDay.isEmpty()){
            StringBuilder longComment = new StringBuilder("Tasks to do today: ");
            tasksForTheDay.forEach(s -> longComment.append(s).append("\n"));
            return longComment.toString();
        }
        else {
            return "No tasks for the day. Enjoy your break!";
        }
    }

    /**
     * Creates a string that is displayed in the short form notification, contains number of tasks for the current day
     * On user's birthday happy birthday message is displayed instead
     */
    private String getShortSummary() {
        return (birthday) ? "Happy birthday!" : tasksForTheDay.size()+" tasks due today";
    }


    /**
     * Fills the list of task for the current day from a JSON file
     */
    @Override
    public void processResponse(JSONArray response, String actionPerformed) {
        try {
            tasksForTheDay.clear();
            for (int i = 0; i < response.length(); i++) {
                ToDo todo = new ToDo(response.getJSONObject(i));
                tasksForTheDay.add(todo.getTitle());
            }
            sendNotification();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void errorResponse(String error) {
        Toast.makeText(
                context,
                "Unable to communicate with the server",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public Context getContext() {
        return context;
    }
}