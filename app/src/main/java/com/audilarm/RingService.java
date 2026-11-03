package com.audilarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

public class RingService extends Service {
    MediaPlayer mp;
    Handler h = new Handler(Looper.getMainLooper());
    float vol = 0.15f;

    Runnable ramp = new Runnable() {
        @Override
        public void run() {
            if (mp == null) return;
            vol = Math.min(1f, vol + 0.05f);
            mp.setVolume(vol, vol);
            if (vol < 1f) h.postDelayed(this, 1500);
        }
    };

    Runnable auto = new Runnable() {
        @Override
        public void run() {
            stopSelf();
        }
    };

    static void start(Context c, String title) {
        Intent i = new Intent(c, RingService.class);
        i.putExtra("title", title);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        String act = i == null ? null : i.getAction();
        if ("STOP".equals(act)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if ("SNOOZE".equals(act)) {
            Scheduler.snooze(this);
            stopSelf();
            return START_NOT_STICKY;
        }
        String title = i == null ? null : i.getStringExtra("title");
        if (title == null) title = "Alarma";

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("ring", "Alarmas",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
        Intent ra = new Intent(this, RingActivity.class);
        ra.putExtra("title", title);
        ra.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent full = PendingIntent.getActivity(this, 10, ra,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent si = new Intent(this, RingService.class);
        si.setAction("STOP");
        PendingIntent stop = PendingIntent.getService(this, 11, si,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "ring")
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText("Sonando por el canal de medios")
                .setContentIntent(full)
                .setFullScreenIntent(full, true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .addAction(android.R.drawable.ic_media_pause, "Detener", stop);
        startForeground(1, b.build());

        startPlayer();
        h.removeCallbacks(auto);
        h.postDelayed(auto, 10 * 60 * 1000L);
        return START_NOT_STICKY;
    }

    void startPlayer() {
        stopPlayer();
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC,
                Math.max(1, max * Store.volume(this) / 100), 0);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        String s = Store.sound(this);
        if (!s.isEmpty()) mp = play(Uri.parse(s));
        if (mp == null) mp = play(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        if (mp == null) mp = play(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        vol = 0.15f;
        h.postDelayed(ramp, 1500);
    }

    MediaPlayer play(Uri u) {
        try {
            MediaPlayer m = new MediaPlayer();
            // Aquí está la clave: USAGE_MEDIA en vez de USAGE_ALARM
            m.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            m.setDataSource(this, u);
            m.setLooping(true);
            m.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            m.prepare();
            m.setVolume(0.15f, 0.15f);
            m.start();
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    void stopPlayer() {
        h.removeCallbacks(ramp);
        if (mp != null) {
            try {
                mp.stop();
            } catch (Exception e) {
            }
            mp.release();
            mp = null;
        }
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.abandonAudioFocus(null);
    }

    @Override
    public void onDestroy() {
        stopPlayer();
        h.removeCallbacksAndMessages(null);
        Intent i = new Intent("com.audilarm.STOPPED");
        i.setPackage(getPackageName());
        sendBroadcast(i);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
