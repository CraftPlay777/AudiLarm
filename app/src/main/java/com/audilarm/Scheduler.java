package com.audilarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

class Scheduler {
    static PendingIntent pi(Context c, int code, String kind, int id) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.putExtra("kind", kind);
        i.putExtra("id", id);
        return PendingIntent.getBroadcast(c, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void set(Context c, long when, PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent show = PendingIntent.getActivity(c, 0,
                new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show), pi);
    }

    static void cancel(Context c, int code, String kind, int id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi(c, code, kind, id));
    }

    // Próxima hora en que debe sonar la alarma, -1 si no hay
    static long nextTime(Store.Alarm a) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            Calendar t = Calendar.getInstance();
            t.add(Calendar.DAY_OF_YEAR, i);
            t.set(Calendar.HOUR_OF_DAY, a.hour);
            t.set(Calendar.MINUTE, a.minute);
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);
            if (t.getTimeInMillis() <= now) continue;
            int bit = (t.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            if (a.days == 0 || (a.days & (1 << bit)) != 0) return t.getTimeInMillis();
        }
        return -1;
    }

    static void alarm(Context c, Store.Alarm a) {
        if (!a.enabled) {
            cancel(c, a.id, "alarm", a.id);
            return;
        }
        long t = nextTime(a);
        if (t > 0) set(c, t, pi(c, a.id, "alarm", a.id));
    }

    static void timer(Context c, long end) {
        set(c, end, pi(c, 1, "timer", 1));
    }

    static void cancelTimer(Context c) {
        cancel(c, 1, "timer", 1);
    }

    static void snooze(Context c) {
        set(c, System.currentTimeMillis() + 10 * 60 * 1000L, pi(c, 2, "snooze", 2));
    }

    static void rescheduleAll(Context c) {
        for (Store.Alarm a : Store.alarms(c)) alarm(c, a);
        long end = Store.timerEnd(c);
        if (end > System.currentTimeMillis()) timer(c, end);
        else Store.setTimerEnd(c, 0);
    }
}
