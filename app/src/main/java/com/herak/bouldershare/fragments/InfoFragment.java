package com.herak.bouldershare.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private ScrollView scroll;
    private EditText author, name, comment;
    private NumberPicker grade;
    private View decor;
    private int lastAvailableHeight;
    private int verticalBackgroundInsets;
    private final ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = this::fitFormToWindow;

    @Override public Dialog onCreateDialog(Bundle state) {
        MainActivity activity = (MainActivity) requireActivity();
        View view = activity.getLayoutInflater().inflate(R.layout.fragment_info, null);
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.info_title), true);
        SharedPreferences settings = activity.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        BoulderProblemInfo info = activity.getmBoulderProblemInfo();
        scroll = view.findViewById(R.id.info_scroll);
        author = view.findViewById(R.id.etInfoAuthor);
        name = view.findViewById(R.id.etInfoProblemName);
        comment = view.findViewById(R.id.etInfoComment);
        grade = view.findViewById(R.id.pickerGrade);
        List<String> grades = Arrays.asList(getResources().getStringArray(R.array.grades_array));
        grade.setMinValue(0); grade.setMaxValue(grades.size() - 1);
        grade.setDisplayedValues(getResources().getStringArray(R.array.grades_array));
        if (info != null) {
            author.setText(info.getAuthor() == null || info.getAuthor().isEmpty()
                    ? settings.getString("username", "") : info.getAuthor());
            name.setText(info.getName()); comment.setText(info.getComment());
            grade.setValue(Math.max(0, grades.indexOf(info.getGrade())));
        }
        if (state != null && state.containsKey("draft_name")) {
            name.setText(state.getString("draft_name"));
            author.setText(state.getString("draft_author"));
            comment.setText(state.getString("draft_comment"));
            grade.setValue(state.getInt("draft_grade"));
            EditText focused = view.findViewById(state.getInt("draft_focus", View.NO_ID));
            if (focused != null) {
                focused.requestFocus();
                int start = Math.max(0, Math.min(focused.length(), state.getInt("draft_selection_start")));
                int end = Math.max(start, Math.min(focused.length(), state.getInt("draft_selection_end")));
                focused.setSelection(start, end);
            }
        }
        name.setOnEditorActionListener((textView, action, event) -> nextField(action, author));
        author.setOnEditorActionListener((textView, action, event) -> nextField(action, comment));
        for (EditText field : new EditText[]{name, author, comment}) {
            field.setOnFocusChangeListener((focused, hasFocus) -> {
                if (hasFocus) scroll.post(this::showFocusedInput);
            });
        }
        view.findViewById(R.id.info_cancel).setOnClickListener(button -> dismiss());
        view.findViewById(R.id.info_done).setOnClickListener(button -> {
                    if (info == null) { dismiss(); return; }
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
                    dismiss();
                });
        // The title and actions scroll too: some landscape keyboards leave less
        // height than a standard alert dialog's fixed title and button rows.
        int verticalInset = Math.round(8 * getResources().getDisplayMetrics().density);
        verticalBackgroundInsets = 2 * verticalInset;
        return new MaterialAlertDialogBuilder(activity).setView(view)
                .setBackgroundInsetTop(verticalInset).setBackgroundInsetBottom(verticalInset).create();
    }

    private boolean nextField(int action, EditText next) {
        if (action != EditorInfo.IME_ACTION_NEXT) return false;
        next.requestFocus();
        return true;
    }

    @Override public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window == null) return;
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        decor = window.getDecorView();
        lastAvailableHeight = 0;
        decor.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
        decor.post(this::fitFormToWindow);
    }

    private void fitFormToWindow() {
        if (decor == null || scroll == null || scroll.getWidth() == 0) return;
        Rect visible = new Rect();
        decor.getWindowVisibleDisplayFrame(visible);
        int availableHeight = visible.height();
        // A floating dialog can keep its original visible frame under edge-to-edge.
        // Use the IME inset as a second bound, rather than adding keyboard padding.
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && insets != null) {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int screenHeight = requireActivity().getWindowManager().getCurrentWindowMetrics().getBounds().height();
            availableHeight = Math.min(availableHeight, screenHeight - bars.top - Math.max(bars.bottom, ime.bottom));
        }
        if (availableHeight <= 0) return;
        View content = scroll.getChildAt(0);
        content.measure(View.MeasureSpec.makeMeasureSpec(scroll.getWidth()
                        - scroll.getPaddingLeft() - scroll.getPaddingRight(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        // A window shrink can temporarily clip the old scroll height. Its
        // negative difference must not enlarge the viewport beyond the dialog.
        int chromeHeight = Math.max(verticalBackgroundInsets, decor.getHeight() - scroll.getHeight());
        int height = Math.min(content.getMeasuredHeight(), Math.max(1, availableHeight - chromeHeight));
        ViewGroup.LayoutParams params = scroll.getLayoutParams();
        boolean windowSpaceChanged = availableHeight != lastAvailableHeight;
        lastAvailableHeight = availableHeight;
        if (params.height != height) {
            params.height = height;
            scroll.setLayoutParams(params);
        }
        // Floating windows can still center in the full display with the IME
        // open. Center explicitly in the available frame so actions stay above it.
        Dialog dialog = getDialog();
        Window window = dialog == null ? null : dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            int offset = Math.max(0, (availableHeight - height - chromeHeight) / 2);
            int gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            if (attributes.gravity != gravity || attributes.y != offset) {
                attributes.gravity = gravity;
                attributes.y = offset;
                window.setAttributes(attributes);
            }
        }
        // Only a window/keyboard change should reclaim the focused line. Height
        // corrections must not undo the user's scroll toward Grade or the actions.
        if (windowSpaceChanged) scroll.post(this::showFocusedInput);
    }

    private void showFocusedInput() {
        if (scroll == null) return;
        View focused = scroll.findFocus();
        if (!(focused instanceof EditText)) return;
        EditText field = (EditText) focused;
        Layout textLayout = field.getLayout();
        if (textLayout == null) return;
        int selection = Math.max(0, field.getSelectionEnd());
        int line = textLayout.getLineForOffset(selection);
        int x = Math.round(textLayout.getPrimaryHorizontal(selection)) + field.getTotalPaddingLeft();
        Rect rectangle = new Rect(x, textLayout.getLineTop(line) + field.getTotalPaddingTop(),
                x + 1, textLayout.getLineBottom(line) + field.getTotalPaddingTop());
        int breathingRoom = Math.round(12 * getResources().getDisplayMetrics().density);
        rectangle.inset(0, -breathingRoom);
        // Convert the line into scroll-content coordinates, including any scroll
        // inside the multiline editor, without Material's whole-field rectangle.
        scroll.offsetDescendantRectToMyCoords(field, rectangle);
        int top = scroll.getScrollY();
        int bottom = top + scroll.getHeight();
        if (rectangle.top < top) scroll.scrollTo(scroll.getScrollX(), rectangle.top);
        else if (rectangle.bottom > bottom) scroll.scrollTo(scroll.getScrollX(), rectangle.bottom - scroll.getHeight());
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (name == null) return;
        outState.putString("draft_name", name.getText().toString());
        outState.putString("draft_author", author.getText().toString());
        outState.putString("draft_comment", comment.getText().toString());
        outState.putInt("draft_grade", grade.getValue());
        View focused = scroll.findFocus();
        if (focused instanceof EditText) {
            EditText field = (EditText) focused;
            outState.putInt("draft_focus", field.getId());
            outState.putInt("draft_selection_start", field.getSelectionStart());
            outState.putInt("draft_selection_end", field.getSelectionEnd());
        }
    }

    @Override public void onStop() {
        if (decor != null) {
            decor.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
            decor = null;
        }
        super.onStop();
    }

    @Override public void onDestroyView() {
        scroll = null; name = null; author = null; comment = null; grade = null;
        super.onDestroyView();
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
