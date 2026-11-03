package com.audilarm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

class WorldTab extends TabView {
    Context ctx;
    int level = 0; // 0 países, 1 provincias, 2 reloj
    String ccode = "", cname = "";
    String[] place;
    boolean silent = false;

    TextView title, timeTv, dateTv, infoTv;
    EditText search;
    Button backBtn;
    LinearLayout list, clockBox;
    Handler h = new Handler(Looper.getMainLooper());

    Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            h.postDelayed(this, 500);
        }
    };

    WorldTab(Context c) {
        super(c);
        ctx = c;
        World.load(c);

        title = Ui.text(c, "Reloj mundial", 26, true);
        col.addView(title);
        backBtn = Ui.button(c, "← Volver", v -> back());
        col.addView(backBtn);

        search = new EditText(c);
        search.setSingleLine(true);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int d) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                if (!silent) render();
            }
        });
        col.addView(search);

        list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list);

        clockBox = new LinearLayout(c);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.CENTER);
        timeTv = Ui.text(c, "", 52, true);
        timeTv.setGravity(Gravity.CENTER);
        dateTv = Ui.text(c, "", 18, false);
        dateTv.setGravity(Gravity.CENTER);
        infoTv = Ui.text(c, "", 14, false);
        infoTv.setGravity(Gravity.CENTER);
        clockBox.addView(timeTv);
        clockBox.addView(dateTv);
        clockBox.addView(infoTv);
        col.addView(clockBox);

        render();
    }

    static String norm(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    void clearSearch() {
        silent = true;
        search.setText("");
        silent = false;
    }

    boolean back() {
        if (level == 0) return false;
        if (level == 2 && World.placesFor(ccode).size() > 1) level = 1;
        else level = 0;
        clearSearch();
        render();
        return true;
    }

    void render() {
        list.removeAllViews();
        boolean clock = level == 2;
        backBtn.setVisibility(level == 0 ? GONE : VISIBLE);
        search.setVisibility(clock ? GONE : VISIBLE);
        clockBox.setVisibility(clock ? VISIBLE : GONE);

        if (level == 0) {
            title.setText("Reloj mundial");
            search.setHint("Buscar país");
            String q = norm(search.getText().toString());
            for (World.Country k : World.countries) {
                if (!norm(k.name).contains(q)) continue;
                final World.Country kk = k;
                list.addView(Ui.button(ctx, k.name, v -> {
                    ccode = kk.code;
                    cname = kk.name;
                    List<String[]> p = World.placesFor(ccode);
                    if (p.size() == 1) {
                        place = p.get(0);
                        level = 2;
                    } else {
                        level = 1;
                    }
                    clearSearch();
                    render();
                }));
            }
        } else if (level == 1) {
            title.setText(cname);
            search.setHint("Buscar provincia o estado");
            String q = norm(search.getText().toString());
            for (String[] p : World.placesFor(ccode)) {
                if (!norm(p[0]).contains(q)) continue;
                final String[] pp = p;
                list.addView(Ui.button(ctx, p[0], v -> {
                    place = pp;
                    level = 2;
                    render();
                }));
            }
        } else {
            title.setText(place[0] + ", " + cname);
            updateClock();
        }
    }

    void updateClock() {
        if (level != 2) return;
        TimeZone tz = TimeZone.getTimeZone(place[1]);
        Date now = new Date();
        SimpleDateFormat t = new SimpleDateFormat("HH:mm:ss", Locale.US);
        t.setTimeZone(tz);
        SimpleDateFormat d = new SimpleDateFormat("EEEE d 'de' MMMM yyyy", new Locale("es"));
        d.setTimeZone(tz);
        int off = tz.getOffset(now.getTime());
        int diff = (off - TimeZone.getDefault().getOffset(now.getTime())) / 60000;
        int ad = Math.abs(diff);
        String dif = diff == 0 ? "Misma hora que tú"
                : (diff > 0 ? "+" : "-") + (ad / 60) + " h" + (ad % 60 != 0 ? " " + (ad % 60) + " min" : "")
                + " respecto a tu hora";
        String utc = String.format(Locale.US, "UTC%s%02d:%02d", off < 0 ? "-" : "+",
                Math.abs(off) / 3600000, (Math.abs(off) / 60000) % 60);
        timeTv.setText(t.format(now));
        dateTv.setText(d.format(now));
        infoTv.setText(utc + "  ·  " + dif + "\n" + place[1]);
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
