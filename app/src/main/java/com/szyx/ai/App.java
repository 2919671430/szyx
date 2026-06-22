package com.szyx.ai;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.szyx.ai.data.db.AppDatabase;

public class App extends Application {

    public static final String CHANNEL_DOWNLOAD = "model_download";
    public static final String CHANNEL_GENERAL = "general";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        // Initialize database
        AppDatabase.getInstance(this);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel downloadChannel = new NotificationChannel(
                    CHANNEL_DOWNLOAD,
                    "模型下载",
                    NotificationManager.IMPORTANCE_LOW
            );
            downloadChannel.setDescription("模型下载通知");

            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "通用通知",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(downloadChannel);
            manager.createNotificationChannel(generalChannel);
        }
    }
}
