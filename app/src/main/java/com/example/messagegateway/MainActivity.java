package com.example.messagegateway;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "GatewayPrefs";
    public static final String KEY_API_URL = "api_url";
    public static final String KEY_CHANNEL = "channel_tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etApiUrl = findViewById(R.id.etApiUrl);
        EditText etChannel = findViewById(R.id.etChannel);
        Button btnSaveStart = findViewById(R.id.btnSaveStart);
        TextView tvStatus = findViewById(R.id.tvStatus);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        etApiUrl.setText(prefs.getString(KEY_API_URL, ""));
        etChannel.setText(prefs.getString(KEY_CHANNEL, "SMS"));

        btnSaveStart.setOnClickListener(v -> {
            String url = etApiUrl.getText().toString().trim();
            String channel = etChannel.getText().toString().trim();

            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter an API URL", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                .putString(KEY_API_URL, url)
                .putString(KEY_CHANNEL, channel)
                .apply();

            Intent serviceIntent = new Intent(this, GatewayService.class);
            stopService(serviceIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            tvStatus.setText("Service running: " + channel);
            Toast.makeText(this, "Saved & Service Started!", Toast.LENGTH_SHORT).show();
        });
    }
}
