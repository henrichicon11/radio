package com.worldradio.app;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;

/** Shared "now playing" bar for all screens. */
public abstract class BaseActivity extends Activity {
    private MediaController controller;
    private View bar;
    private ImageView logo;
    private TextView title, sub;
    private ImageButton playBtn, favBtn;
    private ProgressBar loading;
    private String streamTitle = "";

    private final Player.Listener listener = new Player.Listener() {
        @Override public void onEvents(@NonNull Player player, @NonNull Player.Events events) { refreshBar(); }
        @Override public void onMediaMetadataChanged(@NonNull MediaMetadata m) {
            // ICY "now playing" song title from the stream
            CharSequence t = m.title;
            Station s = controller == null ? null : PlayerManager.current(controller);
            streamTitle = (t != null && s != null && !t.toString().equals(s.name)) ? t.toString() : "";
            refreshBar();
        }
        @Override public void onMediaItemTransition(androidx.media3.common.MediaItem item, int reason) { streamTitle = ""; }
        @Override public void onPlayerError(@NonNull PlaybackException error) {
            Toast.makeText(BaseActivity.this, "This station is unavailable right now. Try another one.",
                    Toast.LENGTH_LONG).show();
        }
    };

    protected void setupPlayerBar() {
        bar = findViewById(R.id.playerBar);
        logo = findViewById(R.id.pbLogo);
        title = findViewById(R.id.pbTitle);
        sub = findViewById(R.id.pbSub);
        sub.setSelected(true);
        playBtn = findViewById(R.id.pbPlay);
        favBtn = findViewById(R.id.pbFav);
        loading = findViewById(R.id.pbLoading);
        playBtn.setOnClickListener(v -> {
            if (controller == null) return;
            if (controller.isPlaying() || controller.getPlayWhenReady()) controller.pause();
            else {
                if (controller.getPlaybackState() == Player.STATE_IDLE) controller.prepare();
                // live radio: jump back to live edge on resume
                controller.seekToDefaultPosition();
                controller.play();
            }
        });
        findViewById(R.id.pbStop).setOnClickListener(v -> {
            if (controller == null) return;
            controller.stop();
            controller.clearMediaItems();
        });
        favBtn.setOnClickListener(v -> {
            if (controller == null) return;
            Station s = PlayerManager.current(controller);
            if (s == null) return;
            boolean on = Favorites.toggle(this, s);
            Toast.makeText(this, on ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
            refreshBar();
            onFavoritesChanged();
        });
    }

    protected void onFavoritesChanged() {}

    protected void playStation(Station s) { PlayerManager.play(this, s); }

    @Override protected void onStart() {
        super.onStart();
        PlayerManager.get(this, c -> {
            controller = c;
            c.addListener(listener);
            refreshBar();
        });
    }

    @Override protected void onStop() {
        if (controller != null) controller.removeListener(listener);
        super.onStop();
    }

    protected String currentUuid() {
        if (controller == null) return null;
        Station s = PlayerManager.current(controller);
        return s == null ? null : s.uuid;
    }

    private void refreshBar() {
        if (bar == null) return;
        Station s = controller == null ? null : PlayerManager.current(controller);
        if (s == null) { bar.setVisibility(View.GONE); onNowPlayingChanged(); return; }
        bar.setVisibility(View.VISIBLE);
        title.setText(s.name);
        int state = controller.getPlaybackState();
        boolean buffering = state == Player.STATE_BUFFERING && controller.getPlayWhenReady();
        String status;
        if (controller.getPlayerError() != null) status = "Unavailable";
        else if (buffering) status = "Connecting…";
        else if (controller.isPlaying()) status = streamTitle.isEmpty() ? "Live · " + s.country : "♪ " + streamTitle;
        else status = "Paused";
        sub.setText(status);
        loading.setVisibility(buffering ? View.VISIBLE : View.GONE);
        playBtn.setImageResource(controller.getPlayWhenReady() ? R.drawable.ic_pause : R.drawable.ic_play);
        favBtn.setImageResource(Favorites.has(this, s.uuid) ? R.drawable.ic_star : R.drawable.ic_star_border);
        ImageLoader.load(s.favicon, logo, R.drawable.ic_radio);
        onNowPlayingChanged();
    }

    protected void onNowPlayingChanged() {}
}
