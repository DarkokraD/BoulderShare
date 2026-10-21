package com.herak.bouldershare.fragments;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.BoulderProblemView;
import com.herak.bouldershare.classes.ImageFiles;
import com.herak.bouldershare.classes.Hold;
import com.herak.bouldershare.classes.MarkerAppearance;
import com.herak.bouldershare.classes.MarkerSettings;
import com.herak.bouldershare.classes.EditorSettings;
import com.herak.bouldershare.data.BoulderRepository;
import com.herak.bouldershare.enums.HoldType;

import java.util.Objects;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One save/export pipeline shared by Save and Share, retained across rotation. */
public class BoulderFragment extends Fragment {
    private BoulderProblemView editor;
    private SaveModel saveModel;
    private TextView gestureHint;
    private TextView smartStatus;
    private MaterialSwitch smartMarkingSwitch;
    private MaterialButton fitPhotoButton, resizeHoldButton;
    private MaterialButton panelToggle;
    private TextView legendStart, legendRegular, legendTop;
    private View editorControls, controlsContent, sizePanel;
    private boolean panelExpanded, showInstructions;
    private BoulderProblemView.SmartMarkingStatus latestSmartStatus = BoulderProblemView.SmartMarkingStatus.IDLE;
    private Slider sizeSlider;
    private Hold resizingHold;
    private int originalRadius, largestRadius;
    private float initialSizeValue;
    private OnBackPressedCallback resizeBackCallback;

    public boolean isBusy() { return saveModel != null && saveModel.busy; }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        setHasOptionsMenu(true);
        saveModel = new ViewModelProvider(this).get(SaveModel.class);
        View root = inflater.inflate(R.layout.fragment_boulder, container, false);
        editor = new BoulderProblemView(requireActivity());
        editor.setId(R.id.boulderProblemView);
        editor.setEnabled(!saveModel.busy);
        ((RelativeLayout) root.findViewById(R.id.rlBoulderFragment)).addView(editor,
                new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        gestureHint = root.findViewById(R.id.editor_gesture_hint);
        smartStatus = root.findViewById(R.id.smart_marking_status);
        smartMarkingSwitch = root.findViewById(R.id.smart_marking);
        Context context = requireContext();
        SharedPreferences preferences = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        smartMarkingSwitch.setChecked(preferences.getBoolean("smart_marking", true));
        editor.setSmartMarkingEnabled(smartMarkingSwitch.isChecked());
        editor.setOnSmartMarkingStatusListener(this::showSmartMarkingStatus);
        smartMarkingSwitch.setOnCheckedChangeListener((button, enabled) -> {
            editor.flushPendingSingleTap();
            editor.setSmartMarkingEnabled(enabled);
            preferences.edit().putBoolean("smart_marking", enabled).apply();
        });
        fitPhotoButton = root.findViewById(R.id.action_fit_photo);
        resizeHoldButton = root.findViewById(R.id.action_resize_hold);
        editorControls = root.findViewById(R.id.editor_controls);
        controlsContent = root.findViewById(R.id.editor_controls_content);
        panelToggle = root.findViewById(R.id.action_toggle_editor_controls);
        panelExpanded = EditorSettings.isPanelExpanded(context);
        showInstructions = EditorSettings.showInstructions(context);
        panelToggle.setOnClickListener(view -> {
            panelExpanded = !panelExpanded;
            EditorSettings.setPanelExpanded(requireContext(), panelExpanded);
            if (!panelExpanded) {
                editor.cancelTypeAssignment();
                editor.flushPendingSingleTap();
                editor.cancelHoldResize();
            }
            updateEditorControls();
        });
        legendStart = root.findViewById(R.id.legend_start);
        legendRegular = root.findViewById(R.id.legend_regular);
        legendTop = root.findViewById(R.id.legend_top);
        legendStart.setOnClickListener(view -> selectHoldType(HoldType.START_HOLD));
        legendRegular.setOnClickListener(view -> selectHoldType(HoldType.REGULAR_HOLD));
        legendTop.setOnClickListener(view -> selectHoldType(HoldType.TOP_HOLD));
        sizePanel = root.findViewById(R.id.hold_size_panel);
        sizeSlider = sizePanel.findViewById(R.id.hold_size);
        sizeSlider.setLabelBehavior(LabelFormatter.LABEL_GONE);
        sizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (resizingHold != null) editor.previewHoldResize(resizingHold, selectedRadius());
        });
        sizePanel.findViewById(R.id.hold_size_cancel).setOnClickListener(view -> closeHoldSizePanel(false));
        sizePanel.findViewById(R.id.hold_size_apply).setOnClickListener(view -> closeHoldSizePanel(true));
        fitPhotoButton.setOnClickListener(view -> {
            editor.cancelTypeAssignment();
            editor.flushPendingSingleTap();
            editor.cancelHoldResize();
            editor.resetViewport();
            updateEditorControls();
        });
        resizeHoldButton.setOnClickListener(view -> {
            if (editor.isChoosingHoldToResize()) editor.cancelHoldResize();
            else if (!editor.isImageReady()) {
                Toast.makeText(requireContext(), R.string.image_loading, Toast.LENGTH_SHORT).show();
            } else if (!editor.requestHoldResize()) {
                Toast.makeText(requireContext(), R.string.add_hold_first, Toast.LENGTH_SHORT).show();
            }
            updateEditorControls();
        });
        editor.setOnHoldResizeRequestedListener(hold -> {
            showHoldSizePanel(hold);
        });
        editor.setOnHoldTypeChangedListener((type, assigning) -> updateEditorControls());
        updateEditorControls();
        return root;
    }

    @Override public void onResume() {
        super.onResume();
        if (editor == null) return;
        panelExpanded = EditorSettings.isPanelExpanded(requireContext());
        showInstructions = EditorSettings.showInstructions(requireContext());
        MarkerAppearance previous = editor.getMarkerAppearance();
        MarkerAppearance current = MarkerSettings.load(requireContext());
        editor.setMarkerAppearance(current);
        if ((previous.strokeWidth != current.strokeWidth || previous.borderWidth != current.borderWidth
                || previous.startColor != current.startColor || previous.regularColor != current.regularColor
                || previous.topColor != current.topColor) && !editor.getBoulderProblemInfo().getHolds().isEmpty()) {
            ((MainActivity) requireActivity()).setHasUnsavedChanges(true);
        }
        updateLegend(R.id.legend_start, current.startColor);
        updateLegend(R.id.legend_regular, current.regularColor);
        updateLegend(R.id.legend_top, current.topColor);
        updateEditorControls();
    }

    private void selectHoldType(HoldType type) {
        if (saveModel.busy || resizingHold != null) return;
        editor.selectHoldType(type);
        updateEditorControls();
    }

    private void updateLegend(int id, int color) {
        TextView label = requireView().findViewById(id);
        GradientDrawable swatch = new GradientDrawable();
        swatch.setShape(GradientDrawable.OVAL);
        float density = getResources().getDisplayMetrics().density;
        swatch.setColor(color);
        swatch.setStroke(Math.max(1, Math.round(density)),
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.on_surface));
        int size = Math.round(14 * density);
        swatch.setSize(size, size);
        label.setCompoundDrawablesRelativeWithIntrinsicBounds(swatch, null, null, null);
    }

    @Override public void onViewCreated(View view, Bundle state) {
        resizeBackCallback = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { closeHoldSizePanel(false); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), resizeBackCallback);
        saveModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            MainActivity activity = (MainActivity) requireActivity();
            BoulderProblemInfo current = activity.getmBoulderProblemInfo();
            if (!Objects.equals(current == null ? null : current.getInputBitmapUri(), result.info.getInputBitmapUri())) {
                saveModel.result.setValue(null);
                return;
            }
            editor.setEnabled(!saveModel.busy);
            updateEditorControls();
            activity.invalidateOptionsMenu();
            if (result.pending) return;
            // Consume once: recreation must not launch the same chooser again.
            saveModel.result.setValue(null);
            if (result.saved) {
                current.setId(result.info.getId());
                current.setFinalBitmapUri(result.info.getFinalBitmapUri());
                activity.setHasUnsavedChanges(false);
                activity.setSaveIconVisibility(false);
                Toast.makeText(activity, R.string.image_saved, Toast.LENGTH_SHORT).show();
            }
            if (result.error) {
                Toast.makeText(activity, R.string.image_save_failed, Toast.LENGTH_LONG).show();
                return;
            }
            if (result.shareUri != null) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/jpeg");
                share.putExtra(Intent.EXTRA_STREAM, result.shareUri);
                share.setClipData(ClipData.newUri(activity.getContentResolver(), getString(R.string.share_image), result.shareUri));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, getString(R.string.share_image)));
            }
        });
    }

    private void updateEditorControls() {
        if (editor == null) return;
        boolean choosing = editor.isChoosingHoldToResize();
        gestureHint.setText(choosing ? R.string.editor_resize_hint : editor.isAssigningHoldType()
                ? R.string.editor_assign_type_hint : R.string.editor_gesture_hint);
        gestureHint.setVisibility(showInstructions ? View.VISIBLE : View.GONE);
        resizeHoldButton.setText(choosing ? R.string.cancel : R.string.resize_hold);
        fitPhotoButton.setEnabled(!saveModel.busy);
        resizeHoldButton.setEnabled(!saveModel.busy);
        smartMarkingSwitch.setEnabled(!saveModel.busy);
        panelToggle.setEnabled(!saveModel.busy);
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        controlsContent.setVisibility(panelExpanded ? View.VISIBLE : View.GONE);
        panelToggle.setContentDescription(getString(panelExpanded
                ? R.string.editor_controls_collapse : R.string.editor_controls_expand));
        panelToggle.setIconResource(panelExpanded ? R.drawable.ic_editor_collapse : R.drawable.ic_editor_expand);
        panelToggle.setText(landscape && !panelExpanded ? "" : getString(panelExpanded
                ? R.string.editor_controls_collapse : R.string.editor_controls_expand));
        if (landscape) {
            ViewGroup.LayoutParams params = editorControls.getLayoutParams();
            params.width = Math.round((panelExpanded ? 280 : 56) * getResources().getDisplayMetrics().density);
            editorControls.setLayoutParams(params);
        }
        updateLegendSelection(legendStart, HoldType.START_HOLD);
        updateLegendSelection(legendRegular, HoldType.REGULAR_HOLD);
        updateLegendSelection(legendTop, HoldType.TOP_HOLD);
        showSmartMarkingStatus(latestSmartStatus);
        boolean resizing = resizingHold != null;
        editorControls.setVisibility(resizing ? View.GONE : View.VISIBLE);
        sizePanel.setVisibility(resizing ? View.VISIBLE : View.GONE);
        if (resizeBackCallback != null) resizeBackCallback.setEnabled(resizing);
        requireActivity().invalidateOptionsMenu();
    }

    private void updateLegendSelection(TextView label, HoldType type) {
        label.setSelected(editor.getCurrentHoldType() == type);
        label.setActivated(label.isSelected() && editor.isAssigningHoldType());
        label.setEnabled(!saveModel.busy);
    }

    private void showSmartMarkingStatus(BoulderProblemView.SmartMarkingStatus status) {
        latestSmartStatus = status;
        if (smartStatus == null) return;
        if (!showInstructions && status != BoulderProblemView.SmartMarkingStatus.DETECTING
                && status != BoulderProblemView.SmartMarkingStatus.UNAVAILABLE) {
            smartStatus.setVisibility(View.GONE);
            return;
        }
        int message;
        switch (status) {
            case DETECTING: message = R.string.smart_marking_detecting; break;
            case FITTED: message = R.string.smart_marking_fitted; break;
            case MANUAL_FALLBACK: message = R.string.smart_marking_manual; break;
            case UNAVAILABLE: message = showInstructions ? R.string.smart_marking_unavailable
                    : R.string.smart_marking_unavailable_status; break;
            default:
                if (editor == null || !editor.isSmartMarkingEnabled()) {
                    smartStatus.setVisibility(View.GONE);
                    return;
                }
                message = R.string.smart_marking_hint;
        }
        smartStatus.setText(message);
        smartStatus.setVisibility(View.VISIBLE);
    }

    private void showHoldSizePanel(Hold hold) {
        if (editor == null || saveModel.busy) return;
        largestRadius = Math.max(2, Math.round(editor.getBoulderProblemInfo().getReferenceHeight() / 3f));
        // Give small markers more slider travel, while leaving an untouched size exact.
        originalRadius = hold.getCircleRadius();
        initialSizeValue = Math.max(0, Math.min(100,
                (float) Math.sqrt(Math.max(0, (originalRadius - 1f) / (largestRadius - 1))) * 100));
        sizeSlider.setValue(initialSizeValue);
        resizingHold = hold;
        updateEditorControls();
    }

    private int selectedRadius() {
        float fraction = sizeSlider.getValue() / 100f;
        return sizeSlider.getValue() == initialSizeValue ? originalRadius
                : Math.round(1 + fraction * fraction * (largestRadius - 1));
    }

    private void closeHoldSizePanel(boolean apply) {
        if (editor == null || resizingHold == null) return;
        if (apply) editor.resizeHold(resizingHold, selectedRadius());
        resizingHold = null;
        editor.cancelHoldResize();
        updateEditorControls();
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MainActivity activity = (MainActivity) requireActivity();
        menu.findItem(R.id.action_info).setVisible(true).setEnabled(!saveModel.busy);
        menu.findItem(R.id.action_share).setVisible(true).setEnabled(!saveModel.busy);
        menu.findItem(R.id.action_save).setVisible(activity.isHasUnsavedChanges()).setEnabled(!saveModel.busy);
        menu.findItem(R.id.action_settings).setEnabled(!saveModel.busy);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            if (!saveModel.busy) new InfoFragment().show(getParentFragmentManager(), "route_info");
            return true;
        }
        if (item.getItemId() == R.id.action_save || item.getItemId() == R.id.action_share) {
            if (saveModel.busy) return true;
            if (!editor.isImageReady()) {
                Toast.makeText(requireContext(), R.string.image_loading, Toast.LENGTH_SHORT).show();
                return true;
            }
            MainActivity activity = (MainActivity) requireActivity();
            closeHoldSizePanel(true);
            editor.flushPendingSingleTap();
            editor.cancelTypeAssignment();
            editor.setEnabled(false);
            BoulderProblemInfo info = BoulderProblemInfo.fromBundle(editor.getBoulderProblemInfo().toBundle());
            boolean needsSave = activity.isHasUnsavedChanges() || info.getFinalBitmapUri() == null || info.getId() == null;
            saveModel.start(requireContext().getApplicationContext(), info, activity.getmBoulderBitmap(),
                    needsSave, item.getItemId() == R.id.action_share, editor.getMarkerAppearance());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Kept for existing callers; persistence belongs on an IO executor. */
    public long addOrUpdateBoulderProblem(BoulderProblemInfo info) {
        return BoulderRepository.save(requireContext().getApplicationContext(), info);
    }

    @Override public void onDestroyView() {
        if (editor != null) {
            editor.cancelHoldResize();
            editor.setOnHoldResizeRequestedListener(null);
            editor.setOnSmartMarkingStatusListener(null);
            editor.setOnHoldTypeChangedListener(null);
        }
        resizingHold = null;
        editorControls = controlsContent = sizePanel = null;
        panelToggle = null;
        legendStart = legendRegular = legendTop = null;
        sizeSlider = null;
        resizeBackCallback = null;
        editor = null;
        gestureHint = null;
        smartStatus = null;
        smartMarkingSwitch = null;
        fitPhotoButton = resizeHoldButton = null;
        super.onDestroyView();
    }

    public static class SaveModel extends ViewModel {
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        final MutableLiveData<SaveResult> result = new MutableLiveData<>();
        volatile boolean busy;

        void start(Context context, BoulderProblemInfo info, Bitmap bitmap, boolean needsSave, boolean share) {
            start(context, info, bitmap, needsSave, share, MarkerSettings.load(context));
        }

        void start(Context context, BoulderProblemInfo info, Bitmap bitmap, boolean needsSave,
                   boolean share, MarkerAppearance appearance) {
            if (busy) return;
            busy = true;
            result.setValue(new SaveResult(info, true, false, false, null));
            executor.execute(() -> {
                boolean saved = false;
                try {
                    Uri shareUri = null;
                    // A saved image can predate global appearance settings changed in the library.
                    boolean regenerate = needsSave || share;
                    if (regenerate) {
                        Bitmap export = BoulderProblemView.render(bitmap, info, appearance);
                        try { info.setFinalBitmapUri(ImageFiles.export(context, export)); }
                        finally { export.recycle(); }
                        BoulderRepository.save(context, info);
                        saved = true;
                    }
                    if (share && shareUri == null) shareUri = ImageFiles.shareableUri(context, info.getFinalBitmapUri());
                    busy = false;
                    result.postValue(new SaveResult(info, false, false, saved, shareUri));
                } catch (Exception error) {
                    busy = false;
                    result.postValue(new SaveResult(info, false, true, saved, null));
                }
            });
        }

        @Override protected void onCleared() { executor.shutdown(); }
    }

    static class SaveResult {
        final BoulderProblemInfo info;
        final boolean pending, error, saved;
        final Uri shareUri;
        SaveResult(BoulderProblemInfo info, boolean pending, boolean error, boolean saved, Uri shareUri) {
            this.info = info; this.pending = pending; this.error = error; this.saved = saved; this.shareUri = shareUri;
        }
    }
}
