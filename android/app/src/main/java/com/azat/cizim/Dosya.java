package com.azat.cizim;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/* Küçük dosya sağlayıcı, yalnızca okuma, dışa kapalı (grantUriPermissions):
   content://…dosya/<ad>      → önbellekteki "paylas" klasörü (png/jpg/gif/mp4/webm) — Paylaş
   content://…dosya/apk/<ad>  → uygulamanın indirme klasöründeki .apk — güncelleme kurulumu */
public class Dosya extends ContentProvider {
    static Uri uri(Context c, File f) { return Uri.parse("content://" + c.getPackageName() + ".dosya/" + Uri.encode(f.getName())); }
    private static String tur(String ad) {
        if (ad.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (ad.endsWith(".jpg")) return "image/jpeg";
        if (ad.endsWith(".gif")) return "image/gif";
        if (ad.endsWith(".mp4")) return "video/mp4";
        if (ad.endsWith(".webm")) return "video/webm";
        if (ad.endsWith(".png")) return "image/png";
        return null;
    }
    private File dosya(Uri u) throws FileNotFoundException {
        List<String> s = u.getPathSegments();
        if (s == null || s.isEmpty()) throw new FileNotFoundException();
        String ad = s.get(s.size() - 1);
        if (ad == null || ad.contains("/") || ad.contains("..") || tur(ad) == null) throw new FileNotFoundException();
        File klasor;
        if (s.size() == 2 && "apk".equals(s.get(0)) && ad.endsWith(".apk")) {
            klasor = getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (klasor == null) klasor = getContext().getFilesDir();
        } else if (s.size() == 1 && !ad.endsWith(".apk")) klasor = new File(getContext().getCacheDir(), "paylas");
        else throw new FileNotFoundException();
        File f = new File(klasor, ad);
        if (!f.exists()) throw new FileNotFoundException();
        return f;
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri u) { String p = u.getLastPathSegment(); String t = p == null ? null : tur(p); return t == null ? "application/octet-stream" : t; }
    @Override public ParcelFileDescriptor openFile(Uri u, String mod) throws FileNotFoundException {
        return ParcelFileDescriptor.open(dosya(u), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri u, String[] s, String w, String[] a, String o) {
        try { File f = dosya(u); MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
              c.addRow(new Object[]{f.getName(), f.length()}); return c; } catch (Exception e) { return null; }
    }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String w, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String w, String[] a) { return 0; }
}
