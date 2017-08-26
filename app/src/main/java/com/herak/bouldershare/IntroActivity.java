package com.herak.bouldershare;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.herak.bouldershare.fragments.TutorialFragment1;
import com.herak.bouldershare.fragments.TutorialFragment2;
import com.herak.bouldershare.fragments.TutorialFragment3;

/**
 * Created by darko on 25.8.2017..
 */

public class IntroActivity extends AppIntro {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoActionBar);
        getSupportActionBar().hide();
        // Note here that we DO NOT use setContentView();

        // Add your slide fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        addSlide(new TutorialFragment1());
        addSlide(new TutorialFragment2());
        addSlide(new TutorialFragment3());


        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
//        addSlide(AppIntroFragment.newInstance(title, description, image, backgroundColor));

        // OPTIONAL METHODS
        // Override bar/separator color.
//        setBarColor(Color.parseColor("#3F51B5"));

        setBarColor(Color.argb(0,0,0,0));
//        setBarColor(Color.rgb(255,64,129));


        setTitle("");

//        setSeparatorColor(Color.argb(33,0,0,0);

        // Hide Skip/Done button.
        showSkipButton(true);
        setProgressButtonEnabled(true);

        setBackButtonVisibilityWithDone(true);

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
//        setVibrate(true);
//        setVibrateIntensity(30);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }


}
