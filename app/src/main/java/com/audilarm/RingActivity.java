package com.audilarm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RingActivity extends Activity {
    BroadcastReceiver stopped = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        String title = getIntent().getStringExtra("title");
        if (title == null) title = "Alarma";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 24);
        root.setPadding(p, p, p, p);

        TextView t = Ui.text(this, title, 30, true);
        t.setGravity(Gravity.CENTER);
        root.addView(t);

        root.addView(Ui.button(this, "Detener", v -> {
            startService(new Intent(this, RingService.class).setAction("STOP"));
            finish();
        }), new LinearLayout.LayoutParams(-1, Ui.dp(this, 90)));

        root.addView(Ui.button(this, "Posponer 10 min", v -> {
            startService(new Intent(this, RingService.class).setAction("SNOOZE"));
            finish();
        }), new LinearLayout.LayoutParams(-1, Ui.dp(this, 70)));

        setContentView(root);
        registerReceiver(stopped, new IntentFilter("com.audilarm.STOPPED"));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(stopped);
        super.onDestroy();
    }
}
