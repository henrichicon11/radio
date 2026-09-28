package com.worldradio.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny async image loader with memory cache. */
public class ImageLoader {
    private static final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(12 * 1024 * 1024) {
        @Override protected int sizeOf(String k, Bitmap v) { return v.getByteCount(); }
    };
    private static final Set<String> failed = Collections.synchronizedSet(new HashSet<>());
    private static final ExecutorService pool = Executors.newFixedThreadPool(4);
    private static final Handler main = new Handler(Looper.getMainLooper());

    public static void load(String url, ImageView iv, int placeholder) {
        iv.setTag(url);
        if (url == null || url.isEmpty() || failed.contains(url)) { iv.setImageResource(placeholder); return; }
        Bitmap b = cache.get(url);
        if (b != null) { iv.setImageBitmap(b); return; }
        iv.setImageResource(placeholder);
        pool.execute(() -> {
            Bitmap bmp = download(url);
            if (bmp == null) { failed.add(url); return; }
            cache.put(url, bmp);
            main.post(() -> { if (url.equals(iv.getTag())) iv.setImageBitmap(bmp); });
        });
    }

    private static Bitmap download(String u) {
        HttpURLConnection c = null;
        try {
            for (int redirects = 0; redirects < 4; redirects++) {
                c = (HttpURLConnection) new URL(u).openConnection();
                c.setConnectTimeout(6000); c.setReadTimeout(8000);
                c.setInstanceFollowRedirects(true);
                c.setRequestProperty("User-Agent", "WorldRadioAndroid/1.0");
                int code = c.getResponseCode();
                if (code >= 300 && code < 400 && c.getHeaderField("Location") != null) {
                    u = new URL(new URL(u), c.getHeaderField("Location")).toString();
                    c.disconnect(); continue;
                }
                if (code != 200) return null;
                break;
            }
            InputStream in = c.getInputStream();
            byte[] data = readAll(in);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int s = 1;
            while (o.outWidth / (s * 2) >= 128 && o.outHeight / (s * 2) >= 128) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            return BitmapFactory.decodeByteArray(data, 0, data.length, o);
        } catch (Throwable t) { return null; }
        finally { if (c != null) c.disconnect(); }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n; int total = 0;
        while ((n = in.read(buf)) > 0) { bos.write(buf, 0, n); total += n; if (total > 2_000_000) break; }
        in.close();
        return bos.toByteArray();
    }

    public static Bitmap cached(String url) { return url == null ? null : cache.get(url); }
}
