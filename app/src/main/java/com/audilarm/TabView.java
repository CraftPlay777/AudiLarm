package com.audilarm;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;

abstract class TabView extends ScrollView {
    LinearLayout col;

    TabView(Context c) {
        super(c);
        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(c, 16);
        col.setPadding(p, p, p, p);
        addView(col);
    }

    void start() {}

    void stop() {}
}
