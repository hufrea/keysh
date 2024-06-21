package io.github.hufrea.keysh.actions;

import android.media.AudioManager;
import android.util.SparseArray;
import android.view.KeyEvent;

public class ActionAudio {
    final private AudioManager am;
    final private static SparseArray<Integer> volumeMap = new SparseArray<>();
    static {
        volumeMap.put(AudioManager.STREAM_MUSIC, 0);
        volumeMap.put(AudioManager.STREAM_NOTIFICATION, 0);
        volumeMap.put(AudioManager.STREAM_RING, 0);
        volumeMap.put(AudioManager.STREAM_ALARM, 0);
        volumeMap.put(AudioManager.STREAM_VOICE_CALL, 0);
    }

    public ActionAudio(AudioManager am) {
        this.am = am;
    }

    public int getFixedVolume(int type) {
        return volumeMap.get(type);
    }

    public void updateVolumeLevels() {
        for (int i = 0; i < volumeMap.size(); i++) {
            int key = volumeMap.keyAt(i);
            volumeMap.set(key, am.getStreamVolume(key));
        }
    }

    public void pressMediaButton(int code) {
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
    }

    public void mediaEvent(String[] args) {
        if (args.length < 2) {
            return;
        }
        switch (args[1]) {
            case "play":
                pressMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY);
                break;
            case "pause":
                pressMediaButton(KeyEvent.KEYCODE_MEDIA_PAUSE);
                break;
            case "play_pause":
                pressMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                break;
            case "next":
                pressMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT);
                break;
            case "previous":
                pressMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;
            case "rewind":
                pressMediaButton(KeyEvent.KEYCODE_MEDIA_REWIND);
                break;
            case "forward":
                pressMediaButton(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
                break;
            case "stop":
                pressMediaButton(KeyEvent.KEYCODE_MEDIA_STOP);
                break;
        }
    }

    public void volumeSet(String[] args) {
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
        volumeMap.set(key, current);
        am.setStreamVolume(key, current, AudioManager.FLAG_SHOW_UI);
    }
}
