package com.developer.paul.itimeviewgroup;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.developer.paul.itimerecycleviewgroup.AwesomeViewGroup;
import com.developer.paul.itimerecycleviewgroup.ITimeRecycleViewGroup;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ITimeRecycleViewGroup re = (ITimeRecycleViewGroup) findViewById(R.id.recycleViewGroup);
        re.setOnScroll(new ITimeRecycleViewGroup.OnScroll() {
            @Override
            public void onPageSelected(View v) {
//                Log.i("onCreate", "onPageSelected: " + ((AwesomeViewGroup)v).getInRecycledViewIndex());
            }

            @Override
            public void onHorizontalScroll(int dx, int preOffsetX) {
//                Log.i("onCreate", "onHorizontalScroll: " + dx + " , " + preOffsetX);
            }

            @Override
            public void onVerticalScroll(int dy, int preOffsetY) {
//                Log.i("onCreate", "onVerticalScroll: " + dy + " , " + preOffsetY);
            }
        });
    }
}
