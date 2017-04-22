package com.herak.bouldershare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.R.attr.rotation;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;


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
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    String mCurrentPhotoPath;

    public Bitmap getmBoulderBitmap() {
        return mBoulderBitmap;
    }

    private Bitmap mBoulderBitmap;
    private FRAGMENT_TYPE nextFragment = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File file = new File(mCurrentPhotoPath);
            Uri uri = Uri.fromFile(file);
            mBoulderBitmap = BitmapFactory.decodeFile(uri.getPath());            //MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            try {
                ExifInterface exif = new ExifInterface(uri.getPath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationInDegrees = exifToDegrees(orientation);
                Matrix matrix = new Matrix();
                if (rotation != 0f) {
                    matrix.preRotate(rotationInDegrees);
                }

                mBoulderBitmap = Bitmap.createBitmap(mBoulderBitmap, 0, 0, mBoulderBitmap.getWidth(), mBoulderBitmap.getHeight(), matrix, true); // rotating bitmap
            }
            catch (Exception e) {

            }

            nextFragment = FRAGMENT_TYPE.BOULDER_FRAGMENT;


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



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fabCamera);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
        }
        else if(newFragmentType == FRAGMENT_TYPE.BOULDER_FRAGMENT)
        {
            transaction.replace(R.id.flayoutMainActivity, new BoulderFragment(), BOULDER_FRAGMENT_TAG);
            findViewById(R.id.fabCamera).setVisibility(View.INVISIBLE);
        }
//        else if(newFragmentType == FRAGMENT_TYPE.SETTINGS)
//        {
//            transaction.replace(R.id.flayoutMainActivity,new SettingsFragment());
//        }


        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack(null);
        transaction.commit();

    }
}
