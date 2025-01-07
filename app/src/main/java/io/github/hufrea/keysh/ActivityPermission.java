package io.github.hufrea.keysh;

import android.app.ActivityManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityPermission extends AppCompatActivity {
    private boolean accessibilityEnabled() {
        try {
            int enabled = Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled == 0) {
                return false;
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.e(getClass().getSimpleName(), e.toString());
            return false;
        }
        String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        for (String pkg : settingValue.split(":")) {
            if (pkg.split("/")[0].equals(getPackageName())) {
                return true;
            }
        }
        return false;
    }


    private void permissionFromName(String name) {
        switch (name) {
            case "SYSTEM_ALERT_WINDOW":
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));

                    startActivity(intent);
                    Toast.makeText(this, R.string.window_perm_req, Toast.LENGTH_LONG).show();
                }
                break;

            case "ACCESSIBILITY":
                if (!accessibilityEnabled()) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                    Toast.makeText(this, R.string.window_perm_req, Toast.LENGTH_LONG).show();
                }
                break;

            case "TERMUX_RUN_COMMAND":
                requestPermissions(new String[]{"com.termux.permission.RUN_COMMAND"}, 1);
                break;

            case "STORAGE":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                    break;
                }
                requestPermissions(new String[]{
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 1);
                break;

            case "BACKGROUND_ACTIVITY":
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    break;
                }
                ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                if (am.isLowRamDevice()) {
                    permissionFromName("ACCESSIBILITY");
                }
                else if (!accessibilityEnabled()) {
                    permissionFromName("SYSTEM_ALERT_WINDOW");
                }
                break;
        }
    }


    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        Bundle bundle = getIntent().getExtras();
        String data = bundle == null ? null : bundle.getString(getPackageName() + ".PERMISSION");
        if (data != null) {
            permissionFromName(data);
        }
        finish();
    }
}