package com.eventadmin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText searchInput;
    private ProgressBar progressBar;
    private RegistrationAdapter adapter;
    private List<Registration> allRegistrations = new ArrayList<>();
    private RequestQueue requestQueue;
    
    private static final String API_BASE_URL = "http://10.10.214.62:3000/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Registrations List");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        recyclerView = findViewById(R.id.recyclerView);
        searchInput = findViewById(R.id.searchInput);
        progressBar = findViewById(R.id.progressBar);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RegistrationAdapter(allRegistrations);
        recyclerView.setAdapter(adapter);
        
        requestQueue = Volley.newRequestQueue(this);
        
        setupSearch();
        loadRegistrations();
    }
    
    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRegistrations(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void filterRegistrations(String query) {
        List<Registration> filtered = new ArrayList<>();
        for (Registration reg : allRegistrations) {
            if (reg.userId.toLowerCase().contains(query.toLowerCase()) ||
                reg.name.toLowerCase().contains(query.toLowerCase()) ||
                reg.college.toLowerCase().contains(query.toLowerCase()) ||
                reg.coordinator.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(reg);
            }
        }
        adapter.updateList(filtered);
    }
    
    private void loadRegistrations() {
        progressBar.setVisibility(View.VISIBLE);
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            API_BASE_URL + "/registrations",
            null,
            response -> {
                try {
                    JSONArray registrations = response.getJSONArray("registrations");
                    allRegistrations.clear();
                    
                    for (int i = 0; i < registrations.length(); i++) {
                        JSONObject reg = registrations.getJSONObject(i);
                        allRegistrations.add(new Registration(
                            reg.getString("userId"),
                            reg.getString("name"),
                            reg.optString("college", "N/A"),
                            reg.optInt("amount", 0),
                            reg.optString("coordinator", "N/A")
                        ));
                    }
                    
                    adapter.updateList(allRegistrations);
                    progressBar.setVisibility(View.GONE);
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            },
            error -> {
                Toast.makeText(this, "Failed to load registrations", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        );
        
        requestQueue.add(request);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
