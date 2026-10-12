package com.herak.bouldershare.adapters;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.ImageFiles;

import java.io.IOException;

/** Cursor ownership belongs to MainFragment; Glide handles recycled thumbnail requests. */
public class BoulderGridViewAdapter extends CursorAdapter {
    public BoulderGridViewAdapter(Context context, Cursor cursor) { super(context, cursor, 0); }

    @Override public void bindView(View view, Context context, Cursor cursor) {
        ImageView image = view.findViewById(R.id.boulderImageView);
        String uri = cursor.getString(cursor.getColumnIndexOrThrow("final_bitmap_uri"));
        String original = cursor.getString(cursor.getColumnIndexOrThrow("input_bitmap_uri"));
        Glide.with(image).load(readable(context, uri)).centerCrop()
                .placeholder(R.drawable.ic_wall)
                .error(Glide.with(image).load(readable(context, original)).centerCrop()
                        .placeholder(R.drawable.ic_wall).error(R.drawable.ic_wall))
                .into(image);
        String title = value(cursor, "name");
        String grade = value(cursor, "grade"), author = value(cursor, "author");
        if (title.isEmpty()) title = context.getString(R.string.untitled_problem);
        ((TextView) view.findViewById(R.id.boulderImageViewText)).setText(title);
        ((TextView) view.findViewById(R.id.boulder_grade)).setText(grade.isEmpty()
                ? context.getString(R.string.route_grade_unknown) : grade);
        TextView setter = view.findViewById(R.id.boulder_setter);
        setter.setText(author.isEmpty() ? "" : context.getString(R.string.route_setter_format, author));
        setter.setVisibility(author.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.boulder_grid_item, parent, false);
    }

    private static String value(Cursor cursor, String column) {
        String value = cursor.getString(cursor.getColumnIndexOrThrow(column));
        return value == null ? "" : value.trim();
    }

    private static Uri readable(Context context, String value) {
        if (value == null) return null;
        try { return ImageFiles.readableUri(context, Uri.parse(value)); }
        catch (IOException | SecurityException unavailable) { return null; }
    }
}
