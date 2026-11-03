package com.audilarm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

class TimerTab extends TabView {
    Context ctx;
    NumberPicker ph, pm, ps;
    LinearLayout pickers;
    TextView big;
    Button go;
    Handler h = new Handler(Looper.getMainLooper());

    Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
            h.postDelayed(this, 200);
        }
    };

    TimerTab(Context c) {
        super(c);
        ctx = c;
        col.addView(Ui.text(c, "Temporizador", 26, true));

        big = Ui.text(c, "", 48, true);
        big.setGravity(Gravity.CENTER);
        col.addView(big);

        pickers = new LinearLayout(c);
        pickers.setGravity(Gravity.CENTER);
        ph = picker(23);
        pm = picker(59);
        ps = picker(59);
        pm.setValue(5);
        pickers.addView(wrap(ph, "h"));
        pickers.addView(wrap(pm, "min"));
        pickers.addView(wrap(ps, "s"));
        col.addView(pickers);

        go = Ui.button(c, "Iniciar", v -> toggle());
        col.addView(go);
    }

    NumberPicker picker(int max) {
        NumberPicker p = new NumberPicker(ctx);
        p.setMinValue(0);
        p.setMaxValue(max);
        return p;
    }

    LinearLayout wrap(NumberPicker p, String label) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(Ui.dp(ctx, 12), 0, Ui.dp(ctx, 12), 0);
        l.addView(p);
        TextView t = Ui.text(ctx, label, 14, false);
        t.setGravity(Gravity.CENTER);
        l.addView(t);
        return l;
    }

    void toggle() {
        if (Store.timerEnd(ctx) > System.currentTimeMillis()) {
            Scheduler.cancelTimer(ctx);
            Store.setTimerEnd(ctx, 0);
        } else {
            long dur = ((ph.getValue() * 60L + pm.getValue()) * 60L + ps.getValue()) * 1000L;
            if (dur == 0) {
                Toast.makeText(ctx, "Elige un tiempo", Toast.LENGTH_SHORT).show();
                return;
            }
            long end = System.currentTimeMillis() + dur;
            Store.setTimerEnd(ctx, end);
            Scheduler.timer(ctx, end);
        }
        update();
    }

    void update() {
        long left = Store.timerEnd(ctx) - System.currentTimeMillis();
        if (left > 0) {
            long s = (left + 999) / 1000;
            big.setText(String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
            pickers.setVisibility(GONE);
            go.setText("Cancelar");
        } else {
            big.setText("");
            pickers.setVisibility(VISIBLE);
            go.setText("Iniciar");
        }
    }

    @Override
    void start() {
        h.removeCallbacks(tick);
        h.post(tick);
    }

    @Override
    void stop() {
        h.removeCallbacks(tick);
    }
}
