package be.kuleuven.gt.todolist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayDeque;
import java.util.Base64;

/**
 * Create task page allows user to create or update a task
 * Photos can be taken and added to tasks from this page
 */
public class CreateTaskPage extends AppCompatActivity implements IJSONResponseListener{
    private EditText titleText;
    private EditText dateText;
    private TextView priorityLevel;
    private EditText tagText;
    private EditText commentText;
    private ImageView[] imageDisplays;
    private ArrayDeque<String> uris;
    private int tagID;
    private int taskID;
    private String encodedTitle;
    private int priority;
    private String encodedComment;
    private String date;
    private User user;
    private ToDoListDatabaseConnection databaseConnection;

    /**
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @SuppressLint("MissingInflatedId")
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
        Button cameraButton = findViewById(R.id.cameraButton);
        ImageView imageDisplay1 = findViewById(R.id.photo1);
        ImageView imageDisplay2 = findViewById(R.id.photo2);
        imageDisplays = new ImageView[] {imageDisplay1, imageDisplay2 };
        tagID=0;
        uris = new ArrayDeque<>();
        databaseConnection = new ToDoListDatabaseConnection(this);

        taskID = getIntent().getIntExtra("taskID", -1);
        user = getIntent().getParcelableExtra("user");

        setTextFromExtra(titleText, "title");
        setTextFromExtra(dateText, "date");
        setIntFromExtra(priorityLevel, "priority");
        setTextFromExtra(commentText, "comment");

        //obtains the name of a tag using the tagID passed as an extra
        if (getIntent().getIntExtra("tag", -1)!=-1 ){
            databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/getTagByID/"+getIntent().getIntExtra("tag", -1) ,
                    "get name of tag");
        }
        //obtains and displays photos associated with the provided taskID
        if (taskID!=-1 ){
            databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/getPicturesForTask/"+taskID,
                    "get photos");
        }

        //displays the camera and allows the user to take a picture, called by a button
        ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            // BitMap is data structure of image file which store the image in memory
                            Bitmap photo = (Bitmap) data.getExtras().get("data");

                            imageDisplays[1].setImageDrawable(imageDisplay1.getDrawable());
                            imageDisplays[0].setImageBitmap(photo);

                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            photo.compress(Bitmap.CompressFormat.JPEG, 100, bytes); // Used for compression rate of the Image : 100 means no compression
                            String path = MediaStore.Images.Media.insertImage(
                                    getApplicationContext().getContentResolver(), photo, "todo picture", null);
                            Uri uri = Uri.parse(path);//uri of the location at which the taken photograph is stored n the device

                            byte[] uriBytes = uri.toString().getBytes();
                            String uriEncoded = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)?
                                    Base64.getEncoder().encodeToString(uriBytes): "";
                            addToUris(uriEncoded);
                        }
                    }
                });

        //button opens the camera
        cameraButton.setOnClickListener(v -> {// will open the camera for capture the image
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraActivityResultLauncher.launch(cameraIntent);
        });
    }

    /**
     * @param control is the TextView for which the text needs to be set
     * @param name is the tag for the StringExtra that needs to be passed to the TextView
     */
    private void setTextFromExtra(TextView control, String name){
        String stringExtra = getIntent().getStringExtra(name);
        if (stringExtra !=null && !stringExtra.equals("null")){
            control.setText(stringExtra);
        }
    }

    /**
     * @param control is the TextView for which the text needs to be set
     * @param name is the tag for the IntExtra that needs to be passed to the TextView
     */
    private void setIntFromExtra(TextView control, String name){
        int intExtra = getIntent().getIntExtra(name, -1);
        if (intExtra !=-1){
            control.setText(String.valueOf(intExtra));
        }
    }

/**
 * submit button updates an existing or creates a new task
 * first updates tag in the tags database
 * a task has a unique title-user combination. When a duplicate task is submitted, date, tag, priority and comment for existing task are updated
 * calls method to upload picture locations to the database
 */
    public void onSubmitButton_Clicked(View Caller) throws UnsupportedEncodingException {
        String title = titleText.getText().toString();
        encodedTitle = URLEncoder.encode(title, "UTF-8");
        String comment = commentText.getText().toString();
        encodedComment = URLEncoder.encode(comment, "UTF-8");
        String tag = tagText.getText().toString();
        priority = Integer.parseInt(priorityLevel.getText().toString());
        date = dateText.getText().toString();

//        updates tags in database
        databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/updateTags/"+tag+"/"+user.getUserID(),
                "tags");
//        creating a task happens in onResponse of tag generation
    }

    /**
     * calls method to delete a task from the database based on the current text in the titleView
     */
    public void onDeleteButton_Clicked(View Caller) throws UnsupportedEncodingException {
        String title = titleText.getText().toString();
        encodedTitle = URLEncoder.encode(title, "UTF-8");
        databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/deleteTask/" + encodedTitle +"/"+ user.getUserID(),
                "delete");
    }

    public void onIncreasePriorityButton_Clicked(View Caller) {
        if (Integer.parseInt(priorityLevel.getText().toString())<5) { // 5 is set to be the max priority level
            int priority = Integer.parseInt(priorityLevel.getText().toString()) + 1;
            priorityLevel.setText(String.valueOf(priority));
        }
    }
    public void onReducePriorityButton_Clicked(View Caller) {
        if (Integer.parseInt(priorityLevel.getText().toString())>0) {
            int priority = Integer.parseInt(priorityLevel.getText().toString()) - 1;
            priorityLevel.setText(String.valueOf(priority));
        }
    }
    private void getTagTitle(JSONArray response) {
        String tagTitle = "";
        try {
            tagTitle = response.getJSONObject(0).getString("tag");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        tagText.setText(tagTitle);
    }

    private void deleteTask() {
        Toast.makeText(
            CreateTaskPage.this,
            "Task deleted successfully",
            Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(CreateTaskPage.this, Show_ToDo.class);
        intent.putExtra("user", user);
        startActivity(intent);
    }

    private void uploadTask(JSONArray response) {
        try {
            taskID = response.getJSONObject(0).getInt("taskID");
            uris.forEach(uri -> //sends all current uris to the database with the taskID obtained from the JSONArray
            databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/uploadPictureForTask/"+uri+"/"+taskID,
                    "upload photo"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
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
        databaseConnection.contactDatabase("https://studev.groept.be/api/a23PT205/createTask/"+user.getUserID()+"/"+encodedTitle+"/"+date+"/"+priority+"/"+tagID+"/"+encodedComment,
                "tasks");
    }
    private void getPhotos(JSONArray response) {
        try {
//            gets uri with image location  from database
            for (int i=0; i<2; i++){
                String encodedString = (String) response.getJSONObject(i).get("pictureLocationCode");
                byte[] locationBytes = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)?
                        java.util.Base64.getDecoder().decode(encodedString): null;

                String uriString = new String(locationBytes);
                addToUris(uriString);
                Uri uri = Uri.parse(uriString);

    //            gets image located at the uri
                Bitmap photoBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
    //            displays image in view
                imageDisplays[i].setImageBitmap(photoBitmap);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * used to limit number of uris stored to two
     */
    private void addToUris(String uriString) {
        uris.addFirst(uriString);
        if (uris.size()==2){//only two pictures can be displayed at a time
            uris.removeLast();
        }
    }
    @Override
    public void processResponse(JSONArray response, String actionPerformed) {
        switch (actionPerformed) {
            case "tags":
                updateTags(response);
                break;
            case "tasks":
                uploadTask(response);
                break;
            case "delete":
                deleteTask();
                break;
            case "get name of tag":
                getTagTitle(response);
                break;
            case "get photos":
                getPhotos(response);
                break;
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