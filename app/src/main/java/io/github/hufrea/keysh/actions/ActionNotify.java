package io.github.hufrea.keysh.actions;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import io.github.hufrea.keysh.R;

public class ActionNotify {
    static public void notifyerror(Context context, String content, int id) {
        String channelId = "";
        NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "2";
            String channelName = "Error";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            nManager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setSubText("Error")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setCategory(Notification.CATEGORY_ERROR)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .build();
        nManager.notify(id, notification);
    }

    static public void notify(Context context, String[] args) {
        String channelId = "";
        NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "1";
            String channelName = "Shell";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            nManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        builder.setSmallIcon(R.drawable.ic_launcher_foreground);
        builder.setOnlyAlertOnce(true);

        int id = 0;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i], str = "";
            if (arg.startsWith("-")) {
                i++;
                if (i >= args.length) {
                    break;
                }
                str = args[i];
            }
            switch (arg) {
                case "--title":
                case "-t":
                    builder.setContentTitle(str);
                    break;
                case "--content":
                case "-c":
                    builder.setContentText(str);
                    break;
                case "--id":
                case "-i":
                    try {
                        id = Integer.parseInt(str);
                    } catch (NumberFormatException e) {
                        id = 0;
                    }
                    break;
                case "--timeout":
                case "-l":
                    long to;
                    try {
                        to = Long.parseLong(str);
                    } catch (NumberFormatException e) {
                        break;
                    }
                    builder.setTimeoutAfter(to);
            }
        }
        nManager.notify(id, builder.build());
    }
}
