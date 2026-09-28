package com.worldradio.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class Favorites {
    private static final String PREF = "favorites", KEY = "list";

    private static SharedPreferences p(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }

    public static List<Station> all(Context c) {
        List<Station> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(Station.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    private static java.util.Set<String> ids;

    public static synchronized boolean has(Context c, String uuid) {
        if (ids == null) { ids = new java.util.HashSet<>(); for (Station s : all(c)) ids.add(s.uuid); }
        return ids.contains(uuid);
    }

    /** Toggles; returns the new state. */
    public static synchronized boolean toggle(Context c, Station st) {
        List<Station> list = all(c);
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--)
            if (list.get(i).uuid.equals(st.uuid)) { list.remove(i); removed = true; }
        if (!removed) list.add(0, st);
        JSONArray a = new JSONArray();
        for (Station s : list) a.put(s.toJson());
        p(c).edit().putString(KEY, a.toString()).apply();
        ids = null;
        return !removed;
    }

    public static void saveLast(Context c, Station s) {
        p(c).edit().putString("last", s.toJson().toString()).apply();
    }
}
