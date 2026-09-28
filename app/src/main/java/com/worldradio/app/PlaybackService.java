package com.worldradio.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/** Background playback with media notification + lock-screen controls. */
@UnstableApi
public class PlaybackService extends MediaSessionService {
    private MediaSession session;

    @Override
    public void onCreate() {
        super.onCreate();
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent("WorldRadioAndroid/1.0")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000);
        DefaultDataSource.Factory ds = new DefaultDataSource.Factory(this, http);

        ExoPlayer player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(ds))
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setLoadControl(new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(15000, 50000, 2000, 3000).build())
                .build();

        Intent open = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, flags);

        session = new MediaSession.Builder(this, player)
                .setSessionActivity(pi)
                .setCallback(new MediaSession.Callback() {
                    @NonNull @Override
                    public ListenableFuture<List<MediaItem>> onAddMediaItems(@NonNull MediaSession s,
                            @NonNull MediaSession.ControllerInfo c, @NonNull List<MediaItem> items) {
                        List<MediaItem> out = new ArrayList<>();
                        for (MediaItem m : items) {
                            if (m.localConfiguration != null || m.requestMetadata.mediaUri == null) { out.add(m); continue; }
                            MediaItem.Builder b = m.buildUpon().setUri(m.requestMetadata.mediaUri);
                            String u = m.requestMetadata.mediaUri.toString().toLowerCase();
                            if (u.contains(".m3u8")) b.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8);
                            out.add(b.build());
                        }
                        return Futures.immediateFuture(out);
                    }
                })
                .build();
    }

    @Nullable @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) { return session; }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        if (session != null) {
            ExoPlayer p = (ExoPlayer) session.getPlayer();
            if (!p.getPlayWhenReady() || p.getMediaItemCount() == 0) stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        if (session != null) { session.getPlayer().release(); session.release(); session = null; }
        super.onDestroy();
    }
}
