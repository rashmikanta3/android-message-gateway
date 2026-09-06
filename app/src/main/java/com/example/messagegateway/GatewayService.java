package com.example.messagegateway;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GatewayService extends Service {
    private static final String TAG = "GatewayService";
    private static final String CHANNEL_ID = "GatewayServiceChannel";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                new Thread(() -> pollNextMessage()).start();
                handler.postDelayed(this, 5000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Message Gateway")
                .setContentText("Gateway service active & listening")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .build();
        startForeground(1, notification);
        isRunning = true;
        handler.post(pollRunnable);
    }

    private void pollNextMessage() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String apiUrl = prefs.getString(MainActivity.KEY_API_URL, "").trim();
        String selectedChannel = prefs.getString(MainActivity.KEY_CHANNEL, "SMS").trim();

        if (apiUrl.isEmpty()) {
            return;
        }

        try {
            Request request = new Request.Builder()
                    .url(apiUrl + "?status=PENDING&page=1&limit=1")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return;
                String jsonString = response.body().string();
                JSONArray array = new JSONArray(jsonString);
                if (array.length() == 0) return;

                JSONObject item = array.getJSONObject(0);
                String id = item.getString("id");
                String phone = item.getString("phone_number");
                String message = item.getString("message_body");
                String channel = item.optString("channel", "SMS");

                if (!"ALL".equalsIgnoreCase(selectedChannel) && !channel.equalsIgnoreCase(selectedChannel)) {
                    return;
                }

                if ("SMS".equalsIgnoreCase(channel)) {
                    sendSms(phone, message);
                } else if ("WHATSAPP".equalsIgnoreCase(channel)) {
                    sendWhatsApp(phone, message);
                }

                updateStatus(apiUrl, id, "SENT");
            }
        } catch (Exception e) {
            Log.e(TAG, "Polling error", e);
        }
    }

    private void sendSms(String phoneNumber, String message) {
        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d(TAG, "SMS dispatched to: " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Failed SMS send", e);
        }
    }

    private void sendWhatsApp(String phoneNumber, String message) {
        try {
            String cleanNumber = phoneNumber.replace("+", "").replace(" ", "").trim();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + cleanNumber + "&text=" + Uri.encode(message)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed WhatsApp intent", e);
        }
    }

    private void updateStatus(String apiUrl, String id, String status) {
        try {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create("{\"status\":\"" + status + "\"}", JSON);
            Request putRequest = new Request.Builder()
                    .url(apiUrl + "/" + id)
                    .put(body)
                    .build();
            client.newCall(putRequest).execute().close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to update status on API", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacks(pollRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Gateway Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
