package be.kuleuven.gt.todolist;

import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

/**
 * This is a class containing one method to contact the database.
 * The constructor accepts an IJSONResponseListener
 */
public class ToDoListDatabaseConnection {

    private final IJSONResponseListener owner;
    public ToDoListDatabaseConnection(IJSONResponseListener owner) {
        this.owner=owner;
    }

    /**
     * The two parameters define the url of the connection and the action passed into the processResponse method of the IJSONResponseListener 'owner'
     * This method sends a get request to the provided url
     * @param GET_URL is the url of the page to be contacted
     * @param actionPerformed is a String passed to the processResponse method
     */
    public void contactDatabase(String GET_URL, String actionPerformed) {
        RequestQueue requestQueue = Volley.newRequestQueue(owner.getContext());
        JsonArrayRequest queueRequest = new JsonArrayRequest(
                Request.Method.GET,
                GET_URL,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        owner.processResponse(response, actionPerformed);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        owner.errorResponse(actionPerformed);
                    }
                });
        requestQueue.add(queueRequest);
    }
}
