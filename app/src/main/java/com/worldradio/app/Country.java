package com.worldradio.app;

public class Country {
    public final String name, code;
    public final int count;
    public Country(String name, String code, int count) { this.name = name; this.code = code; this.count = count; }

    public static String flag(String code) {
        if (code == null || code.length() != 2) return "🌐";
        String c = code.toUpperCase();
        int a = Character.codePointAt(c, 0) - 'A' + 0x1F1E6;
        int b = Character.codePointAt(c, 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(a)) + new String(Character.toChars(b));
    }
}
