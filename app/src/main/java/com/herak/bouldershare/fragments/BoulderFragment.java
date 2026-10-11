package com.herak.bouldershare.fragments;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.BoulderProblemView;
import com.herak.bouldershare.classes.ImageFiles;
import com.herak.bouldershare.classes.Hold;
import com.herak.bouldershare.data.BoulderRepository;

import java.util.Objects;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One save/export pipeline shared by Save and Share, retained across rotation. */
public class BoulderFragment extends Fragment {
    private BoulderProblemView editor;
    private SaveModel saveModel;
    private TextView gestureHint;
    private MaterialButton fitPhotoButton, resizeHoldButton;
    private AlertDialog sizeDialog;

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
        fitPhotoButton = root.findViewById(R.id.action_fit_photo);
        resizeHoldButton = root.findViewById(R.id.action_resize_hold);
        fitPhotoButton.setOnClickListener(view -> {
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
            updateEditorControls();
            showHoldSizeDialog(hold);
        });
        updateEditorControls();
        return root;
    }

    @Override public void onViewCreated(View view, Bundle state) {
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
        gestureHint.setText(choosing ? R.string.editor_resize_hint : R.string.editor_gesture_hint);
        resizeHoldButton.setText(choosing ? R.string.cancel : R.string.resize_hold);
        fitPhotoButton.setEnabled(!saveModel.busy);
        resizeHoldButton.setEnabled(!saveModel.busy);
    }

    private void showHoldSizeDialog(Hold hold) {
        if (editor == null || saveModel.busy) return;
        View content = getLayoutInflater().inflate(R.layout.dialog_hold_size, null);
        Slider slider = content.findViewById(R.id.hold_size);
        int largestRadius = Math.max(2, Math.round(editor.getBoulderProblemInfo().getReferenceHeight() / 3f));
        slider.setValueFrom(0);
        slider.setValueTo(100);
        slider.setLabelBehavior(LabelFormatter.LABEL_GONE);
        // Give small markers more slider travel, while leaving an untouched size exact.
        int originalRadius = hold.getCircleRadius();
        float initialValue = Math.max(0, Math.min(100,
                (float) Math.sqrt(Math.max(0, (originalRadius - 1f) / (largestRadius - 1))) * 100));
        slider.setValue(initialValue);
        sizeDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hold_size)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    if (editor != null) {
                        float fraction = slider.getValue() / 100f;
                        int radius = slider.getValue() == initialValue ? originalRadius
                                : Math.round(1 + fraction * fraction * (largestRadius - 1));
                        editor.resizeHold(hold, radius);
                    }
                }).create();
        sizeDialog.setOnDismissListener(dialog -> {
            sizeDialog = null;
            if (editor != null) {
                editor.cancelHoldResize();
                updateEditorControls();
            }
        });
        sizeDialog.show();
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MainActivity activity = (MainActivity) requireActivity();
        menu.findItem(R.id.action_info).setVisible(true).setEnabled(!saveModel.busy);
        menu.findItem(R.id.action_share).setVisible(true).setEnabled(!saveModel.busy);
        menu.findItem(R.id.action_save).setVisible(activity.isHasUnsavedChanges()).setEnabled(!saveModel.busy);
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
            editor.flushPendingSingleTap();
            editor.setEnabled(false);
            BoulderProblemInfo info = BoulderProblemInfo.fromBundle(editor.getBoulderProblemInfo().toBundle());
            boolean needsSave = activity.isHasUnsavedChanges() || info.getFinalBitmapUri() == null || info.getId() == null;
            saveModel.start(requireContext().getApplicationContext(), info, activity.getmBoulderBitmap(),
                    needsSave, item.getItemId() == R.id.action_share);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Kept for existing callers; persistence belongs on an IO executor. */
    public long addOrUpdateBoulderProblem(BoulderProblemInfo info) {
        return BoulderRepository.save(requireContext().getApplicationContext(), info);
    }

    @Override public void onDestroyView() {
        if (sizeDialog != null) sizeDialog.dismiss();
        editor = null;
        gestureHint = null;
        fitPhotoButton = resizeHoldButton = null;
        super.onDestroyView();
    }

    public static class SaveModel extends ViewModel {
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        final MutableLiveData<SaveResult> result = new MutableLiveData<>();
        volatile boolean busy;

        void start(Context context, BoulderProblemInfo info, Bitmap bitmap, boolean needsSave, boolean share) {
            if (busy) return;
            busy = true;
            result.setValue(new SaveResult(info, true, false, false, null));
            executor.execute(() -> {
                boolean saved = false;
                try {
                    Uri shareUri = null;
                    boolean regenerate = needsSave;
                    if (share && !needsSave) {
                        try { shareUri = ImageFiles.shareableUri(context, info.getFinalBitmapUri()); }
                        catch (IOException | SecurityException unavailableExport) { regenerate = true; }
                    }
                    if (regenerate) {
                        Bitmap export = BoulderProblemView.render(bitmap, info);
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
