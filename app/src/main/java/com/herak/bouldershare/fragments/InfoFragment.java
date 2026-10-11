package com.herak.bouldershare.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class InfoFragment extends DialogFragment {
    private OnInfoStoredListener listener;

    @Override public Dialog onCreateDialog(Bundle state) {
        MainActivity activity = (MainActivity) requireActivity();
        View view = activity.getLayoutInflater().inflate(R.layout.fragment_info, null);
        SharedPreferences settings = activity.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        BoulderProblemInfo info = activity.getmBoulderProblemInfo();
        EditText author = view.findViewById(R.id.etInfoAuthor);
        EditText name = view.findViewById(R.id.etInfoProblemName);
        EditText comment = view.findViewById(R.id.etInfoComment);
        NumberPicker grade = view.findViewById(R.id.pickerGrade);
        List<String> grades = Arrays.asList(getResources().getStringArray(R.array.grades_array));
        grade.setMinValue(0); grade.setMaxValue(grades.size() - 1);
        grade.setDisplayedValues(getResources().getStringArray(R.array.grades_array));
        if (info != null) {
            author.setText(info.getAuthor() == null || info.getAuthor().isEmpty()
                    ? settings.getString("username", "") : info.getAuthor());
            name.setText(info.getName()); comment.setText(info.getComment());
            grade.setValue(Math.max(0, grades.indexOf(info.getGrade())));
        }
        return new MaterialAlertDialogBuilder(activity).setTitle(R.string.route_details)
                .setView(view).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (info == null) return;
                    String newAuthor = author.getText().toString().trim();
                    String newName = name.getText().toString().trim();
                    String newComment = comment.getText().toString().trim();
                    String newGrade = grades.get(grade.getValue());
                    boolean changed = !Objects.equals(newAuthor, info.getAuthor())
                            || !Objects.equals(newName, info.getName())
                            || !Objects.equals(newComment, info.getComment())
                            || !Objects.equals(newGrade, info.getGrade());
                    settings.edit().putString("username", newAuthor).apply();
                    if (changed) {
                        info.setAuthor(newAuthor); info.setName(newName);
                        info.setComment(newComment); info.setGrade(newGrade);
                        activity.setHasUnsavedChanges(true);
                        activity.setSaveIconVisibility(true);
                        onSaveInfo(info);
                    }
                }).create();
    }

    public static InfoFragment newInstance(String param1, String param2) { return new InfoFragment(); }
    public void onSaveInfo(BoulderProblemInfo info) { if (listener != null) listener.onInfoStoredInteraction(info); }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof OnInfoStoredListener)) throw new IllegalStateException("Activity must handle route information");
        listener = (OnInfoStoredListener) context;
    }
    @Override public void onDetach() { listener = null; super.onDetach(); }
    public interface OnInfoStoredListener { void onInfoStoredInteraction(BoulderProblemInfo info); }
}
