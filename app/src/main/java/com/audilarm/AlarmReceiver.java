package com.audilarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String kind = i.getStringExtra("kind");
        int id = i.getIntExtra("id", 0);
        String title = "Alarma";
        if ("alarm".equals(kind)) {
            Store.Alarm a = Store.find(c, id);
            if (a == null || !a.enabled) return;
            title = String.format(Locale.US, "Alarma %02d:%02d", a.hour, a.minute);
            if (a.days == 0) {
                a.enabled = false;
                Store.update(c, a);
            } else {
                Scheduler.alarm(c, a);
            }
        } else if ("timer".equals(kind)) {
            title = "Temporizador terminado";
            Store.setTimerEnd(c, 0);
        } else {
            title = "Alarma pospuesta";
        }
        RingService.start(c, title);
    }
}
