package io.github.hufrea.keysh;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.media.AudioPlaybackConfiguration;
import android.media.MediaRouter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.List;

import io.github.hufrea.keysh.R;
import io.github.hufrea.keysh.actions.ActionAudio;


public class ServiceMediaSession extends Service {
    private static final String TAG = ServiceMediaSession.class.getSimpleName();

    private MediaSession mediaSession = null;
    private BroadcastReceiver receiver;
    private ActionAudio volumeControl;
    private HandlerButton buttonHandler = null;
    private PowerManager.WakeLock wl;
    private AudioManager am;

    private final AudioManager.AudioPlaybackCallback audioPlaybackCallback =
            new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    super.onPlaybackConfigChanged(configs);
                    if (configs.isEmpty()) {
                        return;
                    }
                    Log.d(TAG, "onPlaybackConfigChanged: " + configs.get(0).getAudioAttributes().toString());
                    mediaSessionToTop();
                }
            };

    private final MediaRouter.SimpleCallback mCallback = new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteSelected");
            volumeControl.updateVolumeLevels();
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            int current_volume = info.getVolume();
            int type = info.getPlaybackStream();
            int fixed_volume = volumeControl.getFixedVolume(type);

            if (fixed_volume == -1
                    || fixed_volume == current_volume) {
                Log.d(TAG, "onRouteVolumeChanged ignore: " + current_volume + " : " + fixed_volume);
                return;
            }
            mediaSessionToTop();
            am.setStreamVolume(type, fixed_volume, 0);
            Log.d(TAG, "onRouteVolumeChanged: " + current_volume + " : " + fixed_volume);
            buttonHandler.onButtonPress(current_volume < fixed_volume ? -1 : 1);
        }
    };


    @Override
    public void onTaskRemoved(Intent intent) {
        Log.d(TAG, "onTaskRemoved");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        PackageManager pkgManager = getPackageManager();
        Intent intent = pkgManager.getLaunchIntentForPackage(getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_IMMUTABLE);

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "0";
            String channelName = "Background Service";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN);
            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setSubText("Active")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        setup();

        // restore after lmk
        startForegroundService(new Intent(this, ServiceMediaSession.class));
    }


    private void initMediaSession() {
        this.mediaSession = new MediaSession(this, "ServiceMediaSession");
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1)
                .build());

        final VolumeProvider buttonProvider =
                new VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE,100, 50) {
                    @Override
                    public void onAdjustVolume(int direction) {
                        Log.d(TAG, "onAdjustVolume " + direction);

                        if (direction != 0) {
                            wl.acquire(60 * 1000L);
                            buttonHandler.onButtonPress(direction);
                        } else {
                            buttonHandler.onButtonRelease();
                            if (wl.isHeld()) {
                                wl.release();
                            }
                        }
                    }
                };
        mediaSession.setPlaybackToRemote(buttonProvider);
        mediaSession.setCallback(new MediaSession.Callback() {});
        mediaSession.setActive(false);
    }


    private void initReceiver() {
        MediaRouter mediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    Log.e(TAG, "intent.getAction() is null");
                    return;
                } else {
                    Log.d(TAG, action);
                }
                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    mediaSession.setActive(true);
                    mediaSessionToTop();
                    volumeControl.updateVolumeLevels();

                    mediaRouter.addCallback(MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS,
                            mCallback, MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
                    am.registerAudioPlaybackCallback(audioPlaybackCallback, null);
                } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    mediaSession.setActive(false);

                    mediaRouter.removeCallback(mCallback);
                    am.unregisterAudioPlaybackCallback(audioPlaybackCallback);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);

        registerReceiver(this.receiver, intentFilter);
    }


    private void setup() {
        this.am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        this.volumeControl = new ActionAudio(am);
        this.buttonHandler = new HandlerButton(this, "MediaService", volumeControl);

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        this.wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":lock");

        initMediaSession();
        initReceiver();
    }


    private void mediaSessionToTop() {
        PlaybackState.Builder playbackState = new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1);
        mediaSession.setPlaybackState(playbackState.build());

        playbackState.setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1);
        mediaSession.setPlaybackState(playbackState.build());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand");

        Bundle bundle = intent == null ? null : intent.getExtras();
        if (bundle == null) {
            return START_STICKY;
        }
        Object object = bundle.get(getPackageName() + ".DATA");
        if (object == null) {
            return START_STICKY;
        }
        Log.d(TAG, "restart buttonHandler");
        String data = object.toString();

        if (buttonHandler != null) {
            buttonHandler.deinit();
        }
        this.buttonHandler = new HandlerButton(this, data, volumeControl);
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mediaSession != null) {
            mediaSession.release();
        }
        if (buttonHandler != null) {
            buttonHandler.deinit();
        }
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }
}