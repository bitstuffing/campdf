package com.github.bitstuffing.campdf;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

/**
 * WelcomeViewPager intercepts default events to avoid Swipe
 * until developer wants it.
 */
public class WelcomeViewPager extends ViewPager {

    public WelcomeViewPager(@NonNull Context context) {
        super(context);
    }

    public WelcomeViewPager(Context context, AttributeSet attrs){
        super(context,attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if(Utils.checkPermissions(getContext())){
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(Utils.checkPermissions(getContext())){
            return super.onTouchEvent(event);
        }
        return false;
    }

}
