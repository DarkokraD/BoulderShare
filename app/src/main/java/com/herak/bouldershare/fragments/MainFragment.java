package com.herak.bouldershare.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import com.herak.bouldershare.R;
import com.herak.bouldershare.adapters.BoulderGridViewAdapter;
import com.herak.bouldershare.data.BoulderContract;



/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment {

    public MainFragment() {
    }

    BoulderGridViewAdapter mBoulderGridViewAdapter;
    Context mContext = getContext();
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
//        String[] projection = {
//                BoulderContract.BoulderProblemInfoEntry.COLUMN_AUTHOR,
//                BoulderContract.BoulderProblemInfoEntry.COLUMN_COMMENT,
//                BoulderContract.BoulderProblemInfoEntry.COLUMN_GRADE,
//                BoulderContract.BoulderProblemInfoEntry.COLUMN_INPUTBITMAPURI,
//                BoulderContract.BoulderProblemInfoEntry.COLUMN_NAME,
//        };

        mContext = getContext();
        Cursor boulderCursor = mContext.getContentResolver().query(
                BoulderContract.BoulderProblemInfoEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        mBoulderGridViewAdapter = new BoulderGridViewAdapter(mContext, boulderCursor);
        mBoulderGrid.setAdapter(mBoulderGridViewAdapter);

        long id = -1;
        if(boulderCursor.moveToNext()){
            id = boulderCursor.getLong(boulderCursor.getColumnIndex(BoulderContract.BoulderProblemInfoEntry._ID));
        }
//        boulderCursor.close();

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
