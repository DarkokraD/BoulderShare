package com.herak.bouldershare.classes;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/** Image IO runs on a worker, with durable originals and bounded editing bitmaps. */
public final class ImageFiles {
    private static final int MAX_DIMENSION = 2048;

    private ImageFiles() { }

    public static Bitmap load(Context context, Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream stream = open(context, uri)) {
            BitmapFactory.decodeStream(stream, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw new IOException("Unsupported or empty image");
        }
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > MAX_DIMENSION) {
            options.inSampleSize *= 2;
        }
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap;
        try (InputStream stream = open(context, uri)) {
            bitmap = BitmapFactory.decodeStream(stream, null, options);
        }
        if (bitmap == null) throw new IOException("Unable to decode image");
        int orientation;
        try (InputStream stream = open(context, uri)) {
            orientation = new ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException unsupportedExif) {
            orientation = ExifInterface.ORIENTATION_NORMAL;
        }
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_180: matrix.setRotate(180); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.setScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_90: matrix.setRotate(90); break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90); matrix.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_270: matrix.setRotate(-90); break;
            default: return bitmap;
        }
        Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (oriented != bitmap) bitmap.recycle();
        return oriented;
    }

    public static Uri importPhoto(Context context, Uri source) throws IOException {
        File directory = directory(new File(context.getFilesDir(), "Pictures/Input"));
        File destination = File.createTempFile("wall_", ".image", directory);
        boolean success = false;
        try (InputStream input = open(context, source);
             OutputStream output = new FileOutputStream(destination)) {
            copy(input, output);
            success = true;
            return Uri.fromFile(destination);
        } finally {
            if (!success) destination.delete();
        }
    }

    public static Uri export(Context context, Bitmap bitmap) throws IOException {
        String name = "boulder_" + UUID.randomUUID() + ".jpg";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BoulderShare");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Unable to create gallery image");
            boolean success = false;
            try {
                try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                    write(bitmap, output);
                }
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                if (context.getContentResolver().update(uri, values, null, null) != 1) {
                    throw new IOException("Unable to finish gallery image");
                }
                success = true;
                return uri;
            } finally {
                if (!success) context.getContentResolver().delete(uri, null, null);
            }
        }
        File pictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (pictures == null) throw new IOException("Image storage unavailable");
        File file = new File(directory(new File(pictures, "Output")), name);
        boolean success = false;
        try (OutputStream output = new FileOutputStream(file)) {
            write(bitmap, output);
            success = true;
        } finally {
            if (!success) file.delete();
        }
        MediaScannerConnection.scanFile(context, new String[] {file.getAbsolutePath()}, new String[] {"image/jpeg"}, null);
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    public static Uri shareableUri(Context context, Uri source) throws IOException {
        if (source == null) throw new IOException("No saved image");
        File destination = File.createTempFile("route_", ".jpg", directory(new File(context.getCacheDir(), "images")));
        boolean success = false;
        try (InputStream input = open(context, source);
             OutputStream output = new FileOutputStream(destination)) {
            copy(input, output);
            success = true;
        } finally {
            if (!success) destination.delete();
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", destination);
    }

    /** Read compatibility for stored pre-AndroidX image URIs, also used by Glide. */
    public static Uri readableUri(Context context, Uri source) throws IOException {
        if (source == null) return null;
        File legacy = legacyProviderImage(context, source);
        return legacy == null ? source : Uri.fromFile(legacy);
    }

    public static File cameraFile(Context context) throws IOException {
        return File.createTempFile("camera_", ".jpg", directory(new File(context.getCacheDir(), "images")));
    }

    private static InputStream open(Context context, Uri uri) throws IOException {
        if (uri == null) throw new IOException("Missing image URI");
        InputStream stream;
        if ("file".equals(uri.getScheme())) {
            if (uri.getPath() == null) throw new IOException("Missing image path");
            stream = new FileInputStream(uri.getPath());
        } else {
            try {
                stream = context.getContentResolver().openInputStream(uri);
            } catch (IllegalArgumentException obsoleteProviderRoot) {
                // Prior releases stored our FileProvider URIs in the database. Resolve
                // only their image directories, without reopening broad sharing roots.
                File legacy = legacyProviderImage(context, uri);
                if (legacy == null) throw new IOException("Image URI is unavailable", obsoleteProviderRoot);
                stream = new FileInputStream(legacy);
            }
        }
        if (stream == null) throw new IOException("Image is unavailable");
        return stream;
    }

    private static File legacyProviderImage(Context context, Uri uri) throws IOException {
        if (!(context.getPackageName() + ".fileprovider").equals(uri.getAuthority())) return null;
        java.util.List<String> segments = uri.getPathSegments();
        if (segments.size() < 2) return null;
        File base;
        String root = segments.get(0);
        if ("external".equals(root)) base = Environment.getExternalStorageDirectory();
        else if ("my_images".equals(root)) base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        else if ("BoulderShare Output".equals(root)) base = context.getExternalFilesDir(null);
        else return null;
        if (base == null) return null;
        File candidate = base;
        for (int index = 1; index < segments.size(); index++) candidate = new File(candidate, segments.get(index));
        candidate = candidate.getCanonicalFile();
        if (!candidate.getPath().startsWith(base.getCanonicalPath() + File.separator)) return null;
        if ("external".equals(root)) {
            File pictures = new File(base, "Pictures/BoulderShare Output").getCanonicalFile();
            if (!candidate.getPath().startsWith(pictures.getPath() + File.separator)) return null;
        }
        return candidate;
    }

    private static File directory(File file) throws IOException {
        if (!file.isDirectory() && !file.mkdirs()) throw new IOException("Unable to create image directory");
        return file;
    }

    private static void write(Bitmap bitmap, OutputStream output) throws IOException {
        if (output == null || !bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
            throw new IOException("Unable to write image");
        }
        output.flush();
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[16384];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Image operation cancelled");
            output.write(buffer, 0, count);
        }
        output.flush();
    }
}
