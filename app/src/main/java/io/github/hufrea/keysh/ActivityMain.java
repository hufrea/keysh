package io.github.hufrea.keysh;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import io.github.hufrea.keysh.R;
import io.github.hufrea.keysh.databinding.ActivityMainBinding;


public class ActivityMain extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    public HandlerButton buttonHandler = null;

    static public class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            if (sp.getBoolean("autostart", false)) {
                context.startService(new Intent(context, ServiceMediaSession.class));
            }
        }
    }

    public void restartBH() {
        this.buttonHandler.deinit();
        this.buttonHandler = new HandlerButton(this, "MainActivity");

        stopService(new Intent(this, ServiceMediaSession.class));
        startService(new Intent(this, ServiceMediaSession.class));

        Intent intent = new Intent(getPackageName() + ".RESTART");
        sendBroadcast(intent);
    }

    public void stopBH() {
        stopService(new Intent(this, ServiceMediaSession.class));

        Intent intent = new Intent(getPackageName() + ".STOP");
        sendBroadcast(intent);

        finishAndRemoveTask();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        Intent intent = new Intent(this, ServiceMediaSession.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        buttonHandler = new HandlerButton(this, "MainActivity");
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyUp(keyCode, event);
        }
        buttonHandler.onButtonRelease();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int code;
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                code = 1;
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                code = -1;
                break;
            default:
                return super.onKeyDown(keyCode, event);
        }
        buttonHandler.onButtonPress(code);
        return true;
    }


    @Override
    public void onDestroy() {
        Log.d(ActivityMain.class.getSimpleName(), "onDestroy");
        super.onDestroy();
        buttonHandler.deinit();
    }
}