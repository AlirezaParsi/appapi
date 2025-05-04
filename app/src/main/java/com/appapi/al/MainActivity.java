package com.appapi.al;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start the API service
        Intent serviceIntent = new Intent(this, AppApiService.class);
        startService(serviceIntent);

        // Display status
        TextView statusText = findViewById(R.id.status_text);
        statusText.setText("App API Service is running at http://127.0.0.1:8080/api/apps");
    }
}
