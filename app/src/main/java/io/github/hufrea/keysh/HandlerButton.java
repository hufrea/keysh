package io.github.hufrea.keysh;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import io.github.hufrea.keysh.actions.ActionNotify;

import java.nio.charset.StandardCharsets;
import java.io.*;

public class HandlerButton {
    private static final String TAG = HandlerButton.class.getSimpleName();

    private static final String PRESS_UP = "key:2\n";
    private static final String RELEASE_UP = "key:3\n";
    private static final String PRESS_DOWN = "key:4\n";
    private static final String RELEASE_DOWN = "key:5\n";

    private long press_time = 0;
    private int direction = 0;

    final private Context context;

    private Process process;
    final private OutputStream stdin;
    final private RunnerLoop runner;


    private String getShellPath() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString("path", context.getFilesDir() + "/code.sh");
    }

    private Process initShell(String var) throws IOException {
        String path = getShellPath();

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", path);
        builder.directory(context.getFilesDir());
        try {
            int version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;
            builder.environment().put("VERSION", String.valueOf(version));
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        builder.environment().put("PACKAGE_NAME", context.getPackageName());
        builder.environment().put("PRESS_UP", PRESS_UP.substring(0, 5));
        builder.environment().put("RELEASE_UP", RELEASE_UP.substring(0, 5));
        builder.environment().put("PRESS_DOWN", PRESS_DOWN.substring(0, 5));
        builder.environment().put("RELEASE_DOWN", RELEASE_DOWN.substring(0, 5));
        builder.environment().put("VAR", var);

        return builder.start();
    }

    public HandlerButton(Context context, String var) {
        this.context = context;
        try {
            this.process = initShell(var);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            ActionNotify.notifyerror(context, e.toString(), 0);
        }
        this.stdin = process.getOutputStream(); // NullPoint?
        InputStream stdout = process.getInputStream();

        this.runner = new RunnerLoop(context);
        runner.start(stdout);
    }

    private void onIOError() {
        InputStream stderr = process.getErrorStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(stderr));

        ActionNotify.notifyerror(context, "script error", 0);
        String line;
        for (int i = 1; ; i++) {
            try {
                line = br.readLine();
                if (line == null) {
                    break;
                }
            } catch (IOException e) {
                Log.e(TAG, "read stderr: " + e);
                break;
            }
            Log.e("shell", line);
            ActionNotify.notifyerror(context, line, i);
        }
    }

    public void onButtonPress(int direction) {
        if (direction == this.direction) {
            return;
        }
        this.direction = direction;
        this.press_time = System.currentTimeMillis();

        String data = direction == 1 ? PRESS_UP : PRESS_DOWN;
        try {
            this.stdin.write(data.getBytes(StandardCharsets.UTF_8));
            this.stdin.flush();
        } catch (IOException e) {
            Log.e(TAG, "onButtonPress IO: " + e);
            onIOError();
        }
    }

    public void onButtonRelease() {
        long t = System.currentTimeMillis();
        Log.d(TAG, "press duration: " + (t - this.press_time));

        String data = direction == 1 ? RELEASE_UP : RELEASE_DOWN;
        try {
            this.stdin.write(data.getBytes(StandardCharsets.UTF_8));
            this.stdin.flush();
        } catch (IOException e) {
            Log.e(TAG, "onButtonRelease IO: " + e);
            onIOError();
            return;
        }
        this.press_time = 0;
        this.direction = 0;
    }

    public void writeRAW(String string) {
        try {
            this.stdin.write(string.getBytes(StandardCharsets.UTF_8));
            this.stdin.flush();
        } catch (IOException e) {
            Log.e(TAG, "writeRAW IO: " + e);
            onIOError();
        }
    }

    public void deinit() {
        this.process.destroy();
        this.runner.stop();
    }
}
