package com.herak.bouldershare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.bitmap;
import static android.os.Build.VERSION_CODES.M;

/**
 * A placeholder fragment containing a simple view.
 */
public class BoulderFragment extends Fragment {

    ImageView mImageView;
    String mCurrentPhotoPath;
    Bitmap mBoulderBitmap;

    public BoulderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_boulder, container, false);
        MainActivity mainActivity = (MainActivity) getActivity();
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.llBoulderFragment);
        MyView myView = new MyView(mainActivity);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        myView.setLayoutParams(layoutParams);

        myView.setMinimumHeight(linearLayout.getHeight());
        myView.setMinimumWidth(linearLayout.getWidth());

        linearLayout.addView(myView);
        mBoulderBitmap = mainActivity.getmBoulderBitmap();
//        mImageView = (DrawView) view.findViewById(R.id.imageBoulder);
//        Bitmap bitmap = mainActivity.getmBoulderBitmap();
//        Bitmap bitmap1 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//        Canvas canvas = new Canvas(bitmap1);
//        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
//        p.setStyle(Paint.Style.STROKE);
//        p.setStrokeWidth(5);
//        p.setColor(Color.RED);
//        canvas.drawCircle(100,100,50, p);
//        mImageView.setImageBitmap(bitmap1);

        return view;
    }

    class MyView extends View{

        private List<Point> points = new ArrayList<Point>();
        Paint paint = new Paint();

        public MyView(Context context) {
            super(context);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(15);
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBoulderBitmap.copy(Bitmap.Config.ARGB_8888, true), canvas.getWidth(), canvas.getHeight(), true);
            canvas.drawBitmap(scaledBitmap, 0, 0, paint);
            for(Point point:points){
                canvas.drawCircle(point.x, point.y, 100, paint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Point point = new Point();
                    point.x = event.getX();
                    point.y = event.getY();
                    points.add(point);
            }
            invalidate();
            return true;

        }

    }
    class Point {
        float x, y;
    }

}
