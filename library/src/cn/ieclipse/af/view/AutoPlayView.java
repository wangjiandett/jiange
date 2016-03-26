/*
 * Copyright 2014-2015 ieclipse.cn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ieclipse.af.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.ieclipse.af.util.AppUtils;

/**
 * An auto play container.
 * <p>
 *     AutoPlayView default child views
 * </p>
 * <ol>
 *     <li>{@linkplain android.support.v4.view.ViewPager ViewPager}</li>
 *     <li>Indicator layout (if has), it's a horizontal {@linkplain android.widget.LinearLayout LinearLayout}</li>
 *     <li>Indicator text widget (if has) to show "current/total" text</li>
 * </ol>
 * <p>
 *     You can call {@link #setIndicatorItemLayout(int)} to set indicator item layout and call {@link
 *     #setIndicatorItemPadding(int)} to set item padding. the page indicator will changed dynamically.
 * </p>
 *
 * @author Jamling
 * @date 2015/7/15.
 */
public class AutoPlayView extends FrameLayout
        implements ViewPager.OnPageChangeListener, View.OnTouchListener {
        
    public AutoPlayView(Context context) {
        this(context, null);
    }
    
    public AutoPlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public AutoPlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AutoPlayView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }
    
    private boolean mLoop = true;
    private boolean mSmoothScroll = true;
    private boolean mPlaying;
    private long mInterval = 5000;
    private boolean mAutoStart;
    private ViewPager mViewPager;
    private LinearLayout mIndicatorLayout;
    private TextView mIndicatorTv;
    private int mIndicatorItemLayout;
    private int mIndicatorItemPadding;
    private int mPosition;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int current = mViewPager.getCurrentItem();
            int size = mViewPager.getAdapter() == null ? 0
                    : mViewPager.getAdapter().getCount();
            if (current + 1 < size) {// can next
                mViewPager.setCurrentItem(++current, mSmoothScroll);
            }
            else if (current + 1 == size && mLoop) {
                mViewPager.setCurrentItem(0, mSmoothScroll);
            }
            if (mPlaying) {
                mHandler.sendEmptyMessageDelayed(0, mInterval);
            }
        }
    };
    
    private void init(Context context, AttributeSet attrs) {
        // mViewPager = new ViewPager(context);
        // addView(mViewPager);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIndicatorItemPadding = AppUtils.dp2px(getContext(), 4); // default 4
                                                                 // dip
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            if (getChildAt(i) instanceof ViewPager) {
                mViewPager = (ViewPager) getChildAt(i);
                mViewPager.setFadingEdgeLength(0);
                addOnPageChangedListener(this);
                if (mAutoStart) {
                    start();
                }
                break;
            }
        }
        if (mViewPager != null) {
            // 监听viewpager的触摸事件
            mViewPager.setOnTouchListener(this);
        }
        // default the second layout is indicator layout
        if (size > 1) {
            View v = getChildAt(1);
            if (v instanceof LinearLayout) {
                mIndicatorLayout = (LinearLayout) v;
            }
        }
        // default the 3rd widget is indicator text view
        if (size > 2) {
            View v = getChildAt(2);
            if (v instanceof TextView) {
                mIndicatorTv = (TextView) v;
            }
        }
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_SCROLL:
                stop();
                break;
            case MotionEvent.ACTION_UP:
                start();
                break;
        }
        return false;
    }
    
    /**
     * 开启循环播放
     */
    public void start() {
        if (!mPlaying) {
            mPlaying = true;
            mHandler.sendEmptyMessageDelayed(0, mInterval);
        }
        if (getCount() > 0) {
            if (mPosition > 0) {// 非初次开启从当前位置开启
                mViewPager.setCurrentItem(mPosition);
            }
            else {// 初次开启从0位置循环
                mViewPager.setCurrentItem(0);
            }
        }
    }
    
    /**
     * 停止循环播放
     */
    public void stop() {
        mPlaying = false;
        mHandler.removeMessages(0);
    }
    
    private void addOnPageChangedListener(
            ViewPager.OnPageChangeListener listener) {
        if (listener != null) {
            mViewPager.addOnPageChangeListener(listener);
        }
    }
    
    /**
     * Set a PagerAdapter that will supply views for this pager as needed.
     * @param adapter adapter to use
     * @see android.support.v4.view.ViewPager#setAdapter(android.support.v4.view.PagerAdapter)
     */
    public void setAdapter(PagerAdapter adapter) {
        if (mViewPager != null) {
            mViewPager.setAdapter(adapter);
        }
        initIndicatorLayout();
    }
    
    /**
     * Set layout resource of the page indicator item
     * @param layoutId xml layout id
     */
    public void setIndicatorItemLayout(int layoutId) {
        this.mIndicatorItemLayout = layoutId;
    }
    
    /**
     * Set page indicator layout
     *
     * @param layout
     *            indicator layout
     */
    public void setIndicatorLayout(LinearLayout layout) {
        if (layout != null) {
            mIndicatorLayout = layout;
        }
    }
    
    /**
     * Set page indicator text widget
     * @param tv TextView widget
     */
    public void setIndicatorTextView(TextView tv) {
        if (tv != null) {
            mIndicatorTv = tv;
        }
    }
    
    public void setIndicatorItemPadding(int padding) {
        if (padding > 0) {
            this.mIndicatorItemPadding = padding;
        }
    }
    
    public ViewPager getViewPager() {
        return mViewPager;
    }
    
    public TextView getIndicatorText() {
        return mIndicatorTv;
    }
    
    public int getCurrent() {
        return mViewPager.getCurrentItem();
    }
    
    public int getCount() {
        if (mViewPager.getAdapter() != null) {
            return mViewPager.getAdapter().getCount();
        }
        return 0;
    }
    
    /**
     * 设置图片循环切换时间
     *
     * @param interval auto play interval time, micro seconds unit
     */
    public void setInterval(long interval) {
        if (interval > 0) {
            this.mInterval = interval;
        }
    }
    
    @Override
    public void onPageScrolled(int position, float positionOffset,
            int positionOffsetPixels) {
    }
    
    @Override
    public void onPageSelected(int position) {
        // 显示数字指示器
        if (mIndicatorTv != null) {
            mIndicatorTv.setText(getCurrent() + 1 + "/" + getCount());
        }
        // 显示图片指示器
        if (mIndicatorLayout != null) {
            if (getCount() > 0) {
                // 更新小点点颜色
                changePointView(mPosition, getCurrent());
                mPosition = getCurrent();
            }
            else {
                mIndicatorLayout.setVisibility(View.GONE);
            }
        }
    }
    
    @Override
    public void onPageScrollStateChanged(int state) {
    }
    
    /**
     * Initialize the indicator layout, it will generate indicator item view dynamically.
     * <p>
     *     Please call the method after your pager adapter changed.
     * </p>
     */
    public void initIndicatorLayout() {
        if (mIndicatorLayout != null) {
            if (mIndicatorLayout.getChildCount() > 0) {
                mIndicatorLayout.removeAllViews();
            }
            if (getCount() <= 1) {
                return;
            }
            for (int i = 0; i < getCount(); i++) {
                View item = getIndicatorItemView(i);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) {
                    params.leftMargin = mIndicatorItemPadding;
                }
                mIndicatorLayout.addView(item, params);
            }
        }
    }
    
    protected View getIndicatorItemView(int position) {
        if (mIndicatorItemLayout > 0) {
            View v = View.inflate(getContext(), mIndicatorItemLayout, null);
            return v;
        }
        return null;
    }
    
    /**
     * 改变小点
     *
     */
    private void changePointView(int oldPosition, int newPosition) {
        if (mIndicatorLayout != null) {
            View old = null;
            View current = null;
            if (oldPosition < mIndicatorLayout.getChildCount()) {
                old = mIndicatorLayout.getChildAt(oldPosition);
            }
            if (newPosition < mIndicatorLayout.getChildCount()) {
                current = mIndicatorLayout.getChildAt(newPosition);
            }
            if (old != null) {
                old.setSelected(false);
            }
            if (current != null) {
                current.setSelected(true);
            }
        }
    }
    
}
