package com.herak.bouldershare.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.herak.bouldershare.R;
import com.herak.bouldershare.data.BoulderContract;

import java.io.IOException;

/**
 * Created by darko on 19.8.2017..
 */

public class BoulderGridViewAdapter extends CursorAdapter {
    private LayoutInflater cursorInflater;

    public BoulderGridViewAdapter(Context context, Cursor data) {
        super(context,data, 0);
        cursorInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);


    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final int THUMB_SIZE = 300;
        //TODO figure out how this works on different screensizes and make it responsive
        TextView boulderImageViewText = (TextView) view.findViewById(R.id.boulderImageViewText);
        ImageView boulderImageView = (ImageView) view.findViewById(R.id.boulderImageView);
        if(cursor.getString( cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_FINALBITMAPURI) ) != null) {
//            boulderImageView.setImageURI(Uri.parse(cursor.getString(cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_FINALBITMAPURI))));
            Uri finalBitmapUri = Uri.parse(cursor.getString(cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_FINALBITMAPURI)));
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(),finalBitmapUri);

                boulderImageView.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, THUMB_SIZE*3, THUMB_SIZE*4));
                boulderImageView.layout(0,0,THUMB_SIZE*3,THUMB_SIZE*4);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            File file = new File(finalBitmapUri.getPath());
//            System.out.println("File je: " + file + "######################");
//            Glide.with(context).load("file://"+file).into(boulderImageView);
        }

        Typeface roboto = Typeface.createFromAsset(context.getResources().getAssets(), "font/Roboto-Medium.ttf");

        boulderImageViewText.setText(getTitle(cursor));
        boulderImageViewText.setBackgroundColor(Color.argb(180, 30, 30, 30));
        boulderImageViewText.setTextColor(Color.parseColor("#45BBDC"));
        boulderImageViewText.setTypeface(roboto);
        boulderImageViewText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (int) (16));
        boulderImageViewText.setPadding(10, 10, 10, 10);

        boulderImageViewText.setGravity(Gravity.CENTER);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return cursorInflater.inflate(R.layout.boulder_grid_item, parent, false);
    }

    private String getTitle(Cursor cursor){
        String name = cursor.getString( cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_NAME) );
        String grade = cursor.getString( cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_GRADE) );
        String author = cursor.getString( cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_AUTHOR) );
        String title = "";

        if(name != null && !name.equals("")){
            title = name;
        }
        if(grade != null && !grade.equals("")){
            if(title.length() > 0){
                title += " (" + grade + ")";
            }else {
                title += grade;
            }
        }
        if(author != null && !author.equals("")){
            if(title.length() > 0){
                title += "\nby " + author;
            }else{
                title += author;
            }
        }
        return title;
    }


}
