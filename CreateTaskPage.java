package be.kuleuven.gt.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class CreateTaskPage extends AppCompatActivity {
    private EditText titleText;
    private EditText dateText;
    private TextView priorityLevel;
    private EditText tagText;
    private EditText commentText;
    private int tagID;
    private String encodedTitle;
    private int priority;
    private String encodedComment;
    private String date;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_task_page);
        titleText = findViewById(R.id.titleText);
        dateText = findViewById(R.id.dateText);
        priorityLevel = findViewById(R.id.priorityLevel);
        tagText = findViewById(R.id.tagText);
        commentText = findViewById(R.id.commentText);
        tagID=0;

        user = getIntent().getParcelableExtra("user");

        if (getIntent().getStringExtra("title")!=null){
            titleText.setText(getIntent().getStringExtra("title"));
        }
        if (getIntent().getStringExtra("date")!=null){
            dateText.setText(getIntent().getStringExtra("date"));
        }
        if (getIntent().getStringExtra("priority")!=null){
            priorityLevel.setText(getIntent().getStringExtra("priority"));
        }
        if (getIntent().getStringExtra("tag")!=null){
            tagText.setText(getIntent().getStringExtra("tag"));
        }
        if (getIntent().getStringExtra("comment")!=null){
            commentText.setText(getIntent().getStringExtra("comment"));
        }
    }

/**    submit button updates an existing or creates a new task
 *    first updates tag in the tags database
 *    a task has a unique title-user combination. When a duplicate task is submitted, date, tag, priority and comment for existing task are updated
 *    Currently, only single words can be passed to title and comment, need to fix this */
    public void onSubmitButton_Clicked(View Caller) throws UnsupportedEncodingException {
        String title = titleText.getText().toString();
        encodedTitle = URLEncoder.encode(title, "UTF-8");
        String comment = commentText.getText().toString();
        encodedComment = URLEncoder.encode(comment, "UTF-8");
        String tag = tagText.getText().toString();
        priority = Integer.parseInt(priorityLevel.getText().toString());
        date = dateText.getText().toString();

//        updates tags in database
        contactDatabase("https://studev.groept.be/api/a23PT205/updateTags/"+tag+"/"+user.getUserID(), "tags");
//        creating a task happens in onResponse of tag generation
    }

    public void onDeleteButton_Clicked(View Caller){
        String title = titleText.getText().toString();
        contactDatabase("https://studev.groept.be/api/a23PT205/deleteTask/"+title+"/"+user.getUserID(), "delete");
    }

    public void onIncreasePriorityButton_Clicked(View Caller) {
        if (Integer.parseInt(priorityLevel.getText().toString())<5) { // 5 is set to be the max priority level
            int priority = Integer.parseInt(priorityLevel.getText().toString()) + 1;
            priorityLevel.setText(Integer.toString(priority));
        }
    }
    public void onReducePriorityButton_Clicked(View Caller) {
        if (Integer.parseInt(priorityLevel.getText().toString())>0) {
            int priority = Integer.parseInt(priorityLevel.getText().toString()) - 1;
            priorityLevel.setText(Integer.toString(priority));
        }
    }

    private void contactDatabase(String GET_URL, String databaseContacted) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest queueRequest = new JsonArrayRequest(
                Request.Method.GET,
                GET_URL,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (databaseContacted.equals("tags")){
                            updateTags(response);
                        }
                        else if (databaseContacted.equals("tasks")){
                            uploadTask(response);
                        }
                        else if (databaseContacted.equals("delete")){
                            deleteTask(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(
                                CreateTaskPage.this,
                                "Unable to contact database",
                                Toast.LENGTH_LONG).show();
                    }
                });
        requestQueue.add(queueRequest);
    }

    private void deleteTask(JSONArray response) {
        Toast.makeText(
            CreateTaskPage.this,
            "Task deleted successfully"+tagID,
            Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(CreateTaskPage.this, Show_ToDo.class);
        intent.putExtra("user", user);
        startActivity(intent);
    }

    private void uploadTask(JSONArray response) {
        Toast.makeText(
                CreateTaskPage.this,
                "Task uploaded successfully",
                Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(CreateTaskPage.this, Show_ToDo.class);
        intent.putExtra("user", user);
        startActivity(intent);
    }

    private void updateTags(JSONArray response) {
        try {
            tagID = response.getJSONObject(0).getInt("tagID");
            Toast.makeText(
                    CreateTaskPage.this,
                    "Tag updated successfully",
                    Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
//      creates task. Needs to be created after tag has finished updating in database
        contactDatabase("https://studev.groept.be/api/a23PT205/createTask/"+encodedTitle+"/"+date+"/"+priority+"/"+tagID+"/"+encodedComment+"/"+user.getUserID(), "tasks");
    }
}