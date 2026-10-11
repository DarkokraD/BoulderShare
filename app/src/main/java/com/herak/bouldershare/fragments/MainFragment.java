package com.herak.bouldershare.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.herak.bouldershare.MainActivity;
import com.herak.bouldershare.R;
import com.herak.bouldershare.adapters.BoulderGridViewAdapter;
import com.herak.bouldershare.classes.BoulderProblemInfo;
import com.herak.bouldershare.data.BoulderContract;
import com.herak.bouldershare.data.BoulderRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainFragment extends Fragment {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private GridView grid;
    private BoulderGridViewAdapter adapter;
    private int generation;
    private boolean opening;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        grid = root.findViewById(R.id.gridBoulderProblems);
        grid.setEmptyView(root.findViewById(R.id.empty_state));
        adapter = new BoulderGridViewAdapter(requireContext(), null);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> open(id));
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_problem_title).setMessage(R.string.delete_problem_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete, (dialog, which) -> delete(id)).show();
            return true;
        });
        return root;
    }

    private void open(long id) {
        if (opening) return;
        opening = true;
        int token = generation;
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            try {
                BoulderProblemInfo info = BoulderRepository.load(context, id);
                main.post(() -> {
                    if (!valid(token)) return;
                    opening = false;
                    if (info == null) { refresh(); return; }
                    MainActivity activity = (MainActivity) requireActivity();
                    activity.setmBoulderProblemInfo(info);
                    activity.setHasUnsavedChanges(false);
                    activity.changeFragment(MainActivity.FRAGMENT_TYPE.BOULDER_FRAGMENT);
                });
            } catch (RuntimeException error) {
                main.post(() -> {
                    if (!valid(token)) return;
                    opening = false;
                    Toast.makeText(requireContext(), R.string.image_load_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void delete(long id) {
        Context context = requireContext().getApplicationContext();
        int token = generation;
        executor.execute(() -> {
            try {
                context.getContentResolver().delete(BoulderContract.BoulderProblemInfoEntry.buildBoulderProblemInfoUri(id), null, null);
                main.post(() -> {
                    if (!valid(token)) return;
                    Toast.makeText(requireContext(), R.string.problem_deleted, Toast.LENGTH_SHORT).show();
                    refresh();
                });
            } catch (RuntimeException error) {
                main.post(() -> {
                    if (valid(token)) Toast.makeText(requireContext(), R.string.problem_delete_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void refresh() {
        int token = ++generation;
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            try {
                Cursor cursor = context.getContentResolver().query(BoulderContract.BoulderProblemInfoEntry.CONTENT_URI,
                        null, null, null, "_id DESC");
                main.post(() -> {
                    if (!valid(token)) { if (cursor != null) cursor.close(); return; }
                    adapter.changeCursor(cursor);
                });
            } catch (RuntimeException error) {
                main.post(() -> {
                    if (valid(token)) Toast.makeText(requireContext(), R.string.library_load_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean valid(int token) { return token == generation && isAdded() && grid != null; }
    @Override public void onStart() { super.onStart(); refresh(); }
    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.findItem(R.id.action_info).setVisible(false);
        menu.findItem(R.id.action_save).setVisible(false);
        menu.findItem(R.id.action_share).setVisible(false);
    }
    @Override public void onDestroyView() {
        ++generation;
        opening = false;
        if (adapter != null) adapter.changeCursor(null);
        adapter = null; grid = null;
        super.onDestroyView();
    }
    @Override public void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
