package com.github.bitstuffing.campdf;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Simple OnBoardingFragment which has a layout with a text and a button.
 * If you wish a personalization or include an image in the welcome screen
 * you should take in consideration this fragment.
 */
public class OnBoardingFragment extends Fragment {

    public static final String COMPLETED_ONBOARDING_PREF_NAME = "completed_onboard_start_up";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle s) {
        return inflater.inflate(R.layout.onboarding,container,false);
    }
}
