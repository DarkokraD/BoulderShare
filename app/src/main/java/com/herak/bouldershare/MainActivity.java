package com.herak.bouldershare;

import android.content.Context;
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

    String mCurrentPhotoPath;

    public Bitmap getmBoulderBitmap() {
        return mBoulderBitmap;
    }

    public void setmBoulderBitmap(Bitmap bitmap){
        this.mBoulderBitmap = bitmap;
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
                nextFragment = FRAGMENT_TYPE.BOULDER_FRAGMENT;
            }
            catch (Exception e) {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        }else if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                try {
                    ExifInterface exif = new ExifInterface(imageUri.getPath());
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    int rotationInDegrees = exifToDegrees(orientation);
                    Matrix matrix = new Matrix();
                    if (rotation != 0f) {
                        matrix.preRotate(rotationInDegrees);
                    }

                    mBoulderBitmap = Bitmap.createBitmap(selectedImage, 0, 0, mBoulderBitmap.getWidth(), mBoulderBitmap.getHeight(), matrix, true); // rotating bitmap
                    nextFragment = FRAGMENT_TYPE.BOULDER_FRAGMENT;
                }
                catch (Exception e) {
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
        FloatingActionButton fabGallery = (FloatingActionButton) findViewById(R.id.fabCamera);
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
        if (id == R.id.action_share) {
            // save bitmap to cache directory
            Context context = this;
            mBoulderBitmap = ((MyView) findViewById(R.id.myView)).getBitmap();
            try {

                File cachePath = new File(context.getCacheDir(), "images");
                cachePath.mkdirs(); // don't forget to make the directory
                FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
                mBoulderBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

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
