package com.herak.bouldershare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;

import androidx.exifinterface.media.ExifInterface;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.BoulderProblemView;
import com.herak.bouldershare.classes.Hold;
import com.herak.bouldershare.classes.ImageFiles;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ImageFilesInstrumentedTest {
    @Test public void legacyThumbnailUriMapsOnlyItsOriginalOutputDirectory() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Uri oldOutput = new Uri.Builder().scheme("content").authority(context.getPackageName() + ".fileprovider")
                .appendPath("external").appendPath("Pictures").appendPath("BoulderShare Output")
                .appendPath("thumbnail_fixture.jpg").build();
        File expected = new File(Environment.getExternalStorageDirectory(), "Pictures/BoulderShare Output/thumbnail_fixture.jpg");
        assertEquals(Uri.fromFile(expected.getCanonicalFile()), ImageFiles.readableUri(context, oldOutput));
        Uri outside = oldOutput.buildUpon().path("/external/Documents/private.txt").build();
        assertEquals(outside, ImageFiles.readableUri(context, outside));
        Uri foreign = oldOutput.buildUpon().authority("another.app.fileprovider").build();
        assertEquals(foreign, ImageFiles.readableUri(context, foreign));
    }

    @Test public void landscapePhotoStaysLandscapeAndExifRotationIsApplied() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File file = photo(context, 120, 80);
        Bitmap normal = null, rotated = null;
        try {
            normal = ImageFiles.load(context, Uri.fromFile(file));
            assertEquals(120, normal.getWidth()); assertEquals(80, normal.getHeight());
            ExifInterface exif = new ExifInterface(file);
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(ExifInterface.ORIENTATION_ROTATE_90));
            exif.saveAttributes();
            rotated = ImageFiles.load(context, Uri.fromFile(file));
            assertEquals(80, rotated.getWidth()); assertEquals(120, rotated.getHeight());
            assertTrue(Color.red(rotated.getPixel(40, 20)) > Color.blue(rotated.getPixel(40, 20)));
            assertTrue(Color.blue(rotated.getPixel(40, 100)) > Color.red(rotated.getPixel(40, 100)));
        } finally {
            if (normal != null) normal.recycle();
            if (rotated != null) rotated.recycle();
            file.delete();
        }
    }

    @Test public void largePhotosAreDecodedWithinEditingBound() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File file = photo(context, 4096, 1024);
        Bitmap loaded = null;
        try {
            loaded = ImageFiles.load(context, Uri.fromFile(file));
            assertTrue(Math.max(loaded.getWidth(), loaded.getHeight()) <= 2048);
            assertEquals(4f, (float) loaded.getWidth() / loaded.getHeight(), .01f);
        } finally { if (loaded != null) loaded.recycle(); file.delete(); }
    }

    @Test public void importedOriginalAndShareCopyOutliveTheirSource() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File source = photo(context, 120, 80);
        File importedFile = null;
        Bitmap decoded = null;
        Uri share = null;
        try {
            Uri imported = ImageFiles.importPhoto(context, Uri.fromFile(source));
            importedFile = new File(imported.getPath());
            assertTrue(importedFile.getCanonicalPath().startsWith(new File(context.getFilesDir(), "Pictures/Input").getCanonicalPath() + File.separator));
            assertTrue(source.delete());
            decoded = ImageFiles.load(context, imported);
            assertEquals(120, decoded.getWidth());
            share = ImageFiles.shareableUri(context, imported);
            assertEquals("content", share.getScheme());
            assertEquals(context.getPackageName() + ".fileprovider", share.getAuthority());
            assertEquals("image/jpeg", context.getContentResolver().getType(share));
            assertTrue(importedFile.delete());
            try (InputStream stream = context.getContentResolver().openInputStream(share)) {
                assertNotNull(stream);
                assertTrue(stream.read() != -1);
            }
        } finally {
            if (decoded != null) decoded.recycle();
            source.delete();
            if (importedFile != null) importedFile.delete();
            if (share != null) context.getContentResolver().delete(share, null, null);
        }
    }

    @Test public void exportProjectsMarkersOntoSourceWithoutChangingTheOriginal() {
        Bitmap source = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888);
        source.eraseColor(Color.BLACK);
        BoulderProblemInfo info = new BoulderProblemInfo();
        info.setReferenceSize(1200, 800);
        ArrayList<Hold> holds = new ArrayList<>();
        holds.add(new Hold(450, 200, 24));
        info.setHolds(holds);
        Bitmap rendered = BoulderProblemView.render(source, info);
        try {
            assertEquals(Color.BLACK, source.getPixel(237, 100));
            int outline = rendered.getPixel(237, 100);
            assertTrue(Color.green(outline) > Color.red(outline));
            assertTrue(Color.green(outline) > Color.blue(outline));
        } finally { rendered.recycle(); source.recycle(); }
    }

    private static File photo(Context context, int width, int height) throws Exception {
        File file = File.createTempFile("image_test_", ".jpg", context.getCacheDir());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try {
            bitmap.eraseColor(Color.BLUE);
            Paint paint = new Paint(); paint.setColor(Color.RED);
            new Canvas(bitmap).drawRect(0, 0, width / 2f, height, paint);
            try (FileOutputStream output = new FileOutputStream(file)) {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output));
            }
        } finally { bitmap.recycle(); }
        return file;
    }
}
