package com.worldradio.app;

import org.json.JSONException;
import org.json.JSONObject;

public class Station {
    public String uuid, name, url, favicon, country, countryCode, tags, codec, language, state, homepage;
    public int bitrate, votes;

    public static Station fromJson(JSONObject o) {
        Station s = new Station();
        s.uuid = o.optString("stationuuid");
        s.name = o.optString("name", "").trim();
        String resolved = o.optString("url_resolved", "");
        s.url = resolved.isEmpty() ? o.optString("url", "") : resolved;
        s.favicon = o.optString("favicon", "");
        s.country = o.optString("country", "");
        s.countryCode = o.optString("countrycode", "");
        s.tags = o.optString("tags", "");
        s.codec = o.optString("codec", "");
        s.language = o.optString("language", "");
        s.state = o.optString("state", "");
        s.homepage = o.optString("homepage", "");
        s.bitrate = o.optInt("bitrate", 0);
        s.votes = o.optInt("votes", 0);
        if (s.name.isEmpty()) s.name = "Unnamed station";
        return s;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("stationuuid", uuid); o.put("name", name); o.put("url_resolved", url);
            o.put("favicon", favicon); o.put("country", country); o.put("countrycode", countryCode);
            o.put("tags", tags); o.put("codec", codec); o.put("language", language);
            o.put("state", state); o.put("homepage", homepage); o.put("bitrate", bitrate); o.put("votes", votes);
        } catch (JSONException ignored) {}
        return o;
    }

    public String subtitle() {
        StringBuilder b = new StringBuilder();
        if (!state.isEmpty()) b.append(state);
        if (!country.isEmpty()) { if (b.length() > 0) b.append(", "); b.append(country); }
        if (!codec.isEmpty()) { if (b.length() > 0) b.append(" · "); b.append(codec); }
        if (bitrate > 0) { b.append(" ").append(bitrate).append("kbps"); }
        return b.toString();
    }

    public String prettyTags() {
        if (tags == null || tags.isEmpty()) return language == null ? "" : language;
        String[] parts = tags.split(",");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length && i < 4; i++) {
            String t = parts[i].trim();
            if (t.isEmpty()) continue;
            if (b.length() > 0) b.append(" • ");
            b.append(t);
        }
        return b.toString();
    }
}
