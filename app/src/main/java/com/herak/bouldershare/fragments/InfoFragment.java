package com.herak.bouldershare.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;

import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnInfoStoredListener} interface
 * to handle interaction events.
 * Use the {@link InfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InfoFragment extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int MAX_USERNAME_LENGTH = 20;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnInfoStoredListener mListener;
    private SharedPreferences settings;
    private SharedPreferences.Editor settingsEditor;
    private String username;
    private BoulderProblemInfo mBoulderProblemInfo;

    public InfoFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        MainActivity mainActivity = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.fragment_info, null);
        settings = mainActivity.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        settingsEditor = settings.edit();

        final NumberPicker gradePicker = (NumberPicker) view.findViewById(R.id.pickerGrade);
        final List<String> gradesArray = Arrays.asList(getResources().getStringArray(R.array.grades_array));
        gradePicker.setMinValue(0);
        gradePicker.setMaxValue(gradesArray.size()-1);
        gradePicker.setDisplayedValues(getResources().getStringArray(R.array.grades_array));

        mBoulderProblemInfo = mainActivity.getmBoulderProblemInfo();
        if(mBoulderProblemInfo != null) {
            EditText etAuthor = (EditText) view.findViewById(R.id.etInfoAuthor);
            if (mBoulderProblemInfo.getAuthor() != null && !mBoulderProblemInfo.getAuthor().equals("")) {
                etAuthor.setText(mBoulderProblemInfo.getAuthor());
            } else {
                etAuthor.setText(settings.getString("username", null));
            }

            EditText etName = (EditText) view.findViewById(R.id.etInfoProblemName);
            etName.setText(mBoulderProblemInfo.getName());

            EditText etComment = (EditText) view.findViewById(R.id.etInfoComment);
            etComment.setText(mBoulderProblemInfo.getComment());

            if (mBoulderProblemInfo.getGrade() != null && !mBoulderProblemInfo.getGrade().equals("")) {
                gradePicker.setValue(gradesArray.indexOf(mBoulderProblemInfo.getGrade()));
            }
        }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog mDialog = (Dialog) dialog;
                        mBoulderProblemInfo = new BoulderProblemInfo();

                        EditText etSettingsUsername = (EditText) mDialog.findViewById(R.id.etInfoAuthor);
                        username = etSettingsUsername.getText().toString();
                        settingsEditor.putString("username", username).commit();
                        mBoulderProblemInfo.setAuthor(username);

                        EditText etName = (EditText) mDialog.findViewById(R.id.etInfoProblemName);
                        mBoulderProblemInfo.setName(etName.getText().toString());

                        EditText etComment = (EditText) mDialog.findViewById(R.id.etInfoComment);
                        mBoulderProblemInfo.setComment(etComment.getText().toString());

                        mBoulderProblemInfo.setGrade(gradesArray.get(gradePicker.getValue()));

                        onSaveInfo(mBoulderProblemInfo);
                        dialog.dismiss();

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        InfoFragment.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InfoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InfoFragment newInstance(String param1, String param2) {
        InfoFragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_info, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onSaveInfo(BoulderProblemInfo info ) {
        if (mListener != null) {
            mListener.onInfoStoredInteraction(info);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnInfoStoredListener) {
            mListener = (OnInfoStoredListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnInfoStoredListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnInfoStoredListener {
        // TODO: Update argument type and name
        void onInfoStoredInteraction(BoulderProblemInfo info);
    }
}
