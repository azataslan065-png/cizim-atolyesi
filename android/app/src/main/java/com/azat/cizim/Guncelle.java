package com.azat.cizim;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/* Uygulama içi güncelleme (2026-11-20, kullanıcı: Skorbord'daki gibi indirme çubuklu kart). Açılışta GitHub "apk" sürümündeki
   surum.json okunur; yeni derleme varsa tam ekran kart çıkar. "İndir ve kur": APK uygulamanın kendi klasörüne indirilir (ilerleme çubuğu),
   ardından Android kurulum ekranı açılır; yeni sürüm aynı imzayla eskisinin üstüne kurulur, çizimler korunur.
   Bilinmeyen kaynak izni yoksa ayar ekranı açılır; geri dönülünce kurulum kendiliğinden devam eder (bkz. donus). */
final class Guncelle {
    private static final String SURUM = "https://github.com/azataslan065-png/cizim-atolyesi/releases/download/apk/surum.json";
    private static final Handler h = new Handler(Looper.getMainLooper());
    private static FrameLayout ekran;
    private static ProgressBar cubuk;
    private static TextView durum;
    private static Button ana;
    private static File bekleyen;
    private static boolean indiriyor;

    static void denetle(Activity a) { calis(a, false); }
    static void elle(Activity a) { calis(a, true); }
    static boolean acik() { return ekran != null; }
    static void kapat() {
        if (ekran == null || indiriyor) return;
        final View e = ekran; ekran = null; cubuk = null; durum = null; ana = null;
        e.animate().alpha(0f).setDuration(120).withEndAction(() -> { ViewGroup p = (ViewGroup) e.getParent(); if (p != null) p.removeView(e); }).start();
    }
    static void donus(Activity a) { if (bekleyen != null && ekran != null) kur(a, bekleyen); }

    private static int dp(Activity a, float v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, a.getResources().getDisplayMetrics()); }
    private static String mb(long b) { return String.format(java.util.Locale.US, "%.1f MB", b / 1048576.0); }

    private static String oku(String adres) throws Exception {
        HttpURLConnection c = ac(adres);
        InputStream in = c.getInputStream(); ByteArrayOutputStream bo = new ByteArrayOutputStream(); byte[] b = new byte[4096]; int n;
        while ((n = in.read(b)) > 0) bo.write(b, 0, n);
        in.close(); return bo.toString("UTF-8");
    }
    private static HttpURLConnection ac(String adres) throws Exception {
        String u = adres;
        for (int i = 0; i < 6; i++) {
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(10000); c.setReadTimeout(20000); c.setInstanceFollowRedirects(false);
            c.setRequestProperty("Cache-Control", "no-cache");
            int k = c.getResponseCode();
            if (k >= 300 && k < 400) { String l = c.getHeaderField("Location"); c.disconnect(); if (l == null) throw new Exception("yönlendirme"); u = new URL(new URL(u), l).toString(); continue; }
            if (k != 200) throw new Exception("HTTP " + k);
            return c;
        }
        throw new Exception("çok fazla yönlendirme");
    }

    private static void calis(final Activity a, final boolean elle) {
        if (ekran != null) return;
        new Thread(() -> {
            try {
                JSONObject j = new JSONObject(oku(SURUM + "?t=" + System.currentTimeMillis()));
                final int kod = j.optInt("kod"); final String ad = j.optString("ad"), url = j.optString("url"), not = j.optString("not");
                final long boy = j.optLong("boyut");
                h.post(() -> {
                    if (a.isFinishing()) return;
                    if (kod > BuildConfig.VERSION_CODE && !url.isEmpty()) goster(a, kod, ad, url, not, boy);
                    else if (elle) Toast.makeText(a, "En güncel sürümü kullanıyorsun (" + BuildConfig.VERSION_NAME + ")", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                if (elle) h.post(() -> Toast.makeText(a, "Güncelleme denetlenemedi (internet?)", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private static void goster(final Activity a, final int kod, String ad, final String url, String not, long boy) {
        ekran = new FrameLayout(a);
        ekran.setBackgroundColor(Color.parseColor("#D9050710"));
        ekran.setClickable(true);
        LinearLayout kart = new LinearLayout(a);
        kart.setOrientation(LinearLayout.VERTICAL); kart.setGravity(Gravity.CENTER_HORIZONTAL); kart.setClickable(true);
        kart.setPadding(dp(a, 22), dp(a, 22), dp(a, 22), dp(a, 18));
        GradientDrawable z = new GradientDrawable(); z.setColor(Color.parseColor("#23262C")); z.setCornerRadius(dp(a, 22)); z.setStroke(dp(a, 1), Color.parseColor("#383D47"));
        kart.setBackground(z); kart.setElevation(dp(a, 12));
        ImageView ik = new ImageView(a); ik.setImageResource(R.mipmap.ic_launcher);
        kart.addView(ik, new LinearLayout.LayoutParams(dp(a, 64), dp(a, 64)));
        TextView bas = new TextView(a); bas.setText("Yeni sürüm hazır");
        bas.setTextColor(Color.WHITE); bas.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20); bas.setTypeface(Typeface.DEFAULT_BOLD);
        bas.setGravity(Gravity.CENTER); bas.setPadding(0, dp(a, 12), 0, dp(a, 2)); kart.addView(bas);
        TextView sv = new TextView(a); sv.setText(BuildConfig.VERSION_NAME + "  →  " + ad + (boy > 0 ? "   ·   " + mb(boy) : ""));
        sv.setTextColor(Color.parseColor("#4F9DFF")); sv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); sv.setGravity(Gravity.CENTER); sv.setPadding(0, 0, 0, dp(a, 10)); kart.addView(sv);
        if (not != null && !not.isEmpty()) {
            TextView nt = new TextView(a); nt.setText("Yenilikler: " + not);
            nt.setTextColor(Color.parseColor("#9AA1AD")); nt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13); nt.setGravity(Gravity.CENTER); nt.setLineSpacing(0, 1.2f); nt.setPadding(0, 0, 0, dp(a, 10));
            kart.addView(nt);
        }
        cubuk = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        cubuk.setMax(1000); cubuk.setProgress(0); cubuk.setVisibility(View.GONE);
        cubuk.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4F9DFF")));
        kart.addView(cubuk, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 10)));
        durum = new TextView(a); durum.setText("Çizimlerin korunur; yeni sürüm eskisinin üstüne kurulur.");
        durum.setTextColor(Color.parseColor("#9AA1AD")); durum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13); durum.setGravity(Gravity.CENTER); durum.setPadding(0, dp(a, 8), 0, dp(a, 16));
        kart.addView(durum);
        LinearLayout dug = new LinearLayout(a);
        Button son = dugme(a, "Sonra", "#2C3038");
        son.setOnClickListener(v -> kapat());
        ana = dugme(a, "İndir ve kur", "#4F9DFF");
        final Button fana = ana;
        ana.setOnClickListener(v -> {
            if (bekleyen != null) { kur(a, bekleyen); return; }
            if (indiriyor) return;
            indir(a, kod, url); fana.setText("İndiriliyor…"); fana.setEnabled(false); son.setEnabled(false); son.setAlpha(.4f);
        });
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(a, 48), 1); p1.setMargins(0, 0, dp(a, 10), 0);
        dug.addView(son, p1); dug.addView(ana, new LinearLayout.LayoutParams(0, dp(a, 48), 1));
        kart.addView(dug, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int gen = Math.min(dp(a, 380), a.getResources().getDisplayMetrics().widthPixels - dp(a, 40));
        ekran.addView(kart, new FrameLayout.LayoutParams(gen, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        ((FrameLayout) a.findViewById(android.R.id.content)).addView(ekran, new FrameLayout.LayoutParams(-1, -1));
        ekran.setAlpha(0f); ekran.animate().alpha(1f).setDuration(160).start();
    }

    private static Button dugme(Activity a, String s, String zemin) {
        Button b = new Button(a); b.setText(s); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15); b.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.parseColor(zemin)); g.setCornerRadius(dp(a, 24));
        b.setBackground(g); b.setStateListAnimator(null); return b;
    }

    private static void indir(final Activity a, final int kod, final String url) {
        indiriyor = true;
        if (cubuk != null) cubuk.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                File dir = a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS); if (dir == null) dir = a.getFilesDir();
                dir.mkdirs();
                File[] eski = dir.listFiles(); if (eski != null) for (File f : eski) if (f.getName().endsWith(".apk")) f.delete();
                final File apk = new File(dir, "CizimAtolyesi-" + kod + ".apk");
                HttpURLConnection c = ac(url);
                final long top = c.getContentLengthLong();
                InputStream in = c.getInputStream(); FileOutputStream o = new FileOutputStream(apk);
                byte[] b = new byte[16384]; int n; long say = 0, son = 0;
                while ((n = in.read(b)) > 0) {
                    o.write(b, 0, n); say += n;
                    long now = System.currentTimeMillis();
                    if (now - son > 120) { son = now; final long s = say; h.post(() -> ilerleme(s, top)); }
                }
                o.close(); in.close();
                final long s = say;
                h.post(() -> { indiriyor = false; ilerleme(s, top); bekleyen = apk;
                    if (durum != null) durum.setText("İndirildi. Kurulum ekranında \"Güncelle\"ye dokun.");
                    if (ana != null) { ana.setEnabled(true); ana.setText("Kur"); }
                    kur(a, apk); });
            } catch (Exception e) {
                h.post(() -> { indiriyor = false;
                    if (durum != null) durum.setText("İndirilemedi: " + e.getMessage() + " — internetini kontrol edip tekrar dene.");
                    if (ana != null) { ana.setEnabled(true); ana.setText("Tekrar dene"); } });
            }
        }).start();
    }

    private static void ilerleme(long say, long top) {
        if (cubuk == null || durum == null) return;
        if (top > 0) { cubuk.setProgress((int) Math.min(1000, say * 1000 / top)); durum.setText("İndiriliyor… %" + Math.min(100, say * 100 / top) + "  (" + mb(say) + " / " + mb(top) + ")"); }
        else durum.setText("İndiriliyor… " + mb(say));
    }

    private static void kur(Activity a, File apk) {
        try {
            if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
                if (durum != null) durum.setText("Kurulum için “Bu kaynaktan izin ver”i aç, sonra geri dön.");
                a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName())));
                return;
            }
            Uri u = Uri.parse("content://" + a.getPackageName() + ".dosya/apk/" + Uri.encode(apk.getName()));
            a.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(u, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            if (durum != null) durum.setText("Kurulum açılamadı: " + e.getMessage());
        }
    }
}
