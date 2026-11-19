package com.salon.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int BG = Color.parseColor("#0B0E14");
    private static final int CARD = Color.parseColor("#1B2230");
    private static final int ACCENT = Color.parseColor("#FF8A4C");
    private static final int MUTED = Color.parseColor("#8E97A6");
    private static final int FG = Color.parseColor("#F2EFE9");
    private static final Locale TR = new Locale("tr", "TR");

    private static final String[] FEATURED = {
            "com.google.android.youtube.tv",
            "com.google.android.youtube",
            "com.netflix.ninja",
            "com.netflix.mediaclient",
            "com.amazon.amazonvideo.livingroom",
            "com.amazon.avod.thirdpartyclient",
            "com.disney.disneyplus",
            "com.wbd.stream",
            "com.hbo.hbonow",
            "com.spotify.tv.android",
            "com.spotify.music",
            "org.xbmc.kodi",
            "org.videolan.vlc"
    };

    private static class AppItem {
        String pkg;
        String label;
        String tag;
        String mono;
        Drawable banner;
        Drawable icon;
        Intent intent;
    }

    private float vh;
    private int screenW, screenH, padX, gap, cols, tileW, tileH;
    private TextView clockView, dateView, greetView, heroTag, heroName;
    private LinearLayout content;
    private ScrollView scroll;
    private String lastSig = "";
    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;
        vh = screenH / 100f;
        padX = Math.round(screenW * 0.045f);
        gap = Math.round(screenW * 0.015f);
        cols = screenW > screenH ? 6 : 3;
        tileW = (screenW - 2 * padX - gap * (cols - 1)) / cols;
        tileH = Math.round(tileW * 0.56f);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(padX, Math.round(vh * 3.5f), padX, 0);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        TextView brand = text("SALON", 2.1f, MUTED, false);
        brand.setLetterSpacing(0.3f);
        brand.setPadding(0, Math.round(vh * 1f), 0, 0);
        greetView = text("", 3f, FG, false);
        greetView.setPadding(0, Math.round(vh * 1.2f), 0, 0);
        left.addView(brand);
        left.addView(greetView);

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        clockView = text("--:--", 8.5f, FG, false);
        clockView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        dateView = text("", 2.5f, MUTED, false);
        dateView.setPadding(0, Math.round(vh * 0.8f), 0, 0);
        right.addView(clockView);
        right.addView(dateView);

        top.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(right, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        heroTag = text("", 2f, ACCENT, true);
        heroTag.setLetterSpacing(0.2f);
        heroName = text("", 7f, FG, true);
        heroName.setSingleLine(true);
        heroName.setEllipsize(TextUtils.TruncateAt.END);
        heroName.setPadding(0, Math.round(vh * 1f), 0, 0);
        hero.addView(heroTag);
        hero.addView(heroName);
        root.addView(hero, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(vh * 17f)));

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setPadding(0, 0, 0, Math.round(vh * 6f));
        scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        ticker.run();
        rebuild();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override
    public void onBackPressed() {
        scroll.smoothScrollTo(0, 0);
        content.requestFocus();
    }

    private void updateClock() {
        Date now = new Date();
        clockView.setText(new SimpleDateFormat("HH:mm", TR).format(now));
        dateView.setText(new SimpleDateFormat("d MMMM, EEEE", TR).format(now));
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        greetView.setText(h < 6 ? "İyi geceler" : h < 12 ? "Günaydın" : h < 18 ? "İyi günler" : "İyi akşamlar");
    }

    private void rebuild() {
        PackageManager pm = getPackageManager();
        List<AppItem> items = new ArrayList<AppItem>();
        HashSet<String> seen = new HashSet<String>();
        collect(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, items, seen);
        collect(pm, Intent.CATEGORY_LAUNCHER, items, seen);

        final Collator collator = Collator.getInstance(TR);
        Collections.sort(items, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem x, AppItem y) {
                return collator.compare(x.label, y.label);
            }
        });

        StringBuilder sig = new StringBuilder();
        for (AppItem a : items) {
            sig.append(a.pkg).append('|');
        }
        String signature = sig.toString();
        if (signature.equals(lastSig) && content.getChildCount() > 0) {
            return;
        }
        lastSig = signature;

        List<AppItem> featured = new ArrayList<AppItem>();
        for (String p : FEATURED) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).pkg.equals(p)) {
                    featured.add(items.remove(i));
                    break;
                }
            }
        }

        content.removeAllViews();
        addSection("Öne çıkanlar", featured);
        addSection("Tüm uygulamalar", items);
        addSection("Sistem", systemItems());
        content.post(new Runnable() {
            @Override
            public void run() {
                content.requestFocus();
            }
        });
    }

    private void collect(PackageManager pm, String category, List<AppItem> out, HashSet<String> seen) {
        Intent q = new Intent(Intent.ACTION_MAIN, null);
        q.addCategory(category);
        List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
        for (ResolveInfo ri : list) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || seen.contains(pkg)) {
                continue;
            }
            seen.add(pkg);
            AppItem a = new AppItem();
            a.pkg = pkg;
            CharSequence lbl = ri.loadLabel(pm);
            a.label = lbl == null ? pkg : lbl.toString();
            a.tag = "Uygulama";
            a.banner = ri.activityInfo.loadBanner(pm);
            a.icon = ri.loadIcon(pm);
            Intent li = new Intent(Intent.ACTION_MAIN);
            li.addCategory(category);
            li.setComponent(new ComponentName(pkg, ri.activityInfo.name));
            li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            a.intent = li;
            out.add(a);
        }
    }

    private List<AppItem> systemItems() {
        List<AppItem> out = new ArrayList<AppItem>();
        out.add(sys("Ayarlar", "A", Settings.ACTION_SETTINGS));
        out.add(sys("Wi-Fi", "W", Settings.ACTION_WIFI_SETTINGS));
        out.add(sys("Bluetooth", "B", Settings.ACTION_BLUETOOTH_SETTINGS));
        out.add(sys("Uygulama yöneticisi", "U", Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS));
        return out;
    }

    private AppItem sys(String label, String mono, String action) {
        AppItem a = new AppItem();
        a.label = label;
        a.mono = mono;
        a.tag = "Sistem";
        Intent i = new Intent(action);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        a.intent = i;
        return a;
    }

    private void addSection(String title, List<AppItem> apps) {
        if (apps.isEmpty()) {
            return;
        }
        TextView t = text(title.toUpperCase(TR), 2f, MUTED, false);
        t.setLetterSpacing(0.2f);
        t.setPadding(0, Math.round(vh * 2f), 0, Math.round(vh * 0.6f));
        content.addView(t);
        LinearLayout row = null;
        int n = 0;
        for (AppItem a : apps) {
            if (n % cols == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                content.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            row.addView(tile(a));
            n++;
        }
    }

    private View tile(final AppItem a) {
        final float radius = vh * 1.8f;
        final int b = Math.round(vh * 0.5f);

        final FrameLayout outer = new FrameLayout(this);
        outer.setPadding(b, b, b, b);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH);
        lp.rightMargin = gap;
        lp.bottomMargin = Math.round(vh * 1f);
        outer.setLayoutParams(lp);

        FrameLayout inner = new FrameLayout(this);
        inner.setBackground(round(CARD, radius));
        inner.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        inner.setClipToOutline(true);

        if (a.banner != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageDrawable(a.banner);
            inner.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            if (a.icon != null) {
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(a.icon);
                int s = Math.round(vh * 8f);
                col.addView(iv, new LinearLayout.LayoutParams(s, s));
            } else {
                col.addView(text(a.mono == null ? "?" : a.mono, 6f, FG, true));
            }
            TextView name = text(a.label, 2.2f, FG, false);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setPadding(Math.round(vh), Math.round(vh * 0.8f), Math.round(vh), 0);
            col.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            inner.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        outer.addView(inner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        outer.setFocusable(true);
        outer.setClickable(true);
        outer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(hasFocus ? round(Color.WHITE, radius + b) : null);
                float s = hasFocus ? 1.08f : 1f;
                v.animate().scaleX(s).scaleY(s).setDuration(120).start();
                if (hasFocus) {
                    heroTag.setText(a.tag);
                    heroName.setText(a.label);
                }
            }
        });
        outer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(a.intent);
            }
        });
        outer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (a.pkg == null) {
                    return false;
                }
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.pkg));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                open(i);
                return true;
            }
        });
        return outer;
    }

    private void open(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Açılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private TextView text(String s, float sizeVh, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, vh * sizeVh);
        t.setIncludeFontPadding(false);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return t;
    }
}
