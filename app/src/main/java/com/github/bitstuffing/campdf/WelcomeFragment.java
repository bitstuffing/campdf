package com.github.bitstuffing.campdf;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

/**
 * WelcomeFragment manage pages and positions about
 * OnBoarding Fragment (which should inform to user and put a button)
 */
public class WelcomeFragment extends Fragment {

    private int position;
    private static final int PAGES = 2;

    public WelcomeFragment(int position) {
        this.position = position;
    }

    public static int getPages() {
        return PAGES;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle s) {
        View view = null;
        switch(this.position){
            case 0:
                view = buildMainView(inflater,container);
                break;
            case 1:
                view = buildFinalView(inflater,container);
                break;
        }
        return view;
    }

    private View buildMainView(LayoutInflater inflater, ViewGroup container){
        View view = inflater.inflate(R.layout.onboard_screen, container,false);
        Button button = (Button) view.findViewById(R.id.setPermissionsButton);
        boolean enabled = Utils.checkPermissions(getActivity());
        setScrollClick(!enabled);
        //if( !enabled ){
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    Utils.requestPermissions(getActivity());
                    boolean hasPermissions = Utils.checkPermissions(getActivity());
                    setScrollClick(!hasPermissions);
                    Thread thread = new Thread(new Runnable(){
                        public void run() {
                            while(!Utils.checkPermissions(getActivity())){ //slide to next page
                                try {
                                    Thread.sleep(400);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            //send message with handler to activity (fragment)
                            WelcomeActivity.sendMessage(0);
                        }
                    });
                    thread.start();

                }
            });
        //}
        return view;
    }

    private void setScrollClick(boolean disable) {
        TabLayout indicator = (TabLayout) getActivity().findViewById(R.id.indicatorLayout);
        if(disable){
            indicator.clearOnTabSelectedListeners();
        }
        for (View v : indicator.getTouchables()) {
            v.setEnabled(disable);
        }
    }

    private View buildFinalView(LayoutInflater inflater, ViewGroup container){
        View view = inflater.inflate(R.layout.onboard_screen, container,false);
        Button button = (Button) view.findViewById(R.id.setPermissionsButton);
        button.setText("Let's start");
        TextView warning = (TextView) view.findViewById(R.id.warningTextView);
        warning.setText("All necessary operations are done");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WelcomeActivity.finishOnboarding(getActivity());
            }
        });
        return view;
    }
}
