package io.github.hufrea.keysh;

import android.content.Intent;
import android.media.AudioManager;
import android.os.PowerManager;
import android.util.Log;
import android.content.Context;

import io.github.hufrea.keysh.actions.ActionAudio;
import io.github.hufrea.keysh.actions.ActionIntent;
import io.github.hufrea.keysh.actions.ActionNotify;
import io.github.hufrea.keysh.actions.ActionTorch;
import io.github.hufrea.keysh.actions.ActionVibrate;

import java.io.*;
import java.util.regex.Pattern;

public class RunnerLoop {
    private static final String TAG = RunnerLoop.class.getSimpleName();
    private static final int DEFAULT_WAKELOCK = 5000;
    private static final int DEFAULT_VIBRATE = 50;

    private final Context context;
    final private AudioManager am;
    final private PowerManager pm;
    private PowerManager.WakeLock wl = null;

    private Thread thread = null;

    public RunnerLoop(Context context) {
        this.context = context;
        this.pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private int toInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void runner(String line) {
        String delimiter = " ";
        if (line.startsWith(":")) {
            String[] split = line.split(":", 3);
            if (split.length < 3) {
                return;
            }
            delimiter = Pattern.quote(split[1]);
            line = split[2];
        }
        String[] args = line.split(delimiter);
        switch (args[0]) {
            case "media":
                ActionAudio.mediaEvent(am, args);
                break;
            case "volume":
                ActionAudio.volumeSet(am, args);
                break;
            case "torch":
                if (args.length < 2) {
                    break;
                }
                boolean enable = args[1].equals("on");
                ActionTorch.turnFlashlight(context, enable);
                break;

            case "vibrate":
                int val = args.length > 1 ? toInt(args[1], DEFAULT_VIBRATE) : DEFAULT_VIBRATE;
                ActionVibrate.vibrate(context, val);
                break;

            case "wakelock":
                if (args.length < 2) {
                    break;
                }
                if (wl != null && wl.isHeld()) {
                    wl.release();
                }
                if (args[1].equals("acquire")) {
                    this.wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getPackageName() + ":lock");
                    val = args.length > 2 ? toInt(args[2], DEFAULT_WAKELOCK) : DEFAULT_WAKELOCK;
                    wl.acquire(val);
                }
                break;

            case "intent":
                try {
                    ActionIntent.sendIntent(context, args);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                    ActionNotify.notifyerror(context, e.toString(), 0);
                }
                break;

            case "permission":
                if (args.length < 2 ||
                        !context.getClass().equals(ActivityMain.class)) {
                    break;
                }
                Intent pintent = new Intent(context, ActivityPermission.class);
                pintent.putExtra("permission", args[1]);
                try {
                    context.startActivity(pintent);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                break;

            case "notify":
                ActionNotify.notify(context, args);
                break;

            case "stop_media":
                context.stopService(new Intent(context, ServiceMediaSession.class));
                break;

            case "stop_access":
                Intent intent = new Intent(context.getPackageName() + ".STOP");
                context.sendBroadcast(intent);
        }
    }

    private void readLoop(InputStream stdout) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "line: " + line);
                runner(line);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void start(InputStream stdout) {
        if (thread != null) {
            return;
        }
        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                readLoop(stdout);
            }
        });
        this.thread.start();
    }

    public void stop() {
        Log.d(TAG, "interrupt thread");
        this.thread.interrupt();
    }
}
