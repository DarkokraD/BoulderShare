package com.herak.bouldershare;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/** A small, accessible walkthrough without a separate onboarding dependency. */
public class IntroActivity extends AppCompatActivity {
    private static final String STATE_STEP = "tutorial_step";
    private static final int[] TITLES = {
            R.string.tutorial_title_photo, R.string.tutorial_title_holds,
            R.string.tutorial_title_share
    };
    private static final int[] BODIES = {
            R.string.tutorial_body_photo, R.string.tutorial_body_holds,
            R.string.tutorial_body_share
    };
    private static final int[] IMAGES = {
            R.drawable.illustration_photo, R.drawable.illustration_holds,
            R.drawable.illustration_share
    };

    private int step;
    private TextView title;
    private TextView body;
    private TextView stepLabel;
    private ImageView image;
    private MaterialButton previous;
    private MaterialButton next;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_intro);
        View root = findViewById(R.id.intro_root);
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(left + insets.left, top + insets.top,
                    right + insets.right, bottom + insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);

        title = findViewById(R.id.intro_title);
        body = findViewById(R.id.intro_body);
        stepLabel = findViewById(R.id.intro_step);
        image = findViewById(R.id.intro_image);
        previous = findViewById(R.id.intro_previous);
        next = findViewById(R.id.intro_next);
        ViewCompat.setAccessibilityHeading(title, true);
        title.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        findViewById(R.id.intro_skip).setOnClickListener(view -> finish());
        previous.setOnClickListener(view -> {
            if (step > 0) {
                step--;
                showStep();
            }
        });
        next.setOnClickListener(view -> {
            if (step == TITLES.length - 1) {
                finish();
            } else {
                step++;
                showStep();
            }
        });
        if (savedInstanceState != null) {
            step = Math.max(0, Math.min(TITLES.length - 1,
                    savedInstanceState.getInt(STATE_STEP, 0)));
        }
        showStep();
    }

    private void showStep() {
        ((ScrollView) findViewById(R.id.intro_scroll)).scrollTo(0, 0);
        title.setText(TITLES[step]);
        body.setText(BODIES[step]);
        stepLabel.setText(getString(R.string.tutorial_step, step + 1, TITLES.length));
        image.setImageResource(IMAGES[step]);
        ((LinearProgressIndicator) findViewById(R.id.intro_progress)).setProgress(step + 1);
        previous.setVisibility(step == 0 ? View.GONE : View.VISIBLE);
        next.setText(step == TITLES.length - 1 ? R.string.tutorial_done : R.string.tutorial_next);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_STEP, step);
        super.onSaveInstanceState(outState);
    }
}
