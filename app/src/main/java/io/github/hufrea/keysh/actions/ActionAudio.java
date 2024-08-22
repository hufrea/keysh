package io.github.hufrea.keysh.actions;

import android.media.AudioManager;
import android.util.SparseArray;
import android.view.KeyEvent;

public class ActionAudio {

    public static void pressMediaButton(AudioManager am, int code) {
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    public static void mediaEvent(AudioManager am, String[] args) {
        if (args.length < 2) {
            return;
        }
        switch (args[1]) {
            case "play":
                pressMediaButton(am, KeyEvent.KEYCODE_MEDIA_PLAY);
                break;
            case "pause":
                pressMediaButton(am, KeyEvent.KEYCODE_MEDIA_PAUSE);
                break;
            case "play_pause":
                pressMediaButton(am, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                break;
            case "next":
                pressMediaButton(am, KeyEvent.KEYCODE_MEDIA_NEXT);
                break;
            case "previous":
                pressMediaButton(am, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;
            case "rewind":
                pressMediaButton(am, KeyEvent.KEYCODE_MEDIA_REWIND);
                break;
            case "forward":
                pressMediaButton(am, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
                break;
            case "stop":
                pressMediaButton(am, KeyEvent.KEYCODE_MEDIA_STOP);
                break;
        }
    }

    public static void volumeSet(AudioManager am, String[] args) {
        if (args.length < 3) {
            return;
        }
        int key;
        switch (args[1]) {
            case "music":
                key = AudioManager.STREAM_MUSIC;
                break;
            case "notification":
                key = AudioManager.STREAM_NOTIFICATION;
                break;
            case "ring":
                key = AudioManager.STREAM_RING;
                break;
            case "alarm":
                key = AudioManager.STREAM_ALARM;
                break;
            case "call":
                key = AudioManager.STREAM_VOICE_CALL;
                break;
            case "current":
                switch (am.getMode()) {
                    case AudioManager.MODE_NORMAL:
                        key = AudioManager.STREAM_MUSIC;
                        break;
                    case AudioManager.MODE_RINGTONE:
                        key = AudioManager.STREAM_RING;
                        break;
                    case AudioManager.MODE_IN_CALL:
                    default:
                        key = AudioManager.STREAM_VOICE_CALL;
                        break;
                }
                break;
            default:
                return;
        }
        int current = am.getStreamVolume(key);
        switch (args[2]) {
            case "up":
                current += 1;
                break;
            case "down":
                current -= 1;
                break;
            default:
                try {
                    current = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    return;
                }
        }
        am.setStreamVolume(key, current, AudioManager.FLAG_SHOW_UI);
    }
}
