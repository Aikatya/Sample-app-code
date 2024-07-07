package be.kuleuven.gt.todolist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * CalendarPage displays a calendar with all days with events highlighted.
 * When a calendar date is selected, all tasks for the day are displayed in a RecyclerView
 */
public class CalendarPage extends AppCompatActivity implements OnTaskInteractionListener, IJSONResponseListener {
    private User user;
    private String dateSelected;
    private MaterialCalendarView calendarView;
    private RecyclerView toDoListView;
    private List<ToDo> toDoList = new ArrayList<>();
    private HashSet<String> datesToMark;
    private ToDoListDatabaseConnection databaseConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar);
        databaseConnection = new ToDoListDatabaseConnection(this);

        user = getIntent().getParcelableExtra("user");
        @SuppressLint({"NewApi", "LocalSuppress"}) LocalDate lt = LocalDate.now();
        dateSelected = lt.toString();
        calendarView = findViewById(R.id.calendar);
        toDoListView = findViewById(R.id.listFromCalendar);
        ToDoAdapter adapter = new ToDoAdapter(toDoList, this);
        toDoListView.setAdapter(adapter);
        toDoListView.setLayoutManager(new LinearLayoutManager(this));

        datesToMark = new HashSet<>();
        findAllEventDays(datesToMark);
        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                String stringDay = dayToString(day);
                return datesToMark.contains(stringDay);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new DotSpan(5, Color.parseColor("#FF0000")));
            }
        });

        String birthday = user.getBirthday();
        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                String stringDay = dayToString(day);
                if (birthday!=null){
//                    checks month and day of birthday
                    return birthday.regionMatches(4, stringDay, 4, 6);
                }
                else {
                    return false;
                }
            }

            @Override
            public void decorate(DayViewFacade view) {
                Resources res = getResources();
                Drawable drawable = ResourcesCompat.getDrawable(res, R.drawable.balloon, null);
                assert drawable != null;
                view.setBackgroundDrawable(drawable);
            }
        });

        calendarListener();
    }
    public void calendarListener(){
        calendarView.setOnDateChangedListener(
                        new OnDateSelectedListener() {
                            @Override
                            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
//                                Listener has one method where it will get the date
//                                Store the value of date with format in String
                                dateSelected = dayToString(date);
//                                returns JSON with list of uncompleted tasks ordered by increasing priority
                                databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/getTasksByDay/"+user.getUserID()+"/"+dateSelected,
                                        "update task list");
                            }
                        }
                        );
    }
    public void onListButton_Clicked(View Caller){
        Intent intent = new Intent(CalendarPage.this, Show_ToDo.class);
        intent.putExtra("user", user);
        startActivity(intent);
    }
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onTaskChecked(ToDo task) throws UnsupportedEncodingException {
        String encodedTitle = URLEncoder.encode(task.getTitle(), "UTF-8");
        databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/markTaskCompeted/"+user.getUserID()+"/"+encodedTitle,
                "mark task as completed");
        toDoList.remove(task);
        toDoListView.getAdapter().notifyDataSetChanged();
    }
    private void getAllDates(JSONArray response) {
        try {
            datesToMark.clear();
            for (int i = 0; i < response.length(); i++) {
                datesToMark.add(response.getJSONObject(i).getString("dueDate"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
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

    public void onTaskSelected(ToDo todo){
        Intent intent = new Intent(CalendarPage.this, CreateTaskPage.class);
        intent.putExtra("user", user);
        if (todo != null){
            intent.putExtra("title", todo.getTitle());
            intent.putExtra("date", todo.getDate());
            intent.putExtra("priority", todo.getPriority());
            intent.putExtra("tag", todo.getTag());
            intent.putExtra("comment", todo.getComment());
            intent.putExtra("taskID", todo.getTaskID());
        }
        startActivity(intent);
    }

    private String dayToString(CalendarDay day) {
        StringBuilder date = new StringBuilder(String.valueOf(day.getYear()));
        date.append(day.getMonth() <10? "-0"+day.getMonth(): "-"+day.getMonth());
        date.append(day.getDay() <10? "-0"+day.getDay(): "-"+day.getDay());
        return date.toString();
    }

    private void findAllEventDays(HashSet<String> datesToMark) {
        databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/getAllDaysWithTasks/"+user.getUserID(),
                "find all days with tasks");
    }

    @Override
    public void processResponse(JSONArray response, String actionPerformed) {
        if (actionPerformed.equals("update task list")) {
            updateTaskList(response);
        }
        else if (actionPerformed.equals("find all days with tasks")) {
            getAllDates(response);
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
}