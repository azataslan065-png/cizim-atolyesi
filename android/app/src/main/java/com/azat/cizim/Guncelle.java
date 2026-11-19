package com.azat.cizim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/* Güncelleme: GitHub'daki "apk" sürümünün surum.json dosyasına bakılır. Yeni derleme varsa sorulur; "İndir" APK'yı tarayıcıda indirir,
   Android kurulum ekranı yeni sürümü eskisinin üstüne kurar (aynı imza). İnternet yoksa sessizce geçilir. */
final class Guncelle {
    private static final String SURUM = "https://github.com/azataslan065-png/cizim-atolyesi/releases/download/apk/surum.json";
    private static final Handler h = new Handler(Looper.getMainLooper());

    static void denetle(Activity a) { calis(a, false); }
    static void elle(Activity a) { calis(a, true); }

    private static void calis(final Activity a, final boolean elle) {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(SURUM + "?t=" + System.currentTimeMillis()).openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(10000); c.setInstanceFollowRedirects(true);
                InputStream in = c.getInputStream(); ByteArrayOutputStream bo = new ByteArrayOutputStream(); byte[] b = new byte[4096]; int n;
                while ((n = in.read(b)) > 0) bo.write(b, 0, n);
                JSONObject j = new JSONObject(bo.toString("UTF-8"));
                final int kod = j.optInt("kod"); final String ad = j.optString("ad"), url = j.optString("url"), not = j.optString("not");
                h.post(() -> {
                    if (a.isFinishing()) return;
                    if (kod > BuildConfig.VERSION_CODE && !url.isEmpty()) {
                        new AlertDialog.Builder(a, android.R.style.Theme_Material_Dialog_Alert)
                            .setTitle("Yeni sürüm var: " + ad)
                            .setMessage("Yüklü sürüm: " + BuildConfig.VERSION_NAME + (not.isEmpty() ? "\n\nYenilikler: " + not : "") + "\n\nİndirip kurduğunda çizimlerin korunur.")
                            .setPositiveButton("İndir", (d, w) -> { try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) { } })
                            .setNegativeButton("Sonra", null).show();
                    } else if (elle) Toast.makeText(a, "En güncel sürümü kullanıyorsun (" + BuildConfig.VERSION_NAME + ")", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                if (elle) h.post(() -> Toast.makeText(a, "Güncelleme denetlenemedi (internet?)", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
