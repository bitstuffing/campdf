package com.github.bitstuffing.campdf;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.github.bitstuffing.campdf.fragment.OnBoardingFragment;
import com.github.bitstuffing.campdf.fragment.WelcomeFragment;
import com.google.android.material.tabs.TabLayout;

public class WelcomeActivity extends FragmentActivity {

    private static WelcomeViewPager pager;
    private TabLayout indicator;

    protected static Handler messageHandler;

    static{
        messageHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                int code = bundle.getInt("code",0);
                String message = bundle.getString("message","");
                switch(code){
                    default:
                        int page = pager.getCurrentItem(); //should be 0
                        if(pager.getChildCount()>page){
                            pager.setCurrentItem(/*1*/ page + 1 ); //pager.setCurrentItem(pager.getCurrentItem()+1);
                        }
                        break;
                }

            }
        };
    }

    public static void sendMessage(int code,String message) {
        Message messageToBeSent = new Message();
        Bundle bundle = new Bundle();
        bundle.putInt("code",code);
        bundle.putString("message",message);
        messageToBeSent.setData(bundle);
        messageHandler.sendMessage(messageToBeSent);
    }

    public static void sendMessage(int code){
        sendMessage(code,"");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.onboarding);

        pager = (WelcomeViewPager)findViewById(R.id.pager);
        indicator = (TabLayout)findViewById(R.id.indicatorLayout);

        FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {

            @Override
            public int getCount() {
                return WelcomeFragment.getPages();
            }

            @NonNull
            @Override
            public Fragment getItem(int position) {
                return new WelcomeFragment(position);
            }
        };
        pager.setAdapter(adapter);
        indicator.setupWithViewPager(pager, true);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed(); //avoid default event to keep the control
    }

    public static void finishOnboarding(Activity activity) {
         SharedPreferences preferences = getDefaultSharedPreferences(activity);
         preferences.edit().putBoolean(OnBoardingFragment.COMPLETED_ONBOARDING_PREF_NAME,true).apply();
         Intent main = new Intent(activity, MainActivity.class);
         activity.startActivity(main);
         activity.finish();
    }
}
