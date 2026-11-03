package com.audilarm;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

class AlarmTab extends TabView {
    static final String[] CORTO = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    static final String[] LARGO = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    MainActivity act;
    LinearLayout list;
    TextView soundName;

    AlarmTab(MainActivity a) {
        super(a);
        act = a;
        col.addView(Ui.text(a, "Alarmas", 26, true));
        col.addView(Ui.button(a, "+ Nueva alarma", v -> add()));
        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list);

        col.addView(Ui.text(a, "Sonido (canal de medios)", 18, true));
        soundName = Ui.text(a, "", 14, false);
        col.addView(soundName);
        col.addView(Ui.button(a, "Elegir sonido", v -> pick()));
        col.addView(Ui.text(a, "Volumen de medios al sonar", 14, false));

        SeekBar sb = new SeekBar(a);
        sb.setMax(100);
        sb.setProgress(Store.volume(a));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                Store.setVolume(act, p);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        col.addView(sb);
        col.addView(Ui.button(a, "Probar sonido", v -> RingService.start(act, "Prueba")));
        refresh();
    }

    void refresh() {
        list.removeAllViews();
        List<Store.Alarm> all = Store.alarms(act);
        Collections.sort(all, (x, y) -> (x.hour * 60 + x.minute) - (y.hour * 60 + y.minute));
        for (Store.Alarm a : all) list.addView(row(a));

        String s = Store.sound(act);
        String name = "alarma predeterminada";
        if (!s.isEmpty()) {
            try {
                name = RingtoneManager.getRingtone(act, Uri.parse(s)).getTitle(act);
            } catch (Exception e) {
                name = "personalizado";
            }
        }
        soundName.setText("Sonido: " + name);
    }

    LinearLayout row(final Store.Alarm a) {
        LinearLayout r = new LinearLayout(act);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, Ui.dp(act, 8), 0, Ui.dp(act, 8));

        LinearLayout info = new LinearLayout(act);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView t = Ui.text(act, String.format(Locale.US, "%02d:%02d", a.hour, a.minute), 34, true);
        t.setOnClickListener(v -> editTime(a));
        TextView d = Ui.text(act, daysText(a.days), 14, false);
        d.setOnClickListener(v -> editDays(a));
        info.addView(t);
        info.addView(d);
        r.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(act);
        sw.setChecked(a.enabled);
        sw.setOnCheckedChangeListener((b, on) -> {
            a.enabled = on;
            Store.update(act, a);
            Scheduler.alarm(act, a);
            if (on) toast(a);
        });
        r.addView(sw);

        Button x = Ui.button(act, "✕", v -> {
            a.enabled = false;
            Scheduler.alarm(act, a);
            Store.delete(act, a.id);
            refresh();
        });
        r.addView(x);
        return r;
    }

    String daysText(int days) {
        if (days == 0) return "Una vez (toca para repetir)";
        if (days == 127) return "Todos los días";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((days & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(CORTO[i]);
            }
        }
        return sb.toString();
    }

    void toast(Store.Alarm a) {
        long min = (Scheduler.nextTime(a) - System.currentTimeMillis()) / 60000;
        Toast.makeText(act, "Sonará en " + (min / 60) + " h " + (min % 60) + " min",
                Toast.LENGTH_SHORT).show();
    }

    void add() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(act, (view, h, m) -> {
            Store.Alarm a = new Store.Alarm();
            a.id = Store.newId(act);
            a.hour = h;
            a.minute = m;
            a.days = 0;
            a.enabled = true;
            Store.update(act, a);
            Scheduler.alarm(act, a);
            refresh();
            toast(a);
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    void editTime(final Store.Alarm a) {
        new TimePickerDialog(act, (view, h, m) -> {
            a.hour = h;
            a.minute = m;
            a.enabled = true;
            Store.update(act, a);
            Scheduler.alarm(act, a);
            refresh();
            toast(a);
        }, a.hour, a.minute, true).show();
    }

    void editDays(final Store.Alarm a) {
        final boolean[] ch = new boolean[7];
        for (int i = 0; i < 7; i++) ch[i] = (a.days & (1 << i)) != 0;
        new AlertDialog.Builder(act)
                .setTitle("Repetir")
                .setMultiChoiceItems(LARGO, ch, (d, w, c) -> ch[w] = c)
                .setPositiveButton("Listo", (d, w) -> {
                    int m = 0;
                    for (int i = 0; i < 7; i++) if (ch[i]) m |= 1 << i;
                    a.days = m;
                    Store.update(act, a);
                    Scheduler.alarm(act, a);
                    refresh();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    void pick() {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        String s = Store.sound(act);
        if (!s.isEmpty()) i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(s));
        act.startActivityForResult(i, 7);
    }
}
