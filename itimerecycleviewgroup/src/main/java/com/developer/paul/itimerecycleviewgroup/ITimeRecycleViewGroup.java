package com.developer.paul.itimerecycleviewgroup;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul on 30/5/17.
 */

public class ITimeRecycleViewGroup extends ViewGroup implements RecycleInterface{
    private List<AwesomeViewGroup> awesomeViewGroupList = new ArrayList<>();
    private final static int NUM_SHOW = 7;
    int[] colors =new int[]{Color.RED, Color.BLUE, Color.GRAY, Color.YELLOW, Color.GREEN, Color.WHITE, Color.MAGENTA, Color.DKGRAY, Color.CYAN};

    private int viewHeight, viewWidth, childWidth, childHeight;

    private final int SCROLL_LEFT = -1, SCROLL_RIGHT = 1, SCROLL_UP = -2, SCROLL_DOWN = 2;
    private int scrollDir = 0; //{ SCROLL_LEFT, SCROLL_RIGHT, SCROLL_UP, SCROLL_DOWN }

    private final int SCROLL_HORIZONTAL = 10001, SCROLL_VERTICAL = 10002;
    private int scrollModel = 0;

    private int mTouchSlop;
    private boolean scrollOverTouchSlop = false;

    //for fling
    private VelocityTracker mVelocityTracker;
    private int mMaxVelocity;
    private float mVelocityX, mVelocityY;
    private Scroller mScroller;
    private boolean canHorizontalFling;
    private int status;

    public final static int START = 1000;
    public final static int HORIZONTAL_MOVE = 1001;
    public final static int STOP = 1002;
    public final static int HORIZONTAL_FLING = 1003;
    public final static int CHANGE_PAGE = 1004;
    public final static int VERTICAL_FLING = 1005;
    public final static int MOVE_WITH_PERCENT = 1006;
    public final static int VERTICAL_MOVE = 1007;

    // for coordination
    private float originX, originY;
    private float newX, newY, preX, preY;


    public ITimeRecycleViewGroup(Context context) {
        super(context);
        init();
    }

    public ITimeRecycleViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ITimeRecycleViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        for (int i = 0 ; i < NUM_SHOW+2 ; i ++){
            AwesomeViewGroup awesomeViewGroup = new AwesomeViewGroup(getContext());
            awesomeViewGroup.setBackgroundColor(colors[i]);
            awesomeViewGroup.setInRecycledViewIndex(i);
            awesomeViewGroup.setLayoutParams(new AwesomeViewGroup.AwesomeLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            awesomeViewGroupList.add(awesomeViewGroup);
            addView(awesomeViewGroup);
        }

        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mTouchSlop = vc.getScaledTouchSlop();
        mMaxVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
        mScroller = new Scroller(getContext());
    }


    private void moveXPostCheck(List<AwesomeViewGroup> awesomeViewGroups, int scrollDir){
        int viewGroupSize = awesomeViewGroups.size();
        boolean pageChanged = false;
        List<AwesomeViewGroup> tempList = new ArrayList<>();
        if (scrollDir == SCROLL_LEFT){
            for (int i = 0 ; i < viewGroupSize ; i++){
                if (awesomeViewGroups.get(i).isRightOutOfParentLeft()){
                    pageChanged = true;
                    tempList.add(awesomeViewGroups.get(i));
                }
            }

            for (int i = 0 ; i < tempList.size() ; i++){
                // redraw must before move, because of the index
                reDrawViewGroupToLast(tempList.get(i), awesomeViewGroups.get(viewGroupSize - 1));
                moveViewGroupToLast(tempList.get(i), awesomeViewGroups);
            }
        }else if (scrollDir == SCROLL_RIGHT){
            for (int i = 0 ; i < viewGroupSize ; i ++){
                if (awesomeViewGroups.get(i).isLeftOutOfParentRight()){
                    pageChanged = true;
                    tempList.add(awesomeViewGroups.get(i));
                }
            }

            for (int i = tempList.size() - 1 ; i >= 0 ; i --){
                // redraw must before move, because of the index
                reDrawViewGroupToFirst(tempList.get(i), awesomeViewGroups.get(0));
                moveViewGroupToFirst(tempList.get(i), awesomeViewGroups);
            }
        }

        if (pageChanged){
//            LogUtil.logAwesomes(awesomeViewGroups);
        }
    }

    private void touchUpPostCheck(){
        int maxDis = viewWidth;
        int alreadyMoveDis = (int) (newX - originX);
        int[] scrollPos = ScrollHelper.calculateScrollDistance(mScroller, (int)mVelocityX,
                (int)mVelocityY, maxDis,alreadyMoveDis, childWidth);
        switch (scrollDir){
            case SCROLL_LEFT:
            case SCROLL_RIGHT:
                setStatus(HORIZONTAL_FLING); // should fling or scroll to closet are both horizontal fling
                if (ScrollHelper.shouldFling(mVelocityX)){
                    int distance = scrollPos[0];
                    //float compensite, for inaccurate calculate
                    distance += compensateDistance(distance, awesomeViewGroupList);
                    float scrollTime = ScrollHelper.calculateScrollTime(mVelocityX);
                    LogUtil.logFirstAwesome(awesomeViewGroupList);
                    scrollByXSmoothly(distance, (long) (scrollTime*1000));
                }else{
                    scrollToClosestPosition(awesomeViewGroupList);
                }
        }
    }

    private void scrollToClosestPosition(List<AwesomeViewGroup> awesomeViewGroups){
        int dis = childNeedsScrollToNearestPosition(awesomeViewGroups);
        scrollByXSmoothly(dis);
    }

    private int childNeedsScrollToNearestPosition(List<AwesomeViewGroup> awesomeViewGroups){
        for (AwesomeViewGroup awesomeViewGroup : awesomeViewGroups){
            if (awesomeViewGroup.isVisibleInParent() && awesomeViewGroup.isOutOfParent()){
                AwesomeViewGroup.AwesomeLayoutParams lp = (AwesomeViewGroup.AwesomeLayoutParams) awesomeViewGroup.getLayoutParams();
                return Math.abs(lp.right) > Math.abs(lp.left) ? -lp.left : -lp.right;
            }
        }
        return 0;
    }

    /**
     * distance might be inaccurate, so before fling, distance need add compensate.
     * @param distance
     * @param awesomeViewGroups
     * @return
     */
    private int compensateDistance(int distance, List<AwesomeViewGroup> awesomeViewGroups){
        int min = 10000;
        int minValue = 0;
        for (AwesomeViewGroup awesomeViewGroup :awesomeViewGroups){
            AwesomeViewGroup.AwesomeLayoutParams lp = (AwesomeViewGroup.AwesomeLayoutParams) awesomeViewGroup.getLayoutParams();
            if (Math.abs(lp.left + distance) < min){
                min = Math.abs(lp.left + distance);
                minValue = lp.left + distance;
            }
        }
        return -minValue;
    }

//    private void tinyAdjustAwesomeViewGroups(List<AwesomeViewGroup> awesomeViewGroups){
//        int min = 10000;
//        int minValue = 0;
//        for (AwesomeViewGroup awesomeViewGroup: awesomeViewGroups){
//            AwesomeViewGroup.AwesomeLayoutParams lp = (AwesomeViewGroup.AwesomeLayoutParams) awesomeViewGroup.getLayoutParams();
//            if (Math.abs(lp.left)< min){
//                min = Math.abs(lp.left);
//                minValue = lp.left;
//            }
//        }
//
//        // find the minValue
//        if (Math.abs(minValue) > 10){
//            LogUtil.logError("tinyAdjustError: " + minValue);
//        }
//        Log.i("tiny", "tinyAdjustAwesomeViewGroups: " + minValue);
//        scrollByX(-minValue);
//    }


    private void moveViewGroupToLast(AwesomeViewGroup awesomeViewGroup, List<AwesomeViewGroup> awesomeViewGroups){
        awesomeViewGroups.remove(awesomeViewGroup);
        awesomeViewGroups.add(awesomeViewGroup);
        awesomeViewGroup.setInRecycledViewIndex(awesomeViewGroup.getInRecycledViewIndex() + NUM_SHOW + 2);
    }

    private void moveViewGroupToFirst(AwesomeViewGroup awesomeViewGroup, List<AwesomeViewGroup> awesomeViewGroups){
        awesomeViewGroups.remove(awesomeViewGroup);
        awesomeViewGroups.add(0, awesomeViewGroup);
        awesomeViewGroup.setInRecycledViewIndex(awesomeViewGroup.getInRecycledViewIndex() - (NUM_SHOW + 2));
    }

    private void reDrawViewGroupToLast(AwesomeViewGroup toBeDrawViewGroup, AwesomeViewGroup lastAwesomeViewGroup){
        AwesomeViewGroup.AwesomeLayoutParams lastLp = (AwesomeViewGroup.AwesomeLayoutParams) lastAwesomeViewGroup.getLayoutParams();
        int lastLpRight = lastLp.right;
        AwesomeViewGroup.AwesomeLayoutParams toBeDrawLp = (AwesomeViewGroup.AwesomeLayoutParams) toBeDrawViewGroup.getLayoutParams();
        toBeDrawLp.left = lastLpRight;
        toBeDrawLp.right = toBeDrawLp.left + toBeDrawLp.width;
    }

    private void reDrawViewGroupToFirst(AwesomeViewGroup toBeDrawViewGroup, AwesomeViewGroup firstAwesomeViewGroup){
        AwesomeViewGroup.AwesomeLayoutParams firstLp = (AwesomeViewGroup.AwesomeLayoutParams) firstAwesomeViewGroup.getLayoutParams();
        int firstLpLeft = firstLp.left;
        AwesomeViewGroup.AwesomeLayoutParams toBeDrawLp = (AwesomeViewGroup.AwesomeLayoutParams) toBeDrawViewGroup.getLayoutParams();
        toBeDrawLp.right = firstLpLeft;
        toBeDrawLp.left = toBeDrawLp.right - toBeDrawLp.width;
    }






    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(viewWidth, viewHeight);

        childWidth = viewWidth/NUM_SHOW;
        childHeight = 2 * viewHeight;

        int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);

        for (int i = 0 ; i < awesomeViewGroupList.size() ; i ++){
            AwesomeViewGroup awesomeViewGroup = awesomeViewGroupList.get(i);
            measureChild(awesomeViewGroup, childWidthSpec, childHeightSpec);
            AwesomeViewGroup.AwesomeLayoutParams lp = (AwesomeViewGroup.AwesomeLayoutParams) awesomeViewGroup.getLayoutParams();

            lp.parentHeight = viewHeight;
            lp.width = childWidth;
            lp.height = childHeight;

            lp.left = (i-1) * childWidth;
            lp.right = lp.left + childWidth;
            lp.bottom = lp.top + lp.height;
        }
    }

    private boolean isTouchOutOfParent(MotionEvent ev){
        if (ev.getX() > getWidth() || ev.getX() < 0){
            LogUtil.logError("out");
            return true;
        }
        return false;
    }

    private void setStatus(int status){
        this.status = status;
        LogUtil.log("setStatus", status + "");
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        if (isTouchOutOfParent(ev)){
            return true;
        }
        switch (action){
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                newX = ev.getX();
                newY = ev.getY();
                LogUtil.log("onIntercept","MOVE");
                if (!scrollOverTouchSlop){
                    if (mTouchSlop <= Math.abs(newX - preX)){
                        setStatus(HORIZONTAL_MOVE);
                        scrollOverTouchSlop = true;
                        scrollModel = SCROLL_HORIZONTAL;
                    }else if (mTouchSlop <= Math.abs(newY - preY)){
                        setStatus(VERTICAL_MOVE);
                        scrollOverTouchSlop = true;
                        scrollModel = SCROLL_VERTICAL;
                    }
                }

                if (scrollOverTouchSlop){
                    if (scrollModel == SCROLL_HORIZONTAL){
                        setStatus(HORIZONTAL_MOVE);
                        float moveX = newX - preX;

                        if (moveX > 0){
                            scrollDir = SCROLL_RIGHT;
                        }else if (moveX < 0){
                            scrollDir = SCROLL_LEFT;
                        }
                        scrollByX((int) moveX);
                        preX = newX;
                    }else if (scrollModel == SCROLL_VERTICAL){
                        setStatus(VERTICAL_MOVE);
                    }
                    mVelocityTracker.addMovement(ev);
                    mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
                    mVelocityX = mVelocityTracker.getXVelocity();
                    mVelocityY = mVelocityTracker.getYVelocity();

                    return true;
                }

                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:

                setStatus(START);
                if (mVelocityTracker == null){
                    mVelocityTracker = VelocityTracker.obtain();
                }else{
                    mVelocityTracker.clear();
                }
                mVelocityX = 0;
                mVelocityY = 0;

                originX = event.getX();
                originY = event.getY();
                preX = event.getX();
                preY = event.getY();
                super.onTouchEvent(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isTouchOutOfParent(event)){
                    return true;
                }
                newX = event.getX();
                newY = event.getY();
                LogUtil.log("onTouchEvent", "Move");
                if (!scrollOverTouchSlop) {
                    if (mTouchSlop <= Math.abs(newX - preX)) {
                        setStatus(HORIZONTAL_MOVE);
                        scrollOverTouchSlop = true;
                        scrollModel = SCROLL_HORIZONTAL;
                    } else if (mTouchSlop <= Math.abs(newY - preY)) {
                        setStatus(VERTICAL_MOVE);
                        scrollOverTouchSlop = true;
                        scrollModel = SCROLL_VERTICAL;
                    }
                }

                if (scrollOverTouchSlop) {
                    if (scrollModel == SCROLL_HORIZONTAL) {
                        setStatus(HORIZONTAL_MOVE);
                        float moveX = newX - preX;
                        if (moveX > 0) {
                            scrollDir = SCROLL_RIGHT;
                        } else if (moveX < 0) {
                            scrollDir = SCROLL_LEFT;
                        }

                        LogUtil.log("onTouchEvent",moveX + "");
                        scrollByX((int) moveX);
                        preX = newX;
                    } else if (scrollModel == SCROLL_VERTICAL) {
                        setStatus(VERTICAL_MOVE);
                    }
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
                    mVelocityX = mVelocityTracker.getXVelocity();
                    mVelocityY = mVelocityTracker.getYVelocity();
                }
                return super.onTouchEvent(event);

            case MotionEvent.ACTION_UP:
                newX = getEventXFilterOutside(event);
                newY = event.getY();
                if (scrollOverTouchSlop){
                    touchUpPostCheck();
                }
                preX = newX;
                preY = newY;
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_OUTSIDE:
                break;

        }

        return super.onTouchEvent(event);
    }

    private float getEventXFilterOutside(MotionEvent event){
        if (event.getX() < 0){
            return 0;
        }
        if (event.getX() > viewWidth){
            return viewWidth;
        }
        return event.getX();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = awesomeViewGroupList.size();
        for (int i = 0 ; i < childCount; i ++){
            AwesomeViewGroup child = awesomeViewGroupList.get(i);
            AwesomeViewGroup.AwesomeLayoutParams lp = (AwesomeViewGroup.AwesomeLayoutParams) child.getLayoutParams();
            child.layout(lp.left, lp.top, lp.right, lp.bottom);
        }
    }

    @Override
    public AwesomeViewGroup getChildView(int index) {
        return null;
    }

    @Override
    public AwesomeViewGroup getFirstShowView() {
        return null;
    }

    @Override
    public void initialShowOffset(int offsetY) {

    }

    @Override
    public void onPageChange(AwesomeViewGroup awesomeViewGroup) {

    }

    @Override
    public void scrollToX(int x) {

    }

    @Override
    public void scrollToY(int y) {

    }

    @Override
    public void scrollByX(int x) {
        for (AwesomeViewGroup awesomeViewGroup: awesomeViewGroupList){
            AwesomeViewGroup.AwesomeLayoutParams lp = (AwesomeViewGroup.AwesomeLayoutParams) awesomeViewGroup.getLayoutParams();
            lp.left += x;
            lp.right += x;
            awesomeViewGroup.layout(lp.left, lp.top, lp.right, lp.bottom);
        }

        moveXPostCheck(awesomeViewGroupList, scrollDir);
    }

    @Override
    public void scrollByY(int y) {

    }

    @Override
    public void scrollByXSmoothly(int x) {
        scrollByXSmoothly(x, 200);
    }

    @Override
    public void scrollByXSmoothly(int x, long duration) {
        ValueAnimator animator = ValueAnimator.ofInt(0, x);
        animator.setDuration(duration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            int preAniX = 0;
            int totalScroll = 0;
            boolean abortRest = false;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (status != HORIZONTAL_FLING){
                    // should abort animation...
                    abortRest = true;
                    return;
                }
                if (abortRest){
                    animation.removeAllListeners();
                    return;
                }
                int nowValue = (int) animation.getAnimatedValue();
                int offset = (nowValue - preAniX);
                newX = preX + offset;
                scrollByX(offset);
                preAniX=nowValue;
                preX = newX;
                totalScroll += offset;
                LogUtil.logError("totalScroll : " + totalScroll);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setStatus(STOP);
                LogUtil.logAwesomes(awesomeViewGroupList);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    @Override
    public void scrollToYSmoothly(int y) {

    }

    @Override
    public void onScrollStart() {

    }

    @Override
    public void onScrolling() {

    }

    @Override
    public void onScrollEnd() {

    }

    @Override
    public void onFlingStart() {

    }

    @Override
    public void onFlingEnd() {

    }

    protected class MessageHandler extends Handler{
        static final int MSG_MOVEX = 1;
        static final int MSG_MOVEY = 2;
        static final int MSG_FLINGX = 3;
        static final int MSG_FLINGY = 4;
    }
}
