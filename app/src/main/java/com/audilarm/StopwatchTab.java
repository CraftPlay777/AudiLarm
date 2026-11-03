package com.audilarm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

class StopwatchTab extends TabView {
    Context ctx;
    TextView big, laps;
    Button go;
    int lapCount = 0;
    Handler h = new Handler(Looper.getMainLooper());

    Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
            h.postDelayed(this, 50);
        }
    };

    StopwatchTab(Context c) {
        super(c);
        ctx = c;
        col.addView(Ui.text(c, "Cronómetro", 26, true));

        big = Ui.text(c, "", 44, true);
        big.setGravity(Gravity.CENTER);
        col.addView(big);

        go = Ui.button(c, "Iniciar", v -> toggle());
        col.addView(go);
        col.addView(Ui.button(c, "Vuelta", v -> lap()));
        col.addView(Ui.button(c, "Reiniciar", v -> reset()));

        laps = Ui.text(c, "", 18, false);
        col.addView(laps);
    }

    long elapsed() {
        long start = Store.swStart(ctx);
        long run = start != 0 ? Math.max(0, SystemClock.elapsedRealtime() - start) : 0;
        return Store.swAcc(ctx) + run;
    }

    String fmt(long ms) {
        return String.format(Locale.US, "%02d:%02d:%02d.%02d",
                ms / 3600000, (ms / 60000) % 60, (ms / 1000) % 60, (ms / 10) % 100);
    }

    void toggle() {
        long start = Store.swStart(ctx);
        if (start == 0) {
            Store.setSw(ctx, Store.swAcc(ctx), SystemClock.elapsedRealtime());
        } else {
            Store.setSw(ctx, elapsed(), 0);
        }
        update();
    }

    void lap() {
        if (Store.swStart(ctx) == 0) return;
        lapCount++;
        laps.setText("Vuelta " + lapCount + "   " + fmt(elapsed()) + "\n" + laps.getText());
    }

    void reset() {
        Store.setSw(ctx, 0, 0);
        lapCount = 0;
        laps.setText("");
        update();
    }

    void update() {
        big.setText(fmt(elapsed()));
        go.setText(Store.swStart(ctx) == 0 ? "Iniciar" : "Pausar");
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
