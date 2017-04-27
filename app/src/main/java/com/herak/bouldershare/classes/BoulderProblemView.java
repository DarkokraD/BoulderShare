package com.herak.bouldershare.classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class BoulderProblemView extends View {

    private List<Hold> holds = new ArrayList<Hold>();

    private Paint paint = new Paint();
    private Paint textPaint = new Paint();
    private int circleRadius;

    HoldType currentHoldType = START_HOLD;
    Bitmap mBoulderBitmap;

    ScaleGestureDetector scaleGestureDetector;
    GestureDetector mGestureDetector;
    boolean scaleOngoing;

    long lastScale;

    private BoulderProblemInfo mBoulderProblemInfo;
    MainActivity mainActivity;



    public BoulderProblemView(Context context) {
        super(context);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.STROKE);
        this.mainActivity = (MainActivity) context;
        this.mBoulderBitmap = mainActivity.getmBoulderBitmap();
        this.mBoulderProblemInfo = mainActivity.getmBoulderProblemInfo();
        scaleGestureDetector = new ScaleGestureDetector(context, new MyOnScaleGestureListener(this));
        mGestureDetector = new GestureDetector(context, new GestureListener());
    }



    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBoulderBitmap.copy(Bitmap.Config.ARGB_8888, true), canvas.getWidth(), canvas.getHeight(), true);
        canvas.drawBitmap(scaledBitmap, 0, 0, paint);
        if(mainActivity != null) {
            mBoulderProblemInfo = mainActivity.getmBoulderProblemInfo();
        }
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

        if(mBoulderProblemInfo != null){

            textPaint.setColor(Color.BLACK);
            textPaint.setAlpha(180);
            textPaint.setTextSize(100);
            if(mBoulderProblemInfo != null){
                LinearLayout layout = new LinearLayout(mainActivity);

                Typeface roboto = Typeface.createFromAsset(mainActivity.getResources().getAssets(), "font/Roboto-Medium.ttf");

                TextView textView = new TextView(mainActivity);
                textView.setText(getInfo(mBoulderProblemInfo));
                textView.setVisibility(View.VISIBLE);
                textView.setBackgroundColor(Color.argb(180, 30, 30, 30));
                textView.setTextColor(Color.parseColor("#45BBDC"));
                textView.setTypeface(roboto);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                textView.setPadding(10, 10, 10, 10);
                textView.setWidth(canvas.getWidth());
                textView.setGravity(Gravity.CENTER);

                layout.addView(textView);
                layout.measure(canvas.getWidth(), canvas.getHeight());
                layout.layout(0, 0, canvas.getWidth(), canvas.getHeight());
//                canvas.translate(0, 0);
                layout.draw(canvas);


                //canvas.drawText(text, 100, 100, textPaint);
            }
        }

    }

    private String getInfo(BoulderProblemInfo boulderProblemInfo){
        String infoText = "";
        if(mBoulderProblemInfo.getName() != null && !mBoulderProblemInfo.getName().equals("")){
            infoText = mBoulderProblemInfo.getName();
        }
        if(mBoulderProblemInfo.getGrade() != null && !mBoulderProblemInfo.getGrade().equals("")){
            if(infoText.length() > 0){
                infoText += " (" + mBoulderProblemInfo.getGrade() + ")";
            }else {
                infoText += mBoulderProblemInfo.getGrade();
            }
        }
        if(mBoulderProblemInfo.getAuthor() != null && !mBoulderProblemInfo.getAuthor().equals("")){
            if(infoText.length() > 0){
                infoText += " by " + mBoulderProblemInfo.getAuthor();
            }else{
                infoText += mBoulderProblemInfo.getAuthor();
            }
        }
        if(mBoulderProblemInfo.getComment() != null && !mBoulderProblemInfo.getComment().equals("")){
            if(infoText.length() > 0){
                infoText += "\n" + mBoulderProblemInfo.getComment();
            }else{
                infoText += mBoulderProblemInfo.getComment();
            }
        }
        return infoText;
    }

    long actionDownTimestamp;
    private final long LONG_PRESS_DURATION = 500;
    private boolean moveInitiated = false;
    private Hold holdBeingMoved;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);


        if(event.getPointerCount() == 1) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    actionDownTimestamp = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_MOVE:
                    Hold currentPosition = new Hold(event.getX(), event.getY());
                    if(holdBeingMoved == null) holdBeingMoved = getExistingHold(currentPosition);
                    if(moveInitiated ||
                            System.currentTimeMillis()-actionDownTimestamp > 200 &&
                            holdBeingMoved != null &&
                            holdBeingMoved.distanceFrom(currentPosition) > 10){
                        holdBeingMoved.x = currentPosition.x;
                        holdBeingMoved.y = currentPosition.y;
                        moveInitiated = true;
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_UP:
//                    if(actionDownTimestamp > lastScale & !moveInitiated) {
//                        Hold hold = new Hold(event.getX(), event.getY());
//                        hold.type = currentHoldType;
//                        hold.circleRadius = circleRadius;
//
//                        Hold existingHold = getExistingHold(hold);
//                        if (System.currentTimeMillis() - actionDownTimestamp < LONG_PRESS_DURATION) {
//                            //SHORT PRESS
//                            if (existingHold != null) {
//                                //Hold exists already so remove it
//                                removeHold(existingHold);
//                            } else {
//                                holds.add(hold);
//                            }
//                            invalidate();
//                        } else {
//                            //LONG PRESS
//                            if (existingHold != null) {
//                                changeHoldType(existingHold);
//                            } else {
//                                holds.add(hold);
//                            }
//                            invalidate();
//                        }
//                    }
                    moveInitiated = false;
                    holdBeingMoved = null;
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

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
        float velocityY) {

            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Hold hold = new Hold(e.getX(), e.getY());
            hold.type = currentHoldType;
            hold.circleRadius = circleRadius;

            Hold existingHold = getExistingHold(hold);

            if (existingHold != null) {
                //Hold exists already so remove it
                removeHold(existingHold);
            } else {
                holds.add(hold);
            }
            invalidate();

            return super.onSingleTapUp(e);

        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Hold hold = new Hold(e.getX(), e.getY());
            hold.type = currentHoldType;
            hold.circleRadius = circleRadius;

            Hold existingHold = getExistingHold(hold);

            if (existingHold != null) {
                changeHoldType(existingHold);
                invalidate();
            }

            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return super.onDown(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
        float distanceX, float distanceY) {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    public class MyOnScaleGestureListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener {
        private BoulderProblemView view;

        public MyOnScaleGestureListener(BoulderProblemView view){
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
            scaleOngoing = true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            scaleOngoing = false;
        }
    }
}
