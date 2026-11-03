package com.audilarm;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class Store {
    static class Alarm {
        int id, hour, minute, days; // days: bits 0=lunes ... 6=domingo, 0 = una vez
        boolean enabled;
    }

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences("audilarm", Context.MODE_PRIVATE);
    }

    // Alarmas
    static List<Alarm> alarms(Context c) {
        List<Alarm> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p(c).getString("alarms", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Alarm x = new Alarm();
                x.id = o.getInt("id");
                x.hour = o.getInt("h");
                x.minute = o.getInt("m");
                x.days = o.getInt("d");
                x.enabled = o.getBoolean("e");
                l.add(x);
            }
        } catch (Exception e) {
        }
        return l;
    }

    static void saveAlarms(Context c, List<Alarm> l) {
        JSONArray a = new JSONArray();
        try {
            for (Alarm x : l) {
                JSONObject o = new JSONObject();
                o.put("id", x.id);
                o.put("h", x.hour);
                o.put("m", x.minute);
                o.put("d", x.days);
                o.put("e", x.enabled);
                a.put(o);
            }
        } catch (Exception e) {
        }
        p(c).edit().putString("alarms", a.toString()).apply();
    }

    static int newId(Context c) {
        int n = p(c).getInt("nextId", 100);
        p(c).edit().putInt("nextId", n + 1).apply();
        return n;
    }

    static Alarm find(Context c, int id) {
        for (Alarm a : alarms(c)) if (a.id == id) return a;
        return null;
    }

    static void update(Context c, Alarm x) {
        List<Alarm> l = alarms(c);
        boolean found = false;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id == x.id) {
                l.set(i, x);
                found = true;
            }
        }
        if (!found) l.add(x);
        saveAlarms(c, l);
    }

    static void delete(Context c, int id) {
        List<Alarm> l = alarms(c);
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id == id) {
                l.remove(i);
                break;
            }
        }
        saveAlarms(c, l);
    }

    // Sonido y volumen
    static int volume(Context c) {
        return p(c).getInt("volume", 50);
    }

    static void setVolume(Context c, int v) {
        p(c).edit().putInt("volume", v).apply();
    }

    static String sound(Context c) {
        return p(c).getString("sound", "");
    }

    static void setSound(Context c, String s) {
        p(c).edit().putString("sound", s).apply();
    }

    // Temporizador
    static long timerEnd(Context c) {
        return p(c).getLong("timerEnd", 0);
    }

    static void setTimerEnd(Context c, long t) {
        p(c).edit().putLong("timerEnd", t).apply();
    }

    // Cronómetro
    static long swAcc(Context c) {
        return p(c).getLong("swAcc", 0);
    }

    static long swStart(Context c) {
        return p(c).getLong("swStart", 0);
    }

    static void setSw(Context c, long acc, long start) {
        p(c).edit().putLong("swAcc", acc).putLong("swStart", start).apply();
    }
}
