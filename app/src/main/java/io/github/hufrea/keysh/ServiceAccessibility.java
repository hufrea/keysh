package io.github.hufrea.keysh;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;


public class ServiceAccessibility extends AccessibilityService {
    private static final String TAG = ServiceAccessibility.class.getSimpleName();

    private HandlerButton buttonHandler = null;
    private BroadcastReceiver receiver;
    private boolean stopped = false;
    private boolean send_app = false;
    private String foreground_app = null;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pm = event.getPackageName().toString();
        if (pm.equals(foreground_app)) {
            return;
        }
        Log.d(TAG, "pm: " + pm);
        foreground_app = pm;
        if (buttonHandler != null && send_app) {
            buttonHandler.writeRAW("app:" + pm + "\n");
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        Log.d(TAG, "onKeyEvent: " + event.getKeyCode());
        if (stopped || buttonHandler == null) {
            return super.onKeyEvent(event);
        }
        int dir;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                dir = 1;
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                dir = -1;
                break;
            default:
                return super.onKeyEvent(event);
        }
        if (event.getAction() == KeyEvent.ACTION_UP)
            buttonHandler.onButtonRelease();
        else
            buttonHandler.onButtonPress(dir);
        return true;
    }

    @Override
    public void onInterrupt() {
        buttonHandler.deinit();
        unregisterReceiver(receiver);
    }

    @Override
    public void onServiceConnected() {
        String ACTION_RESTART = getPackageName() + ".RESTART";
        String ACTION_PAUSE = getPackageName() + ".PAUSE";
        String ACTION_RESUME = getPackageName() + ".RESUME";
        String ACTION_RECV_APP_SWITCH = getPackageName() + ".RECV_APP_SWITCH";

        Log.d(TAG, "onServiceConnected");

        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "broadcast: " + intent.getAction());

                if (intent.getAction().equals(ACTION_RECV_APP_SWITCH)) {
                    send_app = true;
                    return;
                }
                else if (intent.getAction().equals(ACTION_PAUSE)) {
                    stopped = true;
                    return;
                }
                else if (intent.getAction().equals(ACTION_RESUME)) {
                    stopped = false;
                    return;
                }
                String data = "AccessibilityService";
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object object = bundle.get(getPackageName() + ".DATA");
                    if (object != null) {
                        data = object.toString();
                    }
                }
                buttonHandler.deinit();
                buttonHandler = new HandlerButton(context, data);
                stopped = false;
                send_app = false;
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESTART);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_RESUME);
        filter.addAction(ACTION_RECV_APP_SWITCH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
        startService(new Intent(this, ServiceMediaSession.class));

        this.buttonHandler = new HandlerButton(this, "AccessibilityService");
    }
}