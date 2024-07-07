package be.kuleuven.gt.todolist;

import android.content.Context;

import org.json.JSONArray;

/**
 * This interface is used by classes that need to contact the database.
 * The processResponse method is required by the ToDoListDatabaseConnection to specify what needs to be done with the JSON response
 */
public interface IJSONResponseListener {
    void processResponse(JSONArray response, String actionPerformed);
    void errorResponse(String error);
    Context getContext();
}
