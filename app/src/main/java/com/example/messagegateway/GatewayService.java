package com.example.messagegateway;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GatewayService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
