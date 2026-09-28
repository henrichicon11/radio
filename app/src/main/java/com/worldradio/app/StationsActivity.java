package com.worldradio.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** All stations of one country, paged (infinite scroll) and filterable. */
public class StationsActivity extends BaseActivity implements StationAdapter.Host {
    private static final int PAGE = 60;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private String code;
    private StationAdapter adapter;
    private ProgressBar loading;
    private TextView empty;
    private EditText search;
    private String filter = "";
    private int gen = 0;
    private boolean busy = false, end = false;
    private final Runnable reload = () -> restart(search.getText().toString());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_stations);
        code = getIntent().getStringExtra("code");
        String name = getIntent().getStringExtra("name");
        int count = getIntent().getIntExtra("count", 0);
        ((TextView) findViewById(R.id.title)).setText(Country.flag(code) + "  " + name);
        ((TextView) findViewById(R.id.count)).setText(String.format(Locale.US, "%,d radio stations", count));
        findViewById(R.id.back).setOnClickListener(v -> finish());
        loading = findViewById(R.id.loading);
        empty = findViewById(R.id.empty);
        search = findViewById(R.id.search);
        search.setHint("Filter stations in " + name + "…");
        ListView list = findViewById(R.id.list);
        adapter = new StationAdapter(this, this);
        list.setAdapter(adapter);
        setupPlayerBar();

        list.setOnItemClickListener((p, v, pos, id) -> playStation(adapter.getItem(pos)));
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView v, int s) {}
            @Override public void onScroll(AbsListView v, int first, int visible, int total) {
                if (!busy && !end && total > 0 && first + visible >= total - 8) loadMore();
            }
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            @Override public void afterTextChanged(Editable e) { main.removeCallbacks(reload); main.postDelayed(reload, 450); }
        });
        restart("");
    }

    private void restart(String f) {
        filter = f.trim();
        gen++;
        busy = false; end = false;
        adapter.items.clear();
        adapter.notifyDataSetChanged();
        empty.setVisibility(View.GONE);
        loadMore();
    }

    private void loadMore() {
        final int g = gen;
        final int offset = adapter.items.size();
        final String f = filter;
        busy = true;
        if (offset == 0) loading.setVisibility(View.VISIBLE);
        io.execute(() -> {
            List<Station> res; Exception err = null;
            try { res = RadioApi.byCountry(code, f, offset, PAGE); } catch (Exception e) { res = new ArrayList<>(); err = e; }
            final List<Station> r = res; final Exception fe = err;
            main.post(() -> {
                if (g != gen) return;
                busy = false;
                loading.setVisibility(View.GONE);
                if (fe != null) {
                    if (offset == 0) {
                        empty.setText("Could not load stations.\nTap to retry.");
                        empty.setVisibility(View.VISIBLE);
                        empty.setOnClickListener(v -> restart(filter));
                    }
                    end = true; // stop auto-retry loop; retry via tap/filter
                    return;
                }
                if (r.size() < PAGE) end = true;
                adapter.items.addAll(r);
                adapter.notifyDataSetChanged();
                if (adapter.items.isEmpty()) {
                    empty.setText(f.isEmpty() ? "No stations available." : "No station matches “" + f + "”");
                    empty.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override protected void onResume() { super.onResume(); adapter.notifyDataSetChanged(); }
    @Override public String playingUuid() { return currentUuid(); }
    @Override public void onFavToggled() { adapter.notifyDataSetChanged(); }
    @Override protected void onFavoritesChanged() { adapter.notifyDataSetChanged(); }
    @Override protected void onNowPlayingChanged() { adapter.notifyDataSetChanged(); }
}
