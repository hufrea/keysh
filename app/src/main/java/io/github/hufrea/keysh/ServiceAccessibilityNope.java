package io.github.hufrea.keysh;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class ServiceAccessibilityNope extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onServiceConnected() {
        Log.d("ServiceAccessibilityNope", "onServiceConnected");
        startService(new Intent(this, ServiceMediaSession.class));
    }
}
