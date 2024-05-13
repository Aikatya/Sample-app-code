package be.kuleuven.gt.todolist;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarPage extends AppCompatActivity implements OnTaskCheckedListener, useOfRecyclerView {

    private User user;
    private String dateSelected;
    private MaterialCalendarView calendarView;
    private RecyclerView toDoListView;
    private List<ToDo> toDoList = new ArrayList<>();;
    private ToDo selectedTask = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar);

        user = getIntent().getParcelableExtra("user");
        @SuppressLint({"NewApi", "LocalSuppress"}) LocalDate lt = LocalDate.now();
        dateSelected = lt.toString();
        calendarView = findViewById(R.id.calendar);
        toDoListView = findViewById(R.id.listFromCalendar);
        ToDoAdapter adapter = new ToDoAdapter(toDoList, this);
        toDoListView.setAdapter(adapter);
        toDoListView.setLayoutManager(new LinearLayoutManager(this));
        calendarListener();
    }

    public void calendarListener(){
        calendarView.setOnDateChangedListener(
                        new OnDateSelectedListener() {
                            @Override
                            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                                // Listener has one method where it will get the date
                                    // Store the value of date with format in String
                                dateSelected = String.valueOf(date.getYear());
                                if (date.getMonth() <10){
                                    dateSelected+="-0" + (date.getMonth());
                                }
                                else {
                                    dateSelected+="-" + (date.getMonth());
                                }
                                if (date.getDay() <10){
                                    dateSelected+="-0" + (date.getDay());
                                }
                                else {
                                    dateSelected+="-" + (date.getDay());
                                }
//                                returns JSON with list of uncompleted tasks ordered by increasing priority
                                contactDatabase("https://studev.groept.be/api/a23PT205/getTasksByDay/"+user.getUserID()+"/"+dateSelected, "update task list");
                            }
                        }
                        );
    }

    public void onListButton_Clicked(View Caller){
        Intent intent = new Intent(CalendarPage.this, Show_ToDo.class);
        intent.putExtra("user", user);
        startActivity(intent);
    }

    /** This is an alternative to the method in Show_ToDo. Instead of deleting completed tasks, they are marked as completed in the database and not displayed.
     * It would then be possible to implement a page with completed tasks to avoid having to rewrite everything if a task needs to be done again
     * If this is not necessary, can be replaced with the same method as in Show_ToDo
     */
    @Override
    public void onTaskChecked(ToDo task) throws UnsupportedEncodingException {
        String encodedTitle = URLEncoder.encode(task.getTitle(), "UTF-8");
        contactDatabase("https://studev.groept.be/api/a23PT205/markTaskCompeted/"+user.getUserID()+"/"+encodedTitle, "mark task as completed");
        toDoList.remove(task);
        toDoListView.getAdapter().notifyDataSetChanged();
    }
    private void contactDatabase(String QUEUE_URL, String actionPerformed) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest queueRequest = new JsonArrayRequest(
                Request.Method.GET,
                QUEUE_URL,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (actionPerformed.equals("update task list")) {
                            updateTaskList(response);
                        }
                        else if (actionPerformed.equals("find by title")) {
                            getTaskFromDB(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(
                                CalendarPage.this,
                                "Unable to contact database",
                                Toast.LENGTH_LONG).show();
                    }
                });
        requestQueue.add(queueRequest);
    }

    private void getTaskFromDB(JSONArray response) {
        try {
            selectedTask = new ToDo(response.getJSONObject(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(CalendarPage.this, CreateTaskPage.class);
        intent.putExtra("user", user);
        if (selectedTask != null){
            intent.putExtra("title", selectedTask.getTitle());
            intent.putExtra("date", selectedTask.getDate());
            intent.putExtra("priority", selectedTask.getPriority());
            intent.putExtra("tag", selectedTask.getTitle());
            intent.putExtra("comment", selectedTask.getComment());
        }
        startActivity(intent);
    }

    private void updateTaskList(JSONArray response) {
        try {
            toDoList.clear();
            for (int i = 0; i < response.length(); i++) {
                ToDo todo = new ToDo(response.getJSONObject(i));
                toDoList.add(todo);
            }
            toDoListView.getAdapter().notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void onAddTaskButton_Clicked(View Caller){
        Intent intent = new Intent(CalendarPage.this, CreateTaskPage.class);
        intent.putExtra("user", user);
        intent.putExtra("date", dateSelected);
        startActivity(intent);
    }


    //need to implement somewhere else, this will not work in Show_ToDo
    public void onTask_Selected(View Caller){
        String title ="test"; //needs to get title from recycler view
        contactDatabase("https://studev.groept.be/api/a23PT205/getTaskByTitle/"+title+"/"+user.getUserID(), "find by title");
    }

}