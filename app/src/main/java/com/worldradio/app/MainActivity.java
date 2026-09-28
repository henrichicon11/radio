package com.worldradio.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity implements StationAdapter.Host {
    private static final int TAB_COUNTRIES = 0, TAB_FAV = 1, TAB_SEARCH = 2;
    private static final int PAGE = 50;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ListView list;
    private ProgressBar loading;
    private TextView empty, subtitle;
    private EditText search;
    private TextView[] tabs;
    private int tab = TAB_COUNTRIES;

    private List<Country> allCountries = new ArrayList<>();
    private final CountryAdapter countryAdapter = new CountryAdapter();
    private StationAdapter stationAdapter;
    private String lastQuery = "";
    private int searchGen = 0;
    private boolean searchLoading = false, searchEnd = false;
    private final Runnable doSearch = () -> startStationSearch(search.getText().toString());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        list = findViewById(R.id.list);
        loading = findViewById(R.id.loading);
        empty = findViewById(R.id.empty);
        subtitle = findViewById(R.id.subtitle);
        search = findViewById(R.id.search);
        tabs = new TextView[]{findViewById(R.id.tabCountries), findViewById(R.id.tabFav), findViewById(R.id.tabSearch)};
        for (int i = 0; i < tabs.length; i++) { final int t = i; tabs[i].setOnClickListener(v -> selectTab(t)); }
        stationAdapter = new StationAdapter(this, this);
        setupPlayerBar();

        list.setOnItemClickListener((p, v, pos, id) -> {
            if (tab == TAB_COUNTRIES) {
                Country c = countryAdapter.shown.get(pos);
                Intent i = new Intent(this, StationsActivity.class);
                i.putExtra("code", c.code);
                i.putExtra("name", c.name);
                i.putExtra("count", c.count);
                startActivity(i);
            } else {
                playStation(stationAdapter.getItem(pos));
            }
        });
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView v, int s) {}
            @Override public void onScroll(AbsListView v, int first, int visible, int total) {
                if (tab == TAB_SEARCH && !searchLoading && !searchEnd && total > 0 && first + visible >= total - 5)
                    loadMoreSearch();
            }
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                if (tab == TAB_COUNTRIES) filterCountries(e.toString());
                else if (tab == TAB_SEARCH) { main.removeCallbacks(doSearch); main.postDelayed(doSearch, 450); }
            }
        });

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);

        selectTab(TAB_COUNTRIES);
        loadCountries();
    }

    private void selectTab(int t) {
        tab = t;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setBackgroundResource(i == t ? R.drawable.bg_tab_sel : R.drawable.bg_tab);
        }
        empty.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
        search.setVisibility(t == TAB_FAV ? View.GONE : View.VISIBLE);
        search.setText("");
        if (t == TAB_COUNTRIES) {
            search.setHint("Search countries…");
            list.setAdapter(countryAdapter);
            filterCountries("");
            if (allCountries.isEmpty()) loading.setVisibility(View.VISIBLE);
        } else if (t == TAB_FAV) {
            list.setAdapter(stationAdapter);
            showFavorites();
        } else {
            search.setHint("Station name, e.g. BBC, Jazz, FM…");
            stationAdapter.items.clear();
            stationAdapter.notifyDataSetChanged();
            list.setAdapter(stationAdapter);
            showEmpty("Type a station name to search\nacross the whole world 🌍");
            search.requestFocus();
        }
    }

    private void showEmpty(String msg) {
        empty.setText(msg);
        empty.setVisibility(View.VISIBLE);
    }

    private void loadCountries() {
        loading.setVisibility(View.VISIBLE);
        io.execute(() -> {
            try {
                List<Country> c = RadioApi.countries();
                main.post(() -> {
                    allCountries = c;
                    int total = 0; for (Country x : c) total += x.count;
                    subtitle.setText(String.format(Locale.US, "%,d stations in %d countries", total, c.size()));
                    if (tab == TAB_COUNTRIES) {
                        loading.setVisibility(View.GONE);
                        filterCountries(search.getText().toString());
                    }
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (tab != TAB_COUNTRIES) return;
                    loading.setVisibility(View.GONE);
                    showEmpty("Could not load countries.\nCheck your internet connection.\n\nTap here to retry");
                    empty.setOnClickListener(v -> { empty.setVisibility(View.GONE); loadCountries(); });
                });
            }
        });
    }

    private void filterCountries(String q) {
        String f = q.trim().toLowerCase(Locale.ROOT);
        countryAdapter.shown.clear();
        for (Country c : allCountries)
            if (f.isEmpty() || c.name.toLowerCase(Locale.ROOT).contains(f) || c.code.equalsIgnoreCase(f))
                countryAdapter.shown.add(c);
        countryAdapter.notifyDataSetChanged();
        if (!allCountries.isEmpty()) {
            if (countryAdapter.shown.isEmpty()) showEmpty("No country matches “" + q + "”");
            else empty.setVisibility(View.GONE);
        }
    }

    private void showFavorites() {
        stationAdapter.items.clear();
        stationAdapter.items.addAll(Favorites.all(this));
        stationAdapter.notifyDataSetChanged();
        if (stationAdapter.items.isEmpty()) showEmpty("No favorites yet.\nTap ☆ on any station to save it here.");
        else empty.setVisibility(View.GONE);
    }

    private void startStationSearch(String q) {
        lastQuery = q.trim();
        searchGen++;
        stationAdapter.items.clear();
        stationAdapter.notifyDataSetChanged();
        searchEnd = false;
        if (lastQuery.length() < 2) {
            loading.setVisibility(View.GONE);
            showEmpty("Type a station name to search\nacross the whole world 🌍");
            return;
        }
        empty.setVisibility(View.GONE);
        loadMoreSearch();
    }

    private void loadMoreSearch() {
        final int gen = searchGen;
        final String q = lastQuery;
        final int offset = stationAdapter.items.size();
        searchLoading = true;
        if (offset == 0) loading.setVisibility(View.VISIBLE);
        io.execute(() -> {
            List<Station> res; Exception err = null;
            try { res = RadioApi.search(q, offset, PAGE); } catch (Exception e) { res = new ArrayList<>(); err = e; }
            final List<Station> r = res; final Exception fe = err;
            main.post(() -> {
                if (gen != searchGen || tab != TAB_SEARCH) return;
                searchLoading = false;
                loading.setVisibility(View.GONE);
                if (fe != null && offset == 0) { showEmpty("Search failed. Check your connection."); return; }
                if (r.size() < PAGE) searchEnd = true;
                stationAdapter.items.addAll(r);
                stationAdapter.notifyDataSetChanged();
                if (stationAdapter.items.isEmpty()) showEmpty("No stations found for “" + q + "”");
            });
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (tab == TAB_FAV) showFavorites(); else stationAdapter.notifyDataSetChanged();
    }

    @Override public String playingUuid() { return currentUuid(); }
    @Override public void onFavToggled() { if (tab == TAB_FAV) showFavorites(); else stationAdapter.notifyDataSetChanged(); }
    @Override protected void onFavoritesChanged() { onFavToggled(); }
    @Override protected void onNowPlayingChanged() { if (tab != TAB_COUNTRIES) stationAdapter.notifyDataSetChanged(); }

    @Override public void onBackPressed() {
        if (tab != TAB_COUNTRIES) { selectTab(TAB_COUNTRIES); return; }
        moveTaskToBack(true); // keep playing in background
    }

    private class CountryAdapter extends BaseAdapter {
        final List<Country> shown = new ArrayList<>();
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public View getView(int pos, View v, ViewGroup parent) {
            if (v == null) v = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_country, parent, false);
            Country c = shown.get(pos);
            ((TextView) v.findViewById(R.id.flag)).setText(Country.flag(c.code));
            ((TextView) v.findViewById(R.id.name)).setText(c.name);
            ((TextView) v.findViewById(R.id.count)).setText(String.format(Locale.US, "%,d", c.count));
            return v;
        }
    }
}
