package com.herak.bouldershare.fragments;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.BoulderProblemView;
import com.herak.bouldershare.classes.Hold;
import com.herak.bouldershare.data.BoulderContract;
import com.herak.bouldershare.data.BoulderProvider;

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
    Context mContext = getContext();

    public BoulderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_boulder, container, false);
        setHasOptionsMenu(true);

        MainActivity mainActivity = (MainActivity) getActivity();
        RelativeLayout relativeLayout = (RelativeLayout) view.findViewById(R.id.rlBoulderFragment);
        relativeLayout.setGravity(Gravity.CENTER);
        BoulderProblemView myView = new BoulderProblemView(mainActivity);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        myView.setLayoutParams(layoutParams);
        myView.setId(R.id.boulderProblemView);

        mBoulderBitmap = mainActivity.getmBoulderBitmap();

        myView.setMinimumHeight(relativeLayout.getHeight());
        myView.setMinimumWidth(relativeLayout.getWidth());
        myView.setLongClickable(true);


        relativeLayout.addView(myView);

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

            final Bitmap resultBitmap = mBoulderBitmap.copy(Bitmap.Config.ARGB_8888, true);

            Canvas canvas = new Canvas(resultBitmap);
            canvas = ((BoulderProblemView) mainActivity.findViewById(R.id.boulderProblemView)).drawOnCustomCanvas(canvas);


            BoulderProblemView bpView = (BoulderProblemView) mainActivity.findViewById(R.id.boulderProblemView);
            final BoulderProblemInfo boulderProblemInfo = bpView.getBoulderProblemInfo();


            final Context context = mainActivity;
            //mBoulderBitmap = ((BoulderProblemView) mainActivity.findViewById(R.id.boulderProblemView)).getBitmap();

            AsyncTask fileTask = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "BoulderShare Output");
                    Object result = null;

                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    File pictureFile;
                    if(boulderProblemInfo.getFinalBitmapUri() != null){
                        pictureFile = new File(boulderProblemInfo.getFinalBitmapUri().getPath());
                    }else {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        String name = "boulder_" + timeStamp + ".jpg";
                        pictureFile = new File(directory, name);
                    }

                    try {
                        pictureFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        FileOutputStream out = new FileOutputStream(pictureFile);
                        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.close();
                        result = "Image saved";
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
                        boulderProblemInfo.setFinalBitmapUri(contentUri);
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                        shareIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        startActivity(Intent.createChooser(shareIntent, "Choose an app"));
                    }
                    return result;
                }

                @Override
                protected void onPostExecute(Object o) {
                    if(o != null){
                        Toast.makeText(context, R.string.image_saved, Toast.LENGTH_LONG).show();
                    }
                    addBoulderProblem(boulderProblemInfo);
                    super.onPostExecute(o);
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

    public long addBoulderProblem(BoulderProblemInfo info){
//        BoulderProvider boulderProvider = new BoulderProvider();
        long boulderProblemId;
        mContext = getContext();
        Cursor boulderCursor = null;
        if(info.getId() != null) {
            boulderCursor = mContext.getContentResolver().query(
                    BoulderContract.BoulderProblemInfoEntry.CONTENT_URI,
                    new String[]{BoulderContract.BoulderProblemInfoEntry._ID},
                    BoulderContract.BoulderProblemInfoEntry._ID + " = ?",
                    new String[]{Long.toString(info.getId())},
                    null);
        }

        if (boulderCursor != null && boulderCursor.moveToFirst()) {
            int locationIdIndex = boulderCursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry._ID);
            boulderProblemId = boulderCursor.getLong(locationIdIndex);
        } else {

            ContentValues values = new ContentValues();

            values.put(BoulderContract.BoulderProblemInfoEntry.COLUMN_AUTHOR, info.getAuthor());
            values.put(BoulderContract.BoulderProblemInfoEntry.COLUMN_COMMENT, info.getComment());
            values.put(BoulderContract.BoulderProblemInfoEntry.COLUMN_GRADE, info.getGrade());
            values.put(BoulderContract.BoulderProblemInfoEntry.COLUMN_NAME, info.getName());
            values.put(BoulderContract.BoulderProblemInfoEntry.COLUMN_INPUTBITMAPURI, info.getInputBitmapUri().toString());
            values.put(BoulderContract.BoulderProblemInfoEntry.COLUMN_FINALBITMAPURI, info.getFinalBitmapUri().toString());

            Uri returnUri = mContext.getContentResolver().insert(BoulderContract.BoulderProblemInfoEntry.CONTENT_URI, values);
            boulderProblemId = Long.parseLong(BoulderContract.HoldsEntry.getBoulderProblemIdFromUri(returnUri));
            info.setId(boulderProblemId);

            ContentValues[] valuesArray = new ContentValues[info.getHolds().size()];
            for(Hold hold:info.getHolds()){
                ContentValues holdValues = new ContentValues();
                holdValues.put(BoulderContract.HoldsEntry.COLUMN_BOULDER_PROBLEM_ID, info.getId());
                holdValues.put(BoulderContract.HoldsEntry.COLUMN_CIRCLE_RADIUS, hold.getCircleRadius());
                holdValues.put(BoulderContract.HoldsEntry.COLUMN_COORD_X, hold.getX());
                holdValues.put(BoulderContract.HoldsEntry.COLUMN_COORD_Y, hold.getY());
                holdValues.put(BoulderContract.HoldsEntry.COLUMN_HOLD_TYPE, hold.getType().toString());
                valuesArray[info.getHolds().indexOf(hold)] = holdValues;
            }
            int rowsInserted = mContext.getContentResolver().bulkInsert(BoulderContract.HoldsEntry.CONTENT_URI, valuesArray);

        }
        return boulderProblemId;
    }
}
