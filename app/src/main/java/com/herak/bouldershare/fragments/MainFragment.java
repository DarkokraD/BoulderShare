package com.herak.bouldershare.fragments;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.adapters.BoulderGridViewAdapter;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.Hold;
import com.herak.bouldershare.data.BoulderContract;
import com.herak.bouldershare.enums.HoldType;

import java.util.ArrayList;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment {

    public MainFragment() {
    }

    BoulderGridViewAdapter mBoulderGridViewAdapter;
    Context mContext = getContext();
    MainActivity mainActivity = (MainActivity) getActivity();
    GridView mBoulderGrid;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {






        View view = inflater.inflate(R.layout.fragment_main, container, false);
        setHasOptionsMenu(true);
        mBoulderGrid = (GridView) view.findViewById(R.id.gridBoulderProblems);
//        TextView textViewInstructions = (TextView) view.findViewById(textViewInstructions);
//        textViewInstructions.setText("Instructions:\n" +
//                "\n" +
//                "- Select a photo of a climbing wall from the gallery or take a photo\n" +
//                "- Add a hold by tapping on the photo\n" +
//                "- Remove a hold by taping inside of an existing hold\n" +
//                "- Change the hold type by double tapping inside of an existing hold (next hold will be of the same type)\n" +
//                "- Resize the hold with a pinching gesture inside an existing hold\n" +
//                "- Move the hold by pressing inside the hold for 200ms and moving the finger around\n" +
//                "- Click the information icon and fill in the information in the dialog to add some information on the boulder problem\n" +
//                "- Once you're done share the image with your climbing buddies by clicking the Share icon");
        mBoulderGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor cursor = ((BoulderGridViewAdapter) parent.getAdapter()).getCursor();
                cursor.moveToPosition(position);
                BoulderProblemInfo boulder = new BoulderProblemInfo();
                boulder.setId(cursor.getLong(cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry._ID)));
                boulder.setAuthor(cursor.getString(cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_AUTHOR)));
                boulder.setName(cursor.getString(cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_NAME)));
                boulder.setComment(cursor.getString(cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_COMMENT)));
                boulder.setGrade(cursor.getString(cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_GRADE)));
                boulder.setInputBitmapUri(Uri.parse(cursor.getString(cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_INPUTBITMAPURI))));
                boulder.setFinalBitmapUri(Uri.parse(cursor.getString(cursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry.COLUMN_FINALBITMAPURI))));

                Cursor holdsCursor = getContext().getContentResolver().query(BoulderContract.HoldsEntry.buildHoldsOfBoulderProblem(boulder.getId()), null, null, null, null);
                List<Hold> holds = new ArrayList<Hold>();
                while(holdsCursor.moveToNext()){
                    Hold hold = new Hold(
                            holdsCursor.getFloat(holdsCursor.getColumnIndex(BoulderContract.HoldsEntry.COLUMN_COORD_X)),
                            holdsCursor.getFloat(holdsCursor.getColumnIndex(BoulderContract.HoldsEntry.COLUMN_COORD_Y))
                    );
                    hold.setCircleRadius(holdsCursor.getInt(holdsCursor.getColumnIndex(BoulderContract.HoldsEntry.COLUMN_CIRCLE_RADIUS)));
                    hold.setType(HoldType.valueOf(holdsCursor.getString(holdsCursor.getColumnIndex(BoulderContract.HoldsEntry.COLUMN_HOLD_TYPE))));
                    hold.setId(holdsCursor.getLong(holdsCursor.getColumnIndex(BoulderContract.HoldsEntry._ID)));
                    hold.setBoulderId(holdsCursor.getLong(holdsCursor.getColumnIndex(BoulderContract.HoldsEntry.COLUMN_BOULDER_PROBLEM_ID)));
                    holds.add(hold);
                }
                boulder.setHolds(holds);

                ((MainActivity) getActivity()).setmBoulderProblemInfo(boulder);
                ((MainActivity) getActivity()).changeFragment(MainActivity.FRAGMENT_TYPE.BOULDER_FRAGMENT);

            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_share).setVisible(false);
        menu.findItem(R.id.action_info).setVisible(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mContext = getContext();
        Cursor boulderCursor = mContext.getContentResolver().query(
                BoulderContract.BoulderProblemInfoEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        mBoulderGridViewAdapter = new BoulderGridViewAdapter(mContext, boulderCursor);
        mBoulderGrid.setAdapter(mBoulderGridViewAdapter);

        getActivity().findViewById(R.id.fabCamera).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.fabGallery).setVisibility(View.VISIBLE);
//        ((TextView) getActivity().findViewById(textViewInstructions)).setText("Id of first boulder:" + id + "\n" +
//                "Instructions:\n" +
//                "\n" +
//                "- Select a photo of a climbing wall from the gallery or take a photo\n" +
//                "- Add a hold by tapping on the photo\n" +
//                "- Remove a hold by taping inside of an existing hold\n" +
//                "- Change the hold type by double tapping inside of an existing hold (next hold will be of the same type)\n" +
//                "- Resize the hold with a pinching gesture inside an existing hold\n" +
//                "- Move the hold by pressing inside the hold for 200ms and moving the finger around\n" +
//                "- Click the information icon and fill in the information in the dialog to add some information on the boulder problem\n" +
//                "- Once you're done share the image with your climbing buddies by clicking the Share icon");
//
    }


}
