/*
 * Copyright (C) 2006-2015 li.jamling@gmail.com rights reserved
 */
package cn.ieclipse.af.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * ViewPager的抽象Adapter
 *
 * @author lijiaming2
 * @date 2015/5/14
 */
public abstract class AfPagerAdapter<T> extends PagerAdapter {
    
    protected Context mContext;
    protected List<T> mDataList;
    private int mRemoveCount;
    
    public AfPagerAdapter(Context context) {
        this.mContext = context;
    }
    
    public AfPagerAdapter() {
    
    }
    
    public abstract int getLayout();
    
    public abstract void onUpdateView(View convertView, int position);
    
    // 以下方法均已实现
    
    /**
     * Change adapter data, then call {@link #notifyDataSetChanged(boolean)} to
     * refresh.
     * 
     * @param list
     *            new data
     * @see #notifyDataSetChanged(boolean)
     */
    public void setDataList(List<T> list) {
        if (list == null) {
            this.mDataList = new ArrayList<T>(0);
        }
        else {
            this.mDataList = list;
        }
        if (this.mDataList == null) {
            this.mDataList = new ArrayList<T>(0);
        }
    }
    
    public void clear() {
        if (mDataList != null) {
            mDataList.clear();
        }
    }
    
    /**
     * 
     * This method should be called by the application if the data backing this
     * adapter has changed and associated views should update.
     *
     * @param force
     *            whether re {@link #instantiateItem(ViewGroup, int)} or not
     */
    public void notifyDataSetChanged(boolean force) {
        if (force) {
            mRemoveCount = getCount();
        }
        notifyDataSetChanged();
    }
    
    public T getItem(int position) {
        if (mDataList == null) {
            return null;
        }
        return mDataList.get(position);
    }
    
    @Override
    public int getCount() {
        return mDataList == null ? 0 : mDataList.size();
    }
    
    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view == o;
    }
    
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = LayoutInflater.from(container.getContext())
                .inflate(getLayout(), container, false);
        container.addView(view, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        try {
            onUpdateView(view, position);
        } catch (Exception e){

        }
        return view;
    }
    
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
    
    @Override
    public int getItemPosition(Object object) {
        if (mRemoveCount-- > 0) {
            return POSITION_NONE;
        }
        return super.getItemPosition(object);
    }
}
