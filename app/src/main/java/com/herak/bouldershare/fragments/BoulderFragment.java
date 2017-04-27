package com.herak.bouldershare.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class BoulderFragment extends Fragment {

    Bitmap mBoulderBitmap;

    public BoulderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_boulder, container, false);
        setHasOptionsMenu(true);

        MainActivity mainActivity = (MainActivity) getActivity();
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.llBoulderFragment);
        BoulderProblemView myView = new BoulderProblemView(mainActivity);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        myView.setLayoutParams(layoutParams);
        myView.setId(R.id.boulderProblemView);

        myView.setMinimumHeight(linearLayout.getHeight());
        myView.setMinimumWidth(linearLayout.getWidth());
        myView.setLongClickable(true);


        linearLayout.addView(myView);
        mBoulderBitmap = mainActivity.getmBoulderBitmap();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_share).setVisible(true);
        menu.findItem(R.id.action_info).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final MainActivity mainActivity = (MainActivity) getActivity();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_info) {
            DialogFragment dialog = new InfoFragment();
            dialog.show(mainActivity.getSupportFragmentManager(), "Settings Fragment");

        }else if (id == R.id.action_share) {

            mainActivity.checkAndGetWritePermission();

            final Context context = mainActivity;
            mBoulderBitmap = ((BoulderProblemView) mainActivity.findViewById(R.id.boulderProblemView)).getBitmap();

            AsyncTask fileTask = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "BoulderShare Output");

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
                    MediaScannerConnection.scanFile(mainActivity, new String[] { pictureFile.toString() }, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("ExternalStorage", "Scanned " + path + ":");
                                    Log.i("ExternalStorage", "-> uri=" + uri);
                                }
                            });

                    Uri contentUri = FileProvider.getUriForFile(context, "com.herak.bouldershare.fileprovider", pictureFile);

                    if (contentUri != null) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                        shareIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
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
}
