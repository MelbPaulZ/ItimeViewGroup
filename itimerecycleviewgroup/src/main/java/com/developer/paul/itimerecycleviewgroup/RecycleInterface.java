package com.developer.paul.itimerecycleviewgroup;

import android.view.ViewGroup;

/**
 * Created by Paul on 30/5/17.
 */

public interface RecycleInterface{

    AwesomeViewGroup getChildView(int index);
    AwesomeViewGroup getFirstShowView();

    // init show offset
    void initialShowOffset(int offsetY);
    // page change
    void onPageChange(AwesomeViewGroup awesomeViewGroup);

    // scroll position
    void scrollToX(int x);
    void scrollToY(int y);
    void scrollByX(int x);
    void scrollByY(int y);
    void scrollByXSmoothly(int x);
    void scrollByXSmoothly(int x, long duration);
    void scrollToYSmoothly(int y);

    // state change
    void onScrollStart();
    void onScrolling();
    void onScrollEnd();
    void onFlingStart();
    void onFlingEnd();


}
