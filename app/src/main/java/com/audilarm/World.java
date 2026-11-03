package com.audilarm;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class World {
    static class Country {
        String code, name;
    }

    static List<Country> countries;
    static Map<String, List<String[]>> places;

    static void load(Context c) {
        if (countries != null) return;
        places = new HashMap<>();
        try {
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(c.getAssets().open("world.txt"), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length < 3) continue;
                List<String[]> l = places.get(p[0]);
                if (l == null) {
                    l = new ArrayList<>();
                    places.put(p[0], l);
                }
                l.add(new String[]{p[1], p[2]});
            }
            r.close();
        } catch (Exception e) {
        }

        countries = new ArrayList<>();
        Locale es = new Locale("es");
        for (String code : Locale.getISOCountries()) {
            if (placesFor(code).isEmpty()) continue;
            Country k = new Country();
            k.code = code;
            k.name = new Locale("", code).getDisplayCountry(es);
            countries.add(k);
        }
        final Collator col = Collator.getInstance(es);
        Collections.sort(countries, (a, b) -> col.compare(a.name, b.name));
    }

    // Si el país no está en world.txt, usa las zonas horarias que trae Android
    static List<String[]> placesFor(String code) {
        List<String[]> l = places.get(code);
        if (l != null) return l;
        l = new ArrayList<>();
        Set<String> ids = android.icu.util.TimeZone.getAvailableIDs(
                android.icu.util.TimeZone.SystemTimeZoneType.CANONICAL_LOCATION, code, null);
        for (String id : ids) {
            String n = id.substring(id.lastIndexOf('/') + 1).replace('_', ' ');
            l.add(new String[]{n, id});
        }
        final Collator col = Collator.getInstance(new Locale("es"));
        Collections.sort(l, (a, b) -> col.compare(a[0], b[0]));
        places.put(code, l);
        return l;
    }
}
