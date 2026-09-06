package com.example.messagegateway;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private EditText etQueueUrl;
    private TextView tvToken;
    private Button btnToggleService;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("gateway_prefs", MODE_PRIVATE);
        etQueueUrl = findViewById(R.id.etQueueUrl);
        tvToken = findViewById(R.id.tvToken);
        btnToggleService = findViewById(R.id.btnToggleService);

        etQueueUrl.setText(prefs.getString("queue_url", ""));
        loadOrGenerateToken();

        findViewById(R.id.btnRegenToken).setOnClickListener(v -> {
            String newToken = "gw_sec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            prefs.edit().putString("api_token", newToken).apply();
            tvToken.setText(newToken);
            Toast.makeText(this, "New token saved!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnPermissions).setOnClickListener(v -> {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 101);
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this, "Enable 'Message Gateway' in Accessibility", Toast.LENGTH_LONG).show();
        });

        btnToggleService.setOnClickListener(v -> {
            String url = etQueueUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter your cloud queue URL", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit().putString("queue_url", url).apply();

            Intent serviceIntent = new Intent(this, GatewayService.class);
            if (!isRunning) {
                startForegroundService(serviceIntent);
                isRunning = true;
                btnToggleService.setText("Stop Listening");
                Toast.makeText(this, "Service started!", Toast.LENGTH_SHORT).show();
            } else {
                stopService(serviceIntent);
                isRunning = false;
                btnToggleService.setText("2. Start Listening");
            }
        });
    }

    private void loadOrGenerateToken() {
        String token = prefs.getString("api_token", null);
        if (token == null) {
            token = "gw_sec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            prefs.edit().putString("api_token", token).apply();
        }
        tvToken.setText(token);
    }
}