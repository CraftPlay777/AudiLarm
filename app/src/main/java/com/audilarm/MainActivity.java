package com.audilarm;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    FrameLayout box;
    TabView[] tabs;
    Button[] btns;
    int cur = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        box = new FrameLayout(this);
        root.addView(box, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        String[] names = {"Alarma", "Timer", "Cronómetro", "Mundo"};
        btns = new Button[4];
        for (int i = 0; i < 4; i++) {
            final int n = i;
            btns[i] = Ui.button(this, names[i], v -> show(n));
            bar.addView(btns[i], new LinearLayout.LayoutParams(0, -2, 1f));
        }
        root.addView(bar);
        setContentView(root);

        tabs = new TabView[]{new AlarmTab(this), new TimerTab(this),
                new StopwatchTab(this), new WorldTab(this)};
        show(0);
    }

    void show(int i) {
        tabs[cur].stop();
        box.removeAllViews();
        cur = i;
        box.addView(tabs[i]);
        tabs[i].start();
        for (int k = 0; k < 4; k++) btns[k].setEnabled(k != cur);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tabs[cur].start();
    }

    @Override
    protected void onPause() {
        tabs[cur].stop();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (cur == 3 && ((WorldTab) tabs[3]).back()) return;
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc == 7 && res == RESULT_OK && d != null) {
            Uri u = d.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            Store.setSound(this, u == null ? "" : u.toString());
            ((AlarmTab) tabs[0]).refresh();
        }
    }
}
