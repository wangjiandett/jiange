/*
 * (C) Copyright 2011-2013 li.jamling@gmail.com. 
 *
 * This software is the property of li.jamling@gmail.com.
 * You have to accept the terms in the license file before use.
 *
 */
package cn.ieclipse.af.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * RatioImageView can display image as a grid base on fix ratio. If the ratio is
 * 1 (default), so the ImageView looks like a square ImageView.
 * 
 * @author Jamling
 * 
 */
public class RatioImageView extends ImageView {
    
    private float mRatio = 1.0f;
    
    /**
     * @param context
     */
    public RatioImageView(Context context) {
        this(context, null);
    }
    
    /**
     * @param context
     * @param attrs
     */
    public RatioImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    /**
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public RatioImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        // if (attrs != null) {
        // TypedArray a = context.obtainStyledAttributes(attrs,
        // R.styleable.RatioImageView);
        // mRatio = a.getInteger(R.styleable.RatioImageView_ratio, 0);
        // a.recycle();
        // }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        if (mRatio > 0) {
            setMeasuredDimension(width, (int) (width * mRatio));
        }
    }
}
