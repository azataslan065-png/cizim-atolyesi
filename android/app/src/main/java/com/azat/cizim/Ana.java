package com.azat.cizim;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/* Çizim Atölyesi Android kabuğu (2026-11-19, kullanıcı: "Apk ye çevir şimdi").
   - Sayfa APK'nın içindedir ve site adresiyle açılır (bkz. ver): internet olmadan çalışır, otomatik kayıt (IndexedDB) aynı adreste kalır.
   - Yakınlaştırma kapalı (sayfa kendi zoom'unu yapar), ekran döndürünce sayfa yeniden yüklenmez (çizim kaybolmaz).
   - Kaydet: resim doğrudan Galeri'ye (Resimler/Çizim Atölyesi) yazılır. Resim aç: Android dosya seçici.
   - Tam ekran: durum ve gezinme çubukları gizlenir. Geri tuşu: önce sayfadaki pencere/panel/seçim kapanır, sonra çıkış penceresi.
   - Güncelleme: açılışta GitHub'daki surum.json'a bakılır, yeni derleme varsa sorulur (bkz. Guncelle). */
public class Ana extends Activity {
    static final String EV = "azataslan065-png.github.io", YOL = "/cizim-atolyesi/";
    private static final String ADRES = "https://" + EV + YOL + "index.html";
    private static final int DOSYA = 7;
    private final Handler h = new Handler(Looper.getMainLooper());
    private WebView w;
    private ValueCallback<Uri[]> dosyaGeri;
    private boolean tam;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout kok = new FrameLayout(this);
        w = new WebView(this);
        w.setBackgroundColor(Color.parseColor("#17191d"));
        kok.addView(w, new FrameLayout.LayoutParams(-1, -1));
        setContentView(kok);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setTextZoom(100);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setUserAgentString(s.getUserAgentString() + " CizimAtolyesiApp/" + BuildConfig.VERSION_CODE);
        w.setOverScrollMode(View.OVER_SCROLL_NEVER);
        w.setVerticalScrollBarEnabled(false);
        w.setHorizontalScrollBarEnabled(false);
        if (Build.VERSION.SDK_INT >= 26) w.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        w.addJavascriptInterface(new Kopru(), "CizimApp");
        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return disari(r.getUrl()); }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) { return ver(r); }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> g, FileChooserParams p) {
                if (dosyaGeri != null) dosyaGeri.onReceiveValue(null);
                dosyaGeri = g;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                try { startActivityForResult(Intent.createChooser(i, "Resim seç"), DOSYA); }
                catch (ActivityNotFoundException e) { dosyaGeri = null; return false; }
                return true;
            }
        });
        w.loadUrl(ADRES);
        Guncelle.denetle(this);
    }

    /* Site adresindeki dosyalar APK'daki kopyadan verilir. */
    private WebResourceResponse ver(WebResourceRequest r) {
        try {
            Uri u = r.getUrl();
            if (!"GET".equalsIgnoreCase(r.getMethod()) || !EV.equals(u.getHost())) return null;
            String p = u.getPath();
            if (p == null || !p.startsWith(YOL)) return null;
            String ad = p.substring(YOL.length());
            if (ad.isEmpty()) ad = "index.html";
            if (ad.contains("..") || ad.startsWith("/")) return null;
            String tur = ad.endsWith(".html") ? "text/html" : ad.endsWith(".js") ? "text/javascript" : ad.endsWith(".png") ? "image/png"
                : ad.endsWith(".json") ? "application/json" : null;
            if (tur == null) return null;
            InputStream in = getAssets().open(ad);
            Map<String, String> hd = new HashMap<>();
            hd.put("Cache-Control", "no-store");
            return new WebResourceResponse(tur, tur.startsWith("image/") ? null : "utf-8", 200, "OK", hd, in);
        } catch (Exception e) { return null; }
    }

    private boolean disari(Uri u) {
        String sema = u.getScheme() == null ? "" : u.getScheme();
        if (("https".equals(sema) || "http".equals(sema)) && EV.equals(u.getHost()) && u.getPath() != null && u.getPath().startsWith(YOL)) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { Toast.makeText(this, "Bağlantı açılamadı", Toast.LENGTH_SHORT).show(); }
        return true;
    }

    private void tamEkran(boolean ac) {
        tam = ac;
        Window win = getWindow();
        View d = win.getDecorView();
        if (ac) {
            d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            if (Build.VERSION.SDK_INT >= 28) win.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        } else {
            d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f && tam) tamEkran(true); }

    @Override protected void onActivityResult(int kod, int sonuc, Intent veri) {
        super.onActivityResult(kod, sonuc, veri);
        if (kod != DOSYA || dosyaGeri == null) return;
        Uri[] r = null;
        if (sonuc == RESULT_OK && veri != null && veri.getData() != null) r = new Uri[]{ veri.getData() };
        dosyaGeri.onReceiveValue(r);
        dosyaGeri = null;
    }

    @Override public void onBackPressed() {
        if (Guncelle.acik()) { Guncelle.kapat(); return; }
        if (Cikis.acik()) { Cikis.kapat(); return; }
        if (w == null) { super.onBackPressed(); return; }
        w.evaluateJavascript("(function(){try{return !!(window.__appGeri&&window.__appGeri())}catch(e){return false}})()",
            v -> { if (!"true".equals(v)) Cikis.goster(Ana.this); });
    }

    @Override protected void onResume() { super.onResume(); if (w != null) w.onResume(); Guncelle.donus(this); Guncelle.denetle(this); }
    @Override protected void onPause() { if (w != null) { w.evaluateJavascript("try{window.__appKaydet&&window.__appKaydet()}catch(e){}", null); w.onPause(); } super.onPause(); }
    @Override protected void onDestroy() { if (w != null) { w.destroy(); w = null; } super.onDestroy(); }

    private final class Kopru {
        @JavascriptInterface public int surumKod() { return BuildConfig.VERSION_CODE; }
        @JavascriptInterface public String surumAd() { return BuildConfig.VERSION_NAME; }
        @JavascriptInterface public void tamEkranAyarla(final boolean ac) { h.post(() -> tamEkran(ac)); }
        @JavascriptInterface public boolean tamEkranMi() { return tam; }
        @JavascriptInterface public void guncellemeDenetle() { h.post(() -> Guncelle.elle(Ana.this)); }
        @JavascriptInterface public void cik() { h.post(Ana.this::finish); }
        /* Resmi Galeri'ye kaydeder: Resimler/Çizim Atölyesi. Sonuç sayfaya __appKaydedildi(ok, mesaj) ile bildirilir. */
        @JavascriptInterface public void resimKaydet(final String ad, final String tip, final String b64) {
            new Thread(() -> {
                boolean ok = false; String m;
                try {
                    byte[] veri = Base64.decode(b64, Base64.DEFAULT);
                    String a = ad.replaceAll("[\\\\/:*?\"<>|]", "_");
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, a);
                        cv.put(MediaStore.MediaColumns.MIME_TYPE, tip);
                        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Çizim Atölyesi");
                        Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                        try (OutputStream o = getContentResolver().openOutputStream(u)) { o.write(veri); }
                    } else {
                        File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Çizim Atölyesi");
                        dir.mkdirs();
                        File f = new File(dir, a);
                        try (FileOutputStream o = new FileOutputStream(f)) { o.write(veri); }
                        MediaScannerConnection.scanFile(Ana.this, new String[]{ f.getAbsolutePath() }, new String[]{ tip }, null);
                    }
                    ok = true; m = "Galeriye kaydedildi: " + a;
                } catch (Exception e) { m = "Kaydedilemedi: " + e.getMessage(); }
                final boolean fok = ok; final String fm = m.replace("\\", "").replace("'", "’");
                h.post(() -> { if (w != null) w.evaluateJavascript("try{window.__appKaydedildi&&window.__appKaydedildi(" + fok + ",'" + fm + "')}catch(e){}", null); });
            }).start();
        }
        /* Video (zaman atlamalı / animasyon) Galeri'ye: Filmler/Çizim Atölyesi */
        @JavascriptInterface public void videoKaydet(final String ad, final String tip, final String b64) {
            new Thread(() -> {
                boolean ok = false; String m;
                try {
                    byte[] veri = Base64.decode(b64, Base64.DEFAULT);
                    String a = ad.replaceAll("[\\\\/:*?\"<>|]", "_");
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, a);
                        cv.put(MediaStore.MediaColumns.MIME_TYPE, tip);
                        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Çizim Atölyesi");
                        Uri u = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv);
                        try (OutputStream o = getContentResolver().openOutputStream(u)) { o.write(veri); }
                    } else {
                        File dir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Çizim Atölyesi"); dir.mkdirs();
                        File f = new File(dir, a);
                        try (FileOutputStream o = new FileOutputStream(f)) { o.write(veri); }
                        MediaScannerConnection.scanFile(Ana.this, new String[]{ f.getAbsolutePath() }, new String[]{ tip }, null);
                    }
                    ok = true; m = "Video galeriye kaydedildi: " + a;
                } catch (Exception e) { m = "Kaydedilemedi: " + e.getMessage(); }
                final boolean fok = ok; final String fm = m.replace("\\", "").replace("'", "’");
                h.post(() -> { if (w != null) w.evaluateJavascript("try{window.__appKaydedildi&&window.__appKaydedildi(" + fok + ",'" + fm + "')}catch(e){}", null); });
            }).start();
        }
        @JavascriptInterface public void paylas(final String ad, final String tip, final String b64) {
            new Thread(() -> {
                try {
                    File dir = new File(getCacheDir(), "paylas"); dir.mkdirs();
                    File f = new File(dir, ad.replaceAll("[\\\\/:*?\"<>|]", "_"));
                    try (FileOutputStream o = new FileOutputStream(f)) { o.write(Base64.decode(b64, Base64.DEFAULT)); }
                    Uri u = Dosya.uri(Ana.this, f);
                    Intent i = new Intent(Intent.ACTION_SEND).setType(tip).putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    h.post(() -> { try { startActivity(Intent.createChooser(i, "Paylaş")); } catch (Exception e) { Toast.makeText(Ana.this, "Paylaşılamadı", Toast.LENGTH_SHORT).show(); } });
                } catch (Exception e) { h.post(() -> Toast.makeText(Ana.this, "Paylaşılamadı", Toast.LENGTH_SHORT).show()); }
            }).start();
        }
    }
}
