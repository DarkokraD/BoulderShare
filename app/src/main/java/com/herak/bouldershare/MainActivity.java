package com.herak.bouldershare;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.herak.bouldershare.classes.MyView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.R.attr.rotation;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int RESULT_LOAD_IMG = 2;
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    String mCurrentPhotoPath;
    private Bitmap mBoulderBitmap;
    private FRAGMENT_TYPE nextFragment = null;

    public Bitmap getmBoulderBitmap() {
        return mBoulderBitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File file = new File(mCurrentPhotoPath);
            Uri uri = Uri.fromFile(file);
            mBoulderBitmap = BitmapFactory.decodeFile(uri.getPath());            //MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            final MainActivity self = this;


            checkAndGetWritePermission();

            AsyncTask fileTask = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "BoulderShare" + File.separator + "Input Photos");

                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    long time = System.currentTimeMillis() / 1000;
                    String name = "boulder_"+ time +".jpg";
                    File pictureFile = new File(directory, name);
                    try {
                        pictureFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        FileOutputStream out = new FileOutputStream(pictureFile);
                        mBoulderBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Tell the media scanner about the new file so that it is
                    // immediately available to the user.
                    MediaScannerConnection.scanFile(self, new String[] { pictureFile.toString() }, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("ExternalStorage", "Scanned " + path + ":");
                                    Log.i("ExternalStorage", "-> uri=" + uri);
                                }
                            });
                    return null;
                }
            };
            fileTask.execute();

            try {
                ExifInterface exif = new ExifInterface(uri.getPath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationInDegrees = exifToDegrees(orientation);
                Matrix matrix = new Matrix();
                if (rotation != 0f) {
                    matrix.preRotate(rotationInDegrees);
                }

                mBoulderBitmap = Bitmap.createBitmap(mBoulderBitmap, 0, 0, mBoulderBitmap.getWidth(), mBoulderBitmap.getHeight(), matrix, true); // rotating bitmap
                nextFragment = FRAGMENT_TYPE.BOULDER_FRAGMENT;
            }
            catch (Exception e) {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        }else if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                try {
                    mBoulderBitmap = modifyOrientation(selectedImage, imageStream);
                    nextFragment = FRAGMENT_TYPE.BOULDER_FRAGMENT;
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }

    public static Bitmap modifyOrientation(Bitmap bitmap, InputStream imageStream) throws IOException {

        ExifInterface ei = new ExifInterface(imageStream);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int rotateBecauseOfWidthHeightRatio = 0;

        if(bitmap.getWidth() > bitmap.getHeight())
            rotateBecauseOfWidthHeightRatio = 90;

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90 + rotateBecauseOfWidthHeightRatio);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180 + rotateBecauseOfWidthHeightRatio);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270 + rotateBecauseOfWidthHeightRatio);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return rotate(bitmap, 0 + rotateBecauseOfWidthHeightRatio);
        }
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    //Create an intent to take a picture, launch the appropriate activity and retrieve the
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                System.err.println(ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.herak.bouldershare.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nextFragment != null){
            changeFragment(nextFragment);
            nextFragment = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fabCamera = (FloatingActionButton) findViewById(R.id.fabCamera);
        FloatingActionButton fabGallery = (FloatingActionButton) findViewById(R.id.fabGallery);
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        fabGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);

                //TODO check if there is a way to open this specific folder for the user to pick from
//                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "BoulderShare" + File.separator + "Input Photos");
//                Uri uri = FileProvider.getUriForFile(context, "com.herak.bouldershare.fileprovider", directory);
//                photoPickerIntent.setDataAndType(uri, "image/*");
            }
        });

        changeFragment(FRAGMENT_TYPE.MAIN_FRAGMENT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final MainActivity self = this;

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_share) {
            checkAndGetWritePermission();
            final Context context = this;
            mBoulderBitmap = ((MyView) findViewById(R.id.myView)).getBitmap();

            AsyncTask fileTask = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "BoulderShare" + File.separator + "Created Problems");

                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String name = "boulder_"+ timeStamp +".jpg";
                    File pictureFile = new File(directory, name);
                    try {
                        pictureFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        FileOutputStream out = new FileOutputStream(pictureFile);
                        mBoulderBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Tell the media scanner about the new file so that it is
                    // immediately available to the user.
                    MediaScannerConnection.scanFile(self, new String[] { pictureFile.toString() }, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("ExternalStorage", "Scanned " + path + ":");
                                    Log.i("ExternalStorage", "-> uri=" + uri);
                                }
                            });

                    File imagePath = new File(context.getCacheDir(), "images");
                    File newFile = new File(imagePath, "image.png");
                    Uri contentUri = FileProvider.getUriForFile(context, "com.herak.bouldershare.fileprovider", newFile);

                    if (contentUri != null) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                        shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        startActivity(Intent.createChooser(shareIntent, "Choose an app"));
                    }
                    return null;
                }
            };
            fileTask.execute();
            // save bitmap to cache directory

//            try {
//
//                File cachePath = new File(context.getExternalFilesDir("BoulderShare Problem"), "images");
//                cachePath.mkdirs(); // don't forget to make the directory
//                FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
//                mBoulderBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                stream.close();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            File imagePath = new File(context.getCacheDir(), "images");
//            File newFile = new File(imagePath, "image.png");
//            Uri contentUri = FileProvider.getUriForFile(context, "com.herak.bouldershare.fileprovider", newFile);
//
//            if (contentUri != null) {
//                Intent shareIntent = new Intent();
//                shareIntent.setAction(Intent.ACTION_SEND);
//                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
//                shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
//                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
//                startActivity(Intent.createChooser(shareIntent, "Choose an app"));
//            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public enum FRAGMENT_TYPE {MAIN_FRAGMENT, BOULDER_FRAGMENT};
    public final String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT_TAG";
    public final String BOULDER_FRAGMENT_TAG = "BOULDER_FRAGMENT_TAG";

    public void changeFragment(FRAGMENT_TYPE newFragmentType)
    {

        FragmentTransaction transaction;
        transaction = getSupportFragmentManager().beginTransaction();

        if(newFragmentType == FRAGMENT_TYPE.MAIN_FRAGMENT)
        {
            transaction.replace(R.id.flayoutMainActivity, new MainFragment(), MAIN_FRAGMENT_TAG);
            findViewById(R.id.fabCamera).setVisibility(View.VISIBLE);
            findViewById(R.id.fabGallery).setVisibility(View.VISIBLE);
        }
        else if(newFragmentType == FRAGMENT_TYPE.BOULDER_FRAGMENT)
        {
            transaction.replace(R.id.flayoutMainActivity, new BoulderFragment(), BOULDER_FRAGMENT_TAG);
            findViewById(R.id.fabCamera).setVisibility(View.INVISIBLE);
            findViewById(R.id.fabGallery).setVisibility(View.INVISIBLE);
        }
//        else if(newFragmentType == FRAGMENT_TYPE.SETTINGS)
//        {
//            transaction.replace(R.id.flayoutMainActivity,new SettingsFragment());
//        }


        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack(null);
        transaction.commit();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void checkAndGetWritePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }
}
