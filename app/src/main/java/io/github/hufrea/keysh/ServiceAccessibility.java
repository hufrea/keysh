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
    private HandlerButton buttonHandler;
    private BroadcastReceiver receiver;
    private boolean stopped = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        Log.d("ServiceAccessibility", "onKeyEvent");
        if (stopped) {
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
        String ACTION_STOP = getPackageName() + ".STOP";

        Log.d("ServiceAccessibility", "onServiceConnected");
        this.buttonHandler = new HandlerButton(this, "AccessibilityService");
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!stopped) {
                    buttonHandler.deinit();
                }
                if (intent.getAction().equals(ACTION_STOP)) {
                    stopped = true;
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
                buttonHandler = new HandlerButton(context, data);
                stopped = false;
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESTART);
        filter.addAction(ACTION_STOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
        startService(new Intent(this, ServiceMediaSession.class));
    }
}