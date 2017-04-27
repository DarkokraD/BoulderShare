package com.herak.bouldershare.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.herak.bouldershare.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment {

    public MainFragment() {
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_main, container, false);
        setHasOptionsMenu(true);
        TextView textViewInstructions = (TextView) view.findViewById(R.id.textViewInstructions);
        textViewInstructions.setText("Instructions:\n" +
                "\n" +
                "- Select a photo of a climbing wall from the gallery or take a photo\n" +
                "- Add a hold by taping on the photo\n" +
                "- Remove a hold by taping inside of an existing hold\n" +
                "- Change the hold type by double taping inside of an existing hold (next hold will be of the same type)\n" +
                "- Resize the hold with a pinching gesture inside an existing hold\n" +
                "- Move the hold by pressing inside the hold for 200ms and moving the finger around\n" +
                "- Once you're done share the image with your climbing buddies by clicking the Share icon");

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
        getActivity().findViewById(R.id.fabCamera).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.fabGallery).setVisibility(View.VISIBLE);
    }
}
