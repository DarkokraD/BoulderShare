package com.herak.bouldershare.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;
import com.herak.bouldershare.R;
import com.herak.bouldershare.SettingsActivity;
import com.herak.bouldershare.classes.MarkerColors;
import com.herak.bouldershare.enums.HoldType;

/** A cancelable, recreation-safe RGB picker and validated six-digit hex editor. */
public class MarkerColorDialog extends DialogFragment {
    public static final String RESULT = "marker_color_result";
    private int color;
    private boolean updating;
    private EditText hex;
    private TextInputLayout hexLayout;
    private View swatch;
    private Slider red, green, blue;

    public static MarkerColorDialog create(HoldType type, int color) {
        MarkerColorDialog dialog = new MarkerColorDialog();
        Bundle args = new Bundle(); args.putString("type", type.name()); args.putInt("color", color);
        dialog.setArguments(args); return dialog;
    }

    @Override public Dialog onCreateDialog(Bundle state) {
        color = state == null ? requireArguments().getInt("color") : state.getInt("draft_color");
        String draft = state == null ? MarkerColors.hex(color) : state.getString("draft_hex", MarkerColors.hex(color));
        View content = getLayoutInflater().inflate(R.layout.dialog_marker_color, null);
        hex = content.findViewById(R.id.marker_color_hex);
        hexLayout = content.findViewById(R.id.marker_color_hex_layout);
        hexLayout.setHelperText(getString(R.string.marker_color_hex_hint));
        swatch = content.findViewById(R.id.marker_color_swatch);
        red = channel(content, R.id.marker_color_red, R.string.marker_color_red);
        green = channel(content, R.id.marker_color_green, R.string.marker_color_green);
        blue = channel(content, R.id.marker_color_blue, R.string.marker_color_blue);
        hex.setText(draft);
        updatePicker();
        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updating) return;
                Integer parsed = MarkerColors.parseHex(s.toString());
                if (parsed != null) { color = parsed; updatePicker(); }
                validate();
            }
            @Override public void afterTextChanged(Editable value) { }
        });
        HoldType type = HoldType.valueOf(requireArguments().getString("type"));
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.marker_color_title, getString(SettingsActivity.typeLabel(type))))
                .setView(content).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.apply, null).create();
    }

    private Slider channel(View content, int id, int label) {
        Slider slider = content.findViewById(id); slider.setValueFrom(0); slider.setValueTo(255);
        slider.setStepSize(1); slider.setContentDescription(getString(label));
        slider.addOnChangeListener((view, value, fromUser) -> {
            if (!fromUser || updating) return;
            color = Color.rgb(Math.round(red.getValue()), Math.round(green.getValue()), Math.round(blue.getValue()));
            updating = true; hex.setText(MarkerColors.hex(color)); updating = false;
            updatePicker(); validate();
        });
        return slider;
    }

    private void updatePicker() {
        updating = true;
        red.setValue(Color.red(color)); green.setValue(Color.green(color)); blue.setValue(Color.blue(color));
        GradientDrawable background = new GradientDrawable();
        background.setColor(color); background.setCornerRadius(12 * getResources().getDisplayMetrics().density);
        background.setStroke(Math.max(1, Math.round(getResources().getDisplayMetrics().density)),
                ContextCompat.getColor(requireContext(), R.color.outline));
        swatch.setBackground(background);
        swatch.setContentDescription(getString(R.string.marker_color_swatch, MarkerColors.hex(color)));
        updating = false;
    }

    private boolean validate() {
        boolean valid = MarkerColors.parseHex(hex.getText().toString()) != null;
        hexLayout.setError(valid ? null : getString(R.string.marker_color_hex_error));
        Dialog dialog = getDialog();
        if (dialog instanceof AlertDialog && ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(valid);
        }
        return valid;
    }

    @Override public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) requireDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (!validate()) return;
            Bundle result = new Bundle(); result.putString("type", requireArguments().getString("type"));
            result.putInt("color", MarkerColors.parseHex(hex.getText().toString()));
            getParentFragmentManager().setFragmentResult(RESULT, result); dismiss();
        });
        validate();
    }

    @Override public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("draft_color", color);
        state.putString("draft_hex", hex == null ? MarkerColors.hex(color) : hex.getText().toString());
    }
    @Override public void onDestroyView() {
        hex = null; hexLayout = null; swatch = null; red = green = blue = null;
        super.onDestroyView();
    }
}
