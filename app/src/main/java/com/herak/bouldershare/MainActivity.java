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
import android.view.View;
import android.widget.Toast;

import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.fragments.BoulderFragment;
import com.herak.bouldershare.fragments.InfoFragment;
import com.herak.bouldershare.fragments.MainFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.R.attr.rotation;

public class MainActivity extends AppCompatActivity
    implements InfoFragment.OnInfoStoredListener {

    public static final String PREFS_NAME = "BoulderSharePrefs";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int RESULT_LOAD_IMG = 2;
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    String mCurrentPhotoPath;
    private Bitmap mBoulderBitmap;

    private boolean hasUnsavedChanges = false;

    private FRAGMENT_TYPE nextFragment = null;
    private Menu menu;

    public void setNextFragment(FRAGMENT_TYPE nextFragment) {
        this.nextFragment = nextFragment;
    }

    private BoulderProblemInfo mBoulderProblemInfo;

    public void setmBoulderBitmap(Bitmap mBoulderBitmap) {
        this.mBoulderBitmap = mBoulderBitmap;
    }

    public BoulderProblemInfo getmBoulderProblemInfo() {
        return mBoulderProblemInfo;
    }

    public void setmBoulderProblemInfo(BoulderProblemInfo mBoulderProblemInfo) {
        this.mBoulderProblemInfo = mBoulderProblemInfo;
    }



    private Uri mBoulderBitmapUri;

    public Bitmap getmBoulderBitmap() {
        return mBoulderBitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File file = new File(mCurrentPhotoPath);
            final Uri uri = Uri.fromFile(file);
            mBoulderBitmap = BitmapFactory.decodeFile(uri.getPath());            //MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            final Context context = this;

            checkAndGetWritePermission();

            AsyncTask fileTask = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "BoulderShare Input");

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
                        ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                        int rotationInDegrees = exifToDegrees(orientation);
                        Matrix matrix = new Matrix();
                        if (rotation != 0f) {
                            matrix.preRotate(rotationInDegrees);
                        }

                        mBoulderBitmap = Bitmap.createBitmap(mBoulderBitmap, 0, 0, mBoulderBitmap.getWidth(), mBoulderBitmap.getHeight(), matrix, true);
                        mBoulderBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Tell the media scanner about the new file so that it is
                    // immediately available to the user.
                    MediaScannerConnection.scanFile(context, new String[] { pictureFile.toString() }, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("ExternalStorage", "Scanned " + path + ":");
                                    Log.i("ExternalStorage", "-> uri=" + uri);
                                }
                            });

                    mBoulderProblemInfo = new BoulderProblemInfo();
                    mBoulderProblemInfo.setInputBitmapUri(Uri.fromFile(pictureFile));
                    mBoulderBitmapUri = Uri.fromFile(pictureFile);

                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    MainActivity mainActivity = (MainActivity) context;

                    mainActivity.changeFragment(FRAGMENT_TYPE.BOULDER_FRAGMENT);
                }
            };

            fileTask.execute();


        }else if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                mBoulderBitmapUri = imageUri;
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
//                try {
//                    mBoulderBitmap = modifyOrientation(selectedImage, imageStream);
                    mBoulderBitmap = selectedImage;
                    nextFragment = FRAGMENT_TYPE.BOULDER_FRAGMENT;
                    mBoulderProblemInfo = new BoulderProblemInfo();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
//                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(this, "You haven't picked an image",Toast.LENGTH_LONG).show();
        }
    }

    public static Bitmap modifyOrientation(Bitmap bitmap, InputStream imageStream) throws IOException {

        ExifInterface ei = new ExifInterface(imageStream);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        Bitmap retBitmap;

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                retBitmap = rotate(bitmap, 90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                retBitmap = rotate(bitmap, 180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                retBitmap = rotate(bitmap, 270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                retBitmap = flip(bitmap, true, false);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                retBitmap = flip(bitmap, false, true);
                break;
            default:
                retBitmap = rotate(bitmap, 0);
        }

        int rotateBecauseOfWidthHeightRatio = 0;
        if(retBitmap.getWidth() > retBitmap.getHeight())
            rotateBecauseOfWidthHeightRatio = 90;
        if(rotateBecauseOfWidthHeightRatio != 0){
            retBitmap = rotate(retBitmap, rotateBecauseOfWidthHeightRatio);
        }

        return retBitmap;
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

        // To get this out of the way
        checkAndGetWritePermission();

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
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onInfoStoredInteraction(BoulderProblemInfo info) {
        mBoulderProblemInfo = info;
        findViewById(R.id.boulderProblemView).invalidate();
    }


    public enum FRAGMENT_TYPE {MAIN_FRAGMENT, BOULDER_FRAGMENT, SETTINGS_FRAGMENT};
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
//        else if(newFragmentType == FRAGMENT_TYPE.SETTINGS_FRAGMENT)
//        {
//
//        }

        //changes cannot exist once the fragment is loaded since all would be discarded if another was loaded anyway
        hasUnsavedChanges = false;

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

    public void checkAndGetWritePermission() {
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

    public Uri getmBoulderBitmapUri() {
        return mBoulderBitmapUri;
    }

    public void setmBoulderBitmapUri(Uri mBoulderBitmapUri) {
        this.mBoulderBitmapUri = mBoulderBitmapUri;
    }

    public boolean isHasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    public void setHasUnsavedChanges(boolean hasUnsavedChanges) {
        this.hasUnsavedChanges = hasUnsavedChanges;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public void setSaveIconVisibility(boolean isVisible){
        menu.findItem(R.id.action_save).setVisible(isVisible);
    }

    public void setShareIconVisibility(boolean isVisible){
        menu.findItem(R.id.action_share).setVisible(isVisible);
    }


}
