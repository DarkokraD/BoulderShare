package com.herak.bouldershare.classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.enums.HoldType;

import java.util.ArrayList;
import java.util.List;

import static com.herak.bouldershare.enums.HoldType.REGULAR_HOLD;
import static com.herak.bouldershare.enums.HoldType.START_HOLD;
import static com.herak.bouldershare.enums.HoldType.TOP_HOLD;

/**
 * Created by darko on 23.4.2017..
 */

public class MyView extends View {

    private List<Hold> holds = new ArrayList<Hold>();

    private Paint paint = new Paint();
    private int circleRadius;

    HoldType currentHoldType = START_HOLD;
    Bitmap mBoulderBitmap;

    ScaleGestureDetector scaleGestureDetector;

    long lastScale;

    public MyView(Context context) {
        super(context);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.STROKE);
        this.mBoulderBitmap = ((MainActivity) context).getmBoulderBitmap();
        scaleGestureDetector = new ScaleGestureDetector(context, new MyOnScaleGestureListener(this));
    }



    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBoulderBitmap.copy(Bitmap.Config.ARGB_8888, true), canvas.getWidth(), canvas.getHeight(), true);
        canvas.drawBitmap(scaledBitmap, 0, 0, paint);

        circleRadius = Math.round(canvas.getHeight() / 25f);


        for(Hold hold : holds){
            switch(hold.type){
                case REGULAR_HOLD:
                    paint.setColor(Color.RED);
                    break;
                case START_HOLD:
                    paint.setColor(Color.GREEN);
                    break;
                case TOP_HOLD:
                    paint.setColor(Color.MAGENTA);
                    break;
            }
            canvas.drawCircle(hold.x, hold.y, hold.circleRadius, paint);
        }

    }

    long actionDownTimestamp;
    private final long LONG_PRESS_DURATION = 500;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        if(event.getPointerCount() == 1) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    actionDownTimestamp = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_UP:
                    if(actionDownTimestamp > lastScale) {
                        Hold hold = new Hold(event.getX(), event.getY());
                        hold.type = currentHoldType;
                        hold.circleRadius = circleRadius;

                        Hold existingHold = getExistingHold(hold);
                        if (System.currentTimeMillis() - actionDownTimestamp < LONG_PRESS_DURATION) {
                            //SHORT PRESS
                            if (existingHold != null) {
                                //Hold exists already so remove it
                                removeHold(existingHold);
                            } else {
                                holds.add(hold);
                            }
                            invalidate();
                        } else {
                            //LONG PRESS
                            if (existingHold != null) {
                                changeHoldType(existingHold);
                            } else {
                                holds.add(hold);
                            }
                            invalidate();
                        }
                    }
                    break;
            }
        }
        return true;

    }

    private void changeHoldType(Hold hold) {
        //holds.remove(hold);
        switch(hold.type){
            case REGULAR_HOLD:
                currentHoldType = TOP_HOLD;
                break;
            case START_HOLD:
                currentHoldType = REGULAR_HOLD;
                break;
            case TOP_HOLD:
                currentHoldType = START_HOLD;
                break;
        }
        hold.setType(currentHoldType);
        //holds.add(hold);

    }

    private void removeHold(Hold existingHold) {
        if(holds.contains(existingHold)) holds.remove(existingHold);
    }

    private Hold getExistingHold(Hold clickedHold) {
        for(Hold hold : holds){
            if(clickedHold.distanceFrom(hold) <= hold.circleRadius) return hold;
        }
        return null;
    }

    public Bitmap getBitmap()
    {
        //this.measure(100, 100);
        //this.layout(0, 0, 100, 100);
        this.setDrawingCacheEnabled(true);
        this.buildDrawingCache();
        Bitmap bmp = Bitmap.createBitmap(this.getDrawingCache());
        this.setDrawingCacheEnabled(false);


        return bmp;
    }

    public class MyOnScaleGestureListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener {
        private MyView view;

        public MyOnScaleGestureListener(MyView view){
            this.view = view;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            Hold existingHold = view.getExistingHold(new Hold(focusX, focusY));

            if(existingHold != null) {
                float scaleFactor = detector.getScaleFactor();
                existingHold.circleRadius *= scaleFactor;
                lastScale = System.currentTimeMillis();
                invalidate();
                return true;
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    }
}
