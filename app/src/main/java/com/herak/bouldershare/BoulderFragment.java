package com.herak.bouldershare;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.herak.bouldershare.classes.MyView;
import com.herak.bouldershare.enums.HoldType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.bitmap;
import static com.herak.bouldershare.enums.HoldType.REGULAR_HOLD;
import static com.herak.bouldershare.enums.HoldType.START_HOLD;
import static com.herak.bouldershare.enums.HoldType.TOP_HOLD;

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
        MyView myView = new MyView(mainActivity);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        myView.setLayoutParams(layoutParams);
        myView.setId(R.id.myView);

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
    }






}
