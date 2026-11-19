package com.azat.cizim;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/* Paylaşılacak resmi diğer uygulamalara veren küçük dosya sağlayıcı. Yalnızca önbellekteki "paylas" klasöründeki
   .png/.jpg dosyalarını, yalnızca okuma için açar (dışa kapalı, grantUriPermissions). */
public class Dosya extends ContentProvider {
    static Uri uri(Context c, File f) { return Uri.parse("content://" + c.getPackageName() + ".dosya/" + Uri.encode(f.getName())); }
    private File dosya(Uri u) throws FileNotFoundException {
        String ad = u.getLastPathSegment();
        if (ad == null || ad.contains("/") || !(ad.endsWith(".png") || ad.endsWith(".jpg"))) throw new FileNotFoundException();
        File f = new File(new File(getContext().getCacheDir(), "paylas"), ad);
        if (!f.exists()) throw new FileNotFoundException();
        return f;
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri u) { String p = u.getLastPathSegment(); return p != null && p.endsWith(".jpg") ? "image/jpeg" : "image/png"; }
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
