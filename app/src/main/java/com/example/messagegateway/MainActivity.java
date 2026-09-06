package com.example.messagegateway;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "GatewayPrefs";
    public static final String KEY_API_URL = "api_url";
    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_WA_PACKAGE = "wa_package";

    private EditText etApiUrl, etChannel;
    private RadioButton rbWhatsApp, rbWhatsAppBusiness;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etApiUrl = findViewById(R.id.etApiUrl);
        etChannel = findViewById(R.id.etChannel);
        rbWhatsApp = findViewById(R.id.rbWhatsApp);
        rbWhatsAppBusiness = findViewById(R.id.rbWhatsAppBusiness);
        Button btnSaveStart = findViewById(R.id.btnSaveStart);
        tvStatus = findViewById(R.id.tvStatus);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        etApiUrl.setText(prefs.getString(KEY_API_URL, "https://6a9dade6a1b37296ad4c229a.mockapi.io/messages"));
        etChannel.setText(prefs.getString(KEY_CHANNEL, "ALL"));
        
        String savedPkg = prefs.getString(KEY_WA_PACKAGE, "com.whatsapp");
        if ("com.whatsapp.w4b".equals(savedPkg)) {
            rbWhatsAppBusiness.setChecked(true);
        } else {
            rbWhatsApp.setChecked(true);
        }

        btnSaveStart.setOnClickListener(v -> {
            String selectedPkg = rbWhatsAppBusiness.isChecked() ? "com.whatsapp.w4b" : "com.whatsapp";
            
            prefs.edit()
                .putString(KEY_API_URL, etApiUrl.getText().toString().trim())
                .putString(KEY_CHANNEL, etChannel.getText().toString().trim())
                .putString(KEY_WA_PACKAGE, selectedPkg)
                .apply();

            Intent serviceIntent = new Intent(this, GatewayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            tvStatus.setText("Service: Running / Polling");
            Toast.makeText(this, "Saved & Started Gateway", Toast.LENGTH_SHORT).show();
        });
    }
}
