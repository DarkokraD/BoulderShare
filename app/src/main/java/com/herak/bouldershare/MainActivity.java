package com.herak.bouldershare;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.classes.BoulderProblemView;
import com.herak.bouldershare.classes.ImageFiles;
import com.herak.bouldershare.fragments.BoulderFragment;
import com.herak.bouldershare.fragments.InfoFragment;
import com.herak.bouldershare.fragments.MainFragment;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements InfoFragment.OnInfoStoredListener {
    public static final String PREFS_NAME = "BoulderSharePrefs";
    public static final String MAIN_FRAGMENT_TAG = "MAIN_FRAGMENT_TAG";
    public static final String BOULDER_FRAGMENT_TAG = "BOULDER_FRAGMENT_TAG";
    public enum FRAGMENT_TYPE { MAIN_FRAGMENT, BOULDER_FRAGMENT, SETTINGS_FRAGMENT }

    private EditorState state;
    private Menu menu;
    private MaterialToolbar toolbar;

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) importPhoto(uri);
                else state.relinkProblem = null;
            });
    private final ActivityResultLauncher<Uri> camera =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                Uri uri = state.cameraUri;
                if (Boolean.TRUE.equals(success) && uri != null) importPhoto(uri);
                state.cameraUri = null;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = new ViewModelProvider(this).get(EditorState.class);
        if (savedInstanceState != null && state.problem == null) {
            Bundle problem = savedInstanceState.getBundle("problem");
            if (problem != null) state.problem = BoulderProblemInfo.fromBundle(problem);
            state.dirty = savedInstanceState.getBoolean("dirty");
            String cameraUri = savedInstanceState.getString("camera_uri");
            if (cameraUri != null) state.cameraUri = Uri.parse(cameraUri);
            state.navigateToEditor = savedInstanceState.getBoolean("navigate_to_editor");
            state.relinkProblem = savedInstanceState.getBundle("relink_problem");
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.main_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.fabCamera).setOnClickListener(view -> takePhoto());
        findViewById(R.id.fabGallery).setOnClickListener(view -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                flushPendingEditorTap();
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.flayoutMainActivity);
                if (current instanceof BoulderFragment) {
                    if (((BoulderFragment) current).isBusy()) {
                        Toast.makeText(MainActivity.this, R.string.saving_progress, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (state.dirty) {
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle(R.string.unsaved_title)
                                .setMessage(R.string.unsaved_message)
                                .setNegativeButton(R.string.keep_editing, null)
                                .setPositiveButton(R.string.discard, (dialog, which) ->
                                        changeFragment(FRAGMENT_TYPE.MAIN_FRAGMENT)).show();
                    } else changeFragment(FRAGMENT_TYPE.MAIN_FRAGMENT);
                } else finish();
            }
        });
        state.loading.observe(this, loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            findViewById(R.id.photo_progress).setVisibility(busy ? View.VISIBLE : View.GONE);
            findViewById(R.id.fabCamera).setEnabled(!busy);
            findViewById(R.id.fabGallery).setEnabled(!busy);
        });
        state.imported.observe(this, result -> {
            if (result == null || result.consumed) return;
            result.consumed = true;
            state.loading.setValue(false);
            if (result.problem == null) {
                Toast.makeText(this, R.string.photo_load_error, Toast.LENGTH_LONG).show();
                return;
            }
            setmBoulderProblemInfo(result.problem);
            state.bitmap = result.bitmap;
            state.dirty = true;
            state.navigateToEditor = true;
            if (!getSupportFragmentManager().isStateSaved()) openPendingEditor();
        });
        if (savedInstanceState == null) {
            changeFragment(FRAGMENT_TYPE.MAIN_FRAGMENT);
            runTutorialIfFirstRun();
        } else updateChrome();
    }

    @Override protected void onResumeFragments() {
        super.onResumeFragments();
        openPendingEditor();
        updateChrome();
    }

    private void openPendingEditor() {
        if (state.navigateToEditor) {
            state.navigateToEditor = false;
            changeFragment(FRAGMENT_TYPE.BOULDER_FRAGMENT);
        }
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        flushPendingEditorTap();
        if (state.problem != null) outState.putBundle("problem", state.problem.toBundle());
        outState.putBoolean("dirty", state.dirty);
        outState.putBoolean("navigate_to_editor", state.navigateToEditor);
        if (state.relinkProblem != null) outState.putBundle("relink_problem", state.relinkProblem);
        if (state.cameraUri != null) outState.putString("camera_uri", state.cameraUri.toString());
        super.onSaveInstanceState(outState);
    }

    private void flushPendingEditorTap() {
        View editor = findViewById(R.id.boulderProblemView);
        if (editor instanceof BoulderProblemView) ((BoulderProblemView) editor).flushPendingSingleTap();
    }

    private void takePhoto() {
        try {
            File file = ImageFiles.cameraFile(this);
            state.cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            camera.launch(state.cameraUri);
        } catch (ActivityNotFoundException exception) {
            state.cameraUri = null;
            Toast.makeText(this, R.string.no_camera_app, Toast.LENGTH_LONG).show();
        } catch (IOException | SecurityException exception) {
            state.cameraUri = null;
            Toast.makeText(this, R.string.photo_load_error, Toast.LENGTH_LONG).show();
        }
    }

    private void importPhoto(Uri uri) {
        if (Boolean.TRUE.equals(state.loading.getValue())) return;
        state.loading.setValue(true);
        final android.content.Context context = getApplicationContext();
        final EditorState model = state;
        final Bundle relink = model.relinkProblem;
        model.relinkProblem = null;
        model.executor.execute(() -> {
            Uri importedUri = null;
            try {
                importedUri = ImageFiles.importPhoto(context, uri);
                Bitmap bitmap = ImageFiles.load(context, importedUri);
                BoulderProblemInfo info = relink == null ? new BoulderProblemInfo() : BoulderProblemInfo.fromBundle(relink);
                info.setInputBitmapUri(importedUri);
                model.imported.postValue(new ImportResult(info, bitmap));
            } catch (IOException | RuntimeException exception) {
                if (importedUri != null && importedUri.getPath() != null) {
                    new File(importedUri.getPath()).delete();
                }
                model.imported.postValue(new ImportResult(null, null));
            }
        });
    }

    private void runTutorialIfFirstRun() {
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        if (prefs.getBoolean("firstStart", true)) {
            prefs.edit().putBoolean("firstStart", false).apply();
            startActivity(new Intent(this, IntroActivity.class));
        }
    }

    /** Recover a revoked legacy gallery URI without discarding its route or holds. */
    public void requestPhotoRelink() {
        if (state.problem == null) return;
        state.relinkProblem = state.problem.toBundle();
        photoPicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }

    public void changeFragment(FRAGMENT_TYPE type) {
        boolean editor = type == FRAGMENT_TYPE.BOULDER_FRAGMENT;
        if (editor && state.problem == null) return;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flayoutMainActivity, editor ? new BoulderFragment() : new MainFragment(),
                        editor ? BOULDER_FRAGMENT_TAG : MAIN_FRAGMENT_TAG)
                .commit();
        if (!editor) {
            state.dirty = false;
            state.bitmap = null;
        }
        updateChrome(editor);
        invalidateOptionsMenu();
    }

    private void updateChrome() {
        updateChrome(getSupportFragmentManager().findFragmentById(R.id.flayoutMainActivity) instanceof BoulderFragment);
    }

    private void updateChrome(boolean editor) {
        if (toolbar == null) return;
        findViewById(R.id.fabCamera).setVisibility(editor ? View.GONE : View.VISIBLE);
        findViewById(R.id.fabGallery).setVisibility(editor ? View.GONE : View.VISIBLE);
        toolbar.setTitle(editor ? R.string.editor_title : R.string.app_name);
        toolbar.setNavigationIcon(editor ? AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back) : null);
        toolbar.setNavigationContentDescription(R.string.back_to_library);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_tutorial) {
            startActivity(new Intent(this, IntroActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_donate) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/DarkoHerak")));
            } catch (ActivityNotFoundException ignored) { }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onInfoStoredInteraction(BoulderProblemInfo info) {
        state.problem = info;
        setHasUnsavedChanges(true);
        View editor = findViewById(R.id.boulderProblemView);
        if (editor != null) editor.invalidate();
    }

    public BoulderProblemInfo getmBoulderProblemInfo() { return state.problem; }
    public void setmBoulderProblemInfo(BoulderProblemInfo info) {
        state.problem = info;
        state.bitmap = null;
        state.dirty = info != null && info.getId() == null;
    }
    public Bitmap getmBoulderBitmap() { return state.bitmap; }
    public void setmBoulderBitmap(Bitmap bitmap) { state.bitmap = bitmap; }
    public Uri getmBoulderBitmapUri() { return state.problem == null ? null : state.problem.getInputBitmapUri(); }
    public void setmBoulderBitmapUri(Uri uri) {
        if (state.problem != null) state.problem.setInputBitmapUri(uri);
    }
    public boolean isHasUnsavedChanges() { return state.dirty; }
    public void setHasUnsavedChanges(boolean dirty) { state.dirty = dirty; setSaveIconVisibility(dirty); }
    public Menu getMenu() { return menu; }
    public void setSaveIconVisibility(boolean visible) {
        if (menu != null && menu.findItem(R.id.action_save) != null) menu.findItem(R.id.action_save).setVisible(visible);
    }
    public void setShareIconVisibility(boolean visible) {
        if (menu != null && menu.findItem(R.id.action_share) != null) menu.findItem(R.id.action_share).setVisible(visible);
    }
    public void setNextFragment(FRAGMENT_TYPE type) { state.navigateToEditor = type == FRAGMENT_TYPE.BOULDER_FRAGMENT; }

    public static final class EditorState extends ViewModel {
        BoulderProblemInfo problem;
        Bitmap bitmap;
        Uri cameraUri;
        boolean dirty;
        boolean navigateToEditor;
        Bundle relinkProblem;
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
        final MutableLiveData<ImportResult> imported = new MutableLiveData<>();
        @Override protected void onCleared() { executor.shutdownNow(); }
    }

    private static final class ImportResult {
        final BoulderProblemInfo problem;
        final Bitmap bitmap;
        boolean consumed;
        ImportResult(BoulderProblemInfo problem, Bitmap bitmap) {
            this.problem = problem;
            this.bitmap = bitmap;
        }
    }
}
