package com.herak.bouldershare;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.herak.bouldershare.classes.MarkerAppearance;
import com.herak.bouldershare.classes.EditorSettings;
import com.herak.bouldershare.classes.MarkerColors;
import com.herak.bouldershare.classes.MarkerSettings;
import com.herak.bouldershare.enums.HoldType;
import com.herak.bouldershare.fragments.MarkerColorDialog;

/** Global marker preferences with live preview. Dimensions stay in photo pixels. */
public class SettingsActivity extends AppCompatActivity {
    private MarkerAppearance appearance;
    private LinearLayout content;
    private Preview preview;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        View root = findViewById(R.id.settings_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
        MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        toolbar.setNavigationContentDescription(R.string.back_to_library);
        toolbar.setNavigationOnClickListener(view -> finish());
        content = findViewById(R.id.settings_content);
        appearance = MarkerSettings.load(this);
        getSupportFragmentManager().setFragmentResultListener(MarkerColorDialog.RESULT, this, (key, result) -> {
            HoldType type = HoldType.valueOf(result.getString("type"));
            int color = result.getInt("color");
            save(new MarkerAppearance(appearance.strokeWidth, appearance.borderWidth, appearance.autoPadding,
                    type == HoldType.START_HOLD ? color : appearance.startColor,
                    type == HoldType.REGULAR_HOLD ? color : appearance.regularColor,
                    type == HoldType.TOP_HOLD ? color : appearance.topColor));
            updateColorButtons();
        });
        buildContent();
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(int string, int appearanceStyle) {
        TextView label = new TextView(this);
        label.setText(string); label.setTextAppearance(appearanceStyle);
        return label;
    }
    private void add(View view, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(topMargin); content.addView(view, params);
    }
    private void buildContent() {
        content.removeAllViews();
        add(text(R.string.marker_settings_intro, R.style.TextAppearance_Boulder_Headline), 0);
        add(text(R.string.marker_settings_units, R.style.TextAppearance_Boulder_Body), 12);
        preview = new Preview();
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, dp(148));
        previewParams.topMargin = dp(20); content.addView(preview, previewParams);
        addDimension(R.string.marker_stroke_label, 0, R.id.marker_stroke, 1, 20, appearance.strokeWidth);
        addDimension(R.string.marker_border_label, R.string.marker_border_hint, R.id.marker_border, 0, 10, appearance.borderWidth);
        addDimension(R.string.marker_padding_label, R.string.marker_padding_hint, R.id.marker_padding, 0, 20, appearance.autoPadding);
        add(text(R.string.marker_colors_title, R.style.TextAppearance_Boulder_Title), 20);
        addColorButton(HoldType.START_HOLD, R.id.marker_start_color);
        addColorButton(HoldType.REGULAR_HOLD, R.id.marker_regular_color);
        addColorButton(HoldType.TOP_HOLD, R.id.marker_top_color);
        MaterialButton reset = new MaterialButton(this);
        reset.setId(R.id.marker_defaults); reset.setText(R.string.marker_restore_defaults);
        reset.setOnClickListener(view -> { save(MarkerAppearance.defaults()); buildContent(); });
        add(reset, 20); updateColorButtons();
        add(text(R.string.editor_preferences_title, R.style.TextAppearance_Boulder_Title), 28);
        MaterialSwitch instructions = new MaterialSwitch(this);
        instructions.setId(R.id.show_editor_instructions);
        instructions.setText(R.string.show_editor_instructions);
        instructions.setTextAppearance(R.style.TextAppearance_Boulder_Body);
        instructions.setMinHeight(dp(48));
        instructions.setChecked(EditorSettings.showInstructions(this));
        instructions.setOnCheckedChangeListener((button, checked) -> EditorSettings.setShowInstructions(this, checked));
        add(instructions, 8);
        add(text(R.string.show_editor_instructions_summary, R.style.TextAppearance_Boulder_Body), 6);
    }

    private void addDimension(int title, int hint, int id, float min, float max, float value) {
        LinearLayout row = new LinearLayout(this); row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView label = text(title, R.style.TextAppearance_Boulder_Title);
        label.setLabelFor(id); row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        TextView number = new TextView(this); number.setTextAppearance(R.style.TextAppearance_Boulder_Body);
        number.setText(getString(R.string.marker_dimension_value, value)); row.addView(number);
        add(row, 22);
        if (hint != 0) add(text(hint, R.style.TextAppearance_Boulder_Body), 6);
        Slider slider = new Slider(this); slider.setId(id); slider.setValueFrom(min); slider.setValueTo(max);
        slider.setStepSize(.5f); slider.setValue(Math.round(value * 2) / 2f);
        slider.setContentDescription(getString(title));
        slider.setLabelFormatter(amount -> getString(R.string.marker_dimension_value, amount));
        slider.addOnChangeListener((control, amount, fromUser) -> {
            if (!fromUser) return;
            number.setText(getString(R.string.marker_dimension_value, amount));
            save(new MarkerAppearance(id == R.id.marker_stroke ? amount : appearance.strokeWidth,
                    id == R.id.marker_border ? amount : appearance.borderWidth,
                    id == R.id.marker_padding ? amount : appearance.autoPadding,
                    appearance.startColor, appearance.regularColor, appearance.topColor));
        });
        add(slider, 0);
    }

    private void save(MarkerAppearance value) {
        appearance = value; MarkerSettings.save(this, value);
        if (preview != null) preview.invalidate();
    }
    private void addColorButton(HoldType type, int id) {
        MaterialButton button = new MaterialButton(this);
        button.setId(id); button.setMinHeight(dp(56)); button.setCornerRadius(dp(16));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_high)));
        button.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        button.setIconTint(null); button.setIconSize(dp(24)); button.setIconPadding(dp(12));
        button.setOnClickListener(view -> {
            if (getSupportFragmentManager().findFragmentByTag("marker_color") == null) {
                MarkerColorDialog.create(type, appearance.colorFor(type)).show(getSupportFragmentManager(), "marker_color");
            }
        });
        add(button, 10);
    }
    private void updateColorButtons() {
        HoldType[] types = HoldType.values();
        int[] ids = {R.id.marker_start_color, R.id.marker_regular_color, R.id.marker_top_color};
        for (int i = 0; i < types.length; i++) {
            MaterialButton button = findViewById(ids[i]); int color = appearance.colorFor(types[i]);
            button.setText(getString(R.string.marker_color_action, getString(typeLabel(types[i])), MarkerColors.hex(color)));
            GradientDrawable swatch = new GradientDrawable(); swatch.setShape(GradientDrawable.OVAL);
            swatch.setColor(color); swatch.setStroke(dp(1), ContextCompat.getColor(this, R.color.outline));
            swatch.setSize(dp(24), dp(24)); button.setIcon(swatch);
        }
    }
    public static int typeLabel(HoldType type) {
        return type == HoldType.START_HOLD ? R.string.start_hold : type == HoldType.TOP_HOLD ? R.string.top_hold : R.string.regular_hold;
    }

    private final class Preview extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Preview() { super(SettingsActivity.this); setContentDescription(getString(R.string.marker_preview_description)); }
        @Override protected void onDraw(Canvas canvas) {
            float scale = Math.min(getWidth() / 540f, getHeight() / 180f);
            paint.setColor(ContextCompat.getColor(SettingsActivity.this, R.color.surface_container));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), dp(24), dp(24), paint);
            HoldType[] types = HoldType.values();
            for (int i = 0; i < types.length; i++) {
                float x = getWidth() * (i + .5f) / 3, y = getHeight() / 2f;
                paint.setStyle(Paint.Style.FILL); paint.setColor(0xffa4aaa0);
                canvas.drawOval(x - 18 * scale, y - 14 * scale, x + 18 * scale, y + 14 * scale, paint);
                float radius = appearance.centerlineRadius((25 + appearance.autoPadding) * scale, scale);
                paint.setStyle(Paint.Style.STROKE);
                if (appearance.borderWidth > 0) {
                    paint.setColor(Color.argb(160, 0, 0, 0));
                    paint.setStrokeWidth((appearance.strokeWidth + appearance.borderWidth * 2) * scale);
                    canvas.drawCircle(x, y, radius, paint);
                }
                paint.setColor(appearance.colorFor(types[i])); paint.setStrokeWidth(appearance.strokeWidth * scale);
                canvas.drawCircle(x, y, radius, paint);
            }
        }
    }
}
