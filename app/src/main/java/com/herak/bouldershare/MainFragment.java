package com.herak.bouldershare;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

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



        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_share).setVisible(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().findViewById(R.id.fabCamera).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.fabGallery).setVisibility(View.VISIBLE);
    }
}
