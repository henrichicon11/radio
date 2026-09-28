package com.worldradio.app;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** App-wide access to the MediaController connected to PlaybackService. */
public class PlayerManager {
    public interface Ready { void onReady(MediaController c); }

    private static ListenableFuture<MediaController> future;
    private static MediaController controller;
    private static final List<Ready> pending = new ArrayList<>();

    public static void get(Context ctx, Ready r) {
        if (controller != null && controller.isConnected()) { r.onReady(controller); return; }
        pending.add(r);
        if (future != null && !future.isDone()) return;
        Context app = ctx.getApplicationContext();
        SessionToken token = new SessionToken(app, new ComponentName(app, PlaybackService.class));
        future = new MediaController.Builder(app, token).buildAsync();
        future.addListener(() -> {
            try { controller = future.get(); } catch (Exception e) { controller = null; }
            List<Ready> copy = new ArrayList<>(pending);
            pending.clear();
            if (controller != null) for (Ready x : copy) x.onReady(controller);
        }, ContextCompat.getMainExecutor(app));
    }

    public static void play(Context ctx, Station s) {
        Favorites.saveLast(ctx, s);
        new Thread(() -> RadioApi.click(s.uuid)).start();
        Bundle extras = new Bundle();
        extras.putString("station", s.toJson().toString());
        MediaMetadata.Builder md = new MediaMetadata.Builder()
                .setTitle(s.name)
                .setStation(s.name)
                .setArtist(s.country)
                .setDisplayTitle(s.name)
                .setExtras(extras);
        if (s.favicon != null && !s.favicon.isEmpty()) {
            try { md.setArtworkUri(Uri.parse(s.favicon)); } catch (Exception ignored) {}
        }
        Uri uri = Uri.parse(s.url);
        MediaItem item = new MediaItem.Builder()
                .setMediaId(s.uuid)
                .setUri(uri)
                .setRequestMetadata(new MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
                .setMediaMetadata(md.build())
                .build();
        get(ctx, c -> {
            c.setMediaItem(item);
            c.prepare();
            c.play();
        });
    }

    public static Station current(MediaController c) {
        MediaItem m = c.getCurrentMediaItem();
        if (m == null || m.mediaMetadata.extras == null) return null;
        String j = m.mediaMetadata.extras.getString("station");
        if (j == null) return null;
        try { return Station.fromJson(new JSONObject(j)); } catch (Exception e) { return null; }
    }
}
