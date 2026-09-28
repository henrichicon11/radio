package com.worldradio.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class StationAdapter extends BaseAdapter {
    public interface Host { String playingUuid(); void onFavToggled(); }

    private final Context ctx;
    private final Host host;
    public final List<Station> items = new ArrayList<>();
    private final int accent, normal;

    public StationAdapter(Context ctx, Host host) {
        this.ctx = ctx; this.host = host;
        accent = ctx.getResources().getColor(R.color.accent);
        normal = ctx.getResources().getColor(R.color.text);
    }

    @Override public int getCount() { return items.size(); }
    @Override public Station getItem(int i) { return items.get(i); }
    @Override public long getItemId(int i) { return i; }

    @Override
    public View getView(int pos, View v, ViewGroup parent) {
        if (v == null) v = LayoutInflater.from(ctx).inflate(R.layout.item_station, parent, false);
        Station s = items.get(pos);
        TextView name = v.findViewById(R.id.name);
        name.setText(s.name);
        boolean playing = s.uuid.equals(host.playingUuid());
        name.setTextColor(playing ? accent : normal);
        ((TextView) v.findViewById(R.id.info)).setText((playing ? "▶ " : "") + s.subtitle());
        ((TextView) v.findViewById(R.id.tags)).setText(s.prettyTags());
        ImageLoader.load(s.favicon, (ImageView) v.findViewById(R.id.logo), R.drawable.ic_radio);
        ImageButton fav = v.findViewById(R.id.fav);
        fav.setImageResource(Favorites.has(ctx, s.uuid) ? R.drawable.ic_star : R.drawable.ic_star_border);
        fav.setOnClickListener(x -> {
            boolean on = Favorites.toggle(ctx, s);
            Toast.makeText(ctx, on ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
            host.onFavToggled();
        });
        return v;
    }
}
