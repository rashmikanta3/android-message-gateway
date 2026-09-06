package com.example.messagegateway;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.SmsManager;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;

public class GatewayService extends Service {
    private boolean isRunning = false;
    private final OkHttpClient httpClient = new OkHttpClient();
    private String queueUrl = "";
    private String apiToken = "";

    @Override
    public void onCreate() {
        super.onCreate();

        String channelId = "gateway_service_channel";
        NotificationChannel channel = new NotificationChannel(channelId, "Gateway Service", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("Message Gateway Running")
                .setContentText("Listening to cloud queue...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        startForeground(1, notification);

        SharedPreferences prefs = getSharedPreferences("gateway_prefs", MODE_PRIVATE);
        queueUrl = prefs.getString("queue_url", "");
        apiToken = prefs.getString("api_token", "");

        isRunning = true;
        startPolling();
    }

    private void startPolling() {
        new Thread(() -> {
            while (isRunning) {
                if (!queueUrl.isEmpty()) {
                    try {
                        Request request = new Request.Builder()
                                .url(queueUrl)
                                .addHeader("Authorization", "Bearer " + apiToken)
                                .get()
                                .build();

                        Response response = httpClient.newCall(request).execute();
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            JSONArray items = new JSONArray(responseBody);

                            for (int i = 0; i < items.length(); i++) {
                                JSONObject task = items.getJSONObject(i);
                                String id = task.optString("id");
                                String type = task.optString("type");
                                String phone = task.optString("phone");
                                String message = task.optString("message");

                                if ("sms".equalsIgnoreCase(type)) {
                                    sendSms(phone, message);
                                } else if ("whatsapp".equalsIgnoreCase(type)) {
                                    triggerWhatsApp(phone, message);
                                }

                                deleteMessageFromQueue(id);
                            }
                        }
                    } catch (Exception ignored) {}
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void deleteMessageFromQueue(String id) {
        try {
            Request deleteRequest = new Request.Builder()
                    .url(queueUrl + "/" + id)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .delete()
                    .build();
            httpClient.newCall(deleteRequest).execute();
        } catch (Exception ignored) {}
    }

    private void sendSms(String phone, String message) {
        try {
            SmsManager smsManager = getSystemService(SmsManager.class);
            smsManager.sendTextMessage(phone, null, message, null, null);
        } catch (Exception ignored) {}
    }

    private void triggerWhatsApp(String phone, String message) {
        try {
            String cleanPhone = phone.replace("+", "").replace(" ", "").trim();
            String encodedText = URLEncoder.encode(message, "UTF-8");
            Uri uri = Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + encodedText);

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.whatsapp");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}