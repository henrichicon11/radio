package com.worldradio.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Client for the free, community-driven Radio Browser directory (radio-browser.info). */
public class RadioApi {
    private static final List<String> FALLBACK = Arrays.asList(
            "de1.api.radio-browser.info", "de2.api.radio-browser.info", "fi1.api.radio-browser.info",
            "nl1.api.radio-browser.info", "at1.api.radio-browser.info");
    private static List<String> servers;
    private static final String UA = "WorldRadioAndroid/1.0";

    private static synchronized List<String> servers() {
        if (servers != null) return servers;
        List<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(fetchRaw("https://all.api.radio-browser.info/json/servers"));
            for (int i = 0; i < arr.length(); i++) {
                String n = arr.getJSONObject(i).optString("name");
                if (!n.isEmpty() && !list.contains(n)) list.add(n);
            }
        } catch (Exception ignored) {}
        if (list.isEmpty()) list.addAll(FALLBACK);
        Collections.shuffle(list);
        servers = list;
        return servers;
    }

    private static String fetchRaw(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", UA);
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new Exception("HTTP " + code);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
            r.close();
            return sb.toString();
        } finally { c.disconnect(); }
    }

    /** Tries each mirror (https, then http) until one answers. */
    private static String get(String path) throws Exception {
        Exception last = null;
        for (String s : servers()) {
            for (String scheme : new String[]{"https://", "http://"}) {
                try { return fetchRaw(scheme + s + path); } catch (Exception e) { last = e; }
            }
        }
        throw last != null ? last : new Exception("No server available");
    }

    public static List<Country> countries() throws Exception {
        JSONArray arr = new JSONArray(get("/json/countries?hidebroken=true&order=name"));
        // Merge duplicates by country code
        java.util.LinkedHashMap<String, Country> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String code = o.optString("iso_3166_1", "").toUpperCase(Locale.ROOT);
            String name = o.optString("name", "");
            int cnt = o.optInt("stationcount", 0);
            if (code.length() != 2 || cnt <= 0) continue;
            String display = new Locale("", code).getDisplayCountry(Locale.ENGLISH);
            if (display == null || display.isEmpty() || display.equals(code)) display = name;
            Country prev = map.get(code);
            map.put(code, new Country(display, code, cnt + (prev == null ? 0 : prev.count)));
        }
        List<Country> out = new ArrayList<>(map.values());
        Collections.sort(out, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    private static List<Station> parse(String json) throws Exception {
        JSONArray arr = new JSONArray(json);
        List<Station> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            Station s = Station.fromJson(arr.getJSONObject(i));
            if (!s.url.isEmpty()) out.add(s);
        }
        return out;
    }

    public static List<Station> byCountry(String code, String filter, int offset, int limit) throws Exception {
        String path = "/json/stations/search?countrycode=" + code
                + "&hidebroken=true&order=clickcount&reverse=true&offset=" + offset + "&limit=" + limit;
        if (filter != null && !filter.trim().isEmpty())
            path += "&name=" + URLEncoder.encode(filter.trim(), "UTF-8");
        return parse(get(path));
    }

    public static List<Station> search(String q, int offset, int limit) throws Exception {
        String path = "/json/stations/search?name=" + URLEncoder.encode(q.trim(), "UTF-8")
                + "&hidebroken=true&order=clickcount&reverse=true&offset=" + offset + "&limit=" + limit;
        return parse(get(path));
    }

    /** Counts a click for the station (helps community rankings). Fire-and-forget. */
    public static void click(String uuid) {
        try { get("/json/url/" + uuid); } catch (Exception ignored) {}
    }
}
