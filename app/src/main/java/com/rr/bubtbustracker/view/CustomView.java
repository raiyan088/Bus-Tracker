package com.rr.bubtbustracker.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.rr.bubtbustracker.R;

public class CustomView extends CardView {

    private int bgColor = Color.WHITE;
    private int strokeColor = 0xFFE0E0E0;
    private int strokeWidth = 8;
    private boolean clickEffect = true;
    private Drawable originalBackground;
    private GradientDrawable shapeDrawable;
    private int rippleColor = Color.parseColor("#AA22F074");

    public CustomView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public CustomView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @SuppressLint("CustomViewStyleable")
    private void init(Context context, @Nullable AttributeSet attrs) {
        setCardElevation(6f);
        setRadius(0f);
        setCardBackgroundColor(Color.TRANSPARENT);

        float topLeft = 0f, topRight = 0f, bottomRight = 0f, bottomLeft = 0f, cornerRadius = -1f;
        boolean usePadding = true;

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CustomView);
            cornerRadius = ta.getDimension(R.styleable.CustomView_cornerRadius, -1f);
            topLeft = ta.getDimension(R.styleable.CustomView_cornerTopLeft, 0f);
            topRight = ta.getDimension(R.styleable.CustomView_cornerTopRight, 0f);
            bottomRight = ta.getDimension(R.styleable.CustomView_cornerBottomRight, 0f);
            bottomLeft = ta.getDimension(R.styleable.CustomView_cornerBottomLeft, 0f);
            bgColor = ta.getColor(R.styleable.CustomView_bgColor, Color.WHITE);
            strokeColor = ta.getColor(R.styleable.CustomView_strokeColor, 0xFFE0E0E0);
            strokeWidth = (int) ta.getDimension(R.styleable.CustomView_strokeWidth, 8f);
            usePadding = ta.getBoolean(R.styleable.CustomView_cardUsePadding, true);
            clickEffect = ta.getBoolean(R.styleable.CustomView_clickEffect, true);
            rippleColor = ta.getColor(R.styleable.CustomView_rippleColor, Color.parseColor("#AA22F074"));
            ta.recycle();
        }

        setUseCompatPadding(usePadding);

        shapeDrawable = new GradientDrawable();
        shapeDrawable.setColor(bgColor);

        if (cornerRadius >= 0) {
            shapeDrawable.setCornerRadius(cornerRadius);
        } else {
            shapeDrawable.setCornerRadii(new float[]{
                    topLeft, topLeft, topRight, topRight,
                    bottomRight, bottomRight, bottomLeft, bottomLeft
            });
        }

        shapeDrawable.setStroke(strokeWidth, strokeColor);
        if (clickEffect) {
            RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(rippleColor), shapeDrawable, null);
            originalBackground = rippleDrawable;
            setBackground(rippleDrawable);
            setClickable(true);
        } else {
            originalBackground = shapeDrawable;
            setBackground(shapeDrawable);
            setClickable(false);
        }
    }

    public void enable() {
        shapeDrawable.setColor(bgColor);
        shapeDrawable.setStroke(strokeWidth, strokeColor);
        setBackground(originalBackground);
        setEnabled(true);
    }

    public void disable() {
        shapeDrawable.setColor(Color.LTGRAY);
        shapeDrawable.setStroke(0, Color.TRANSPARENT);
        setBackground(shapeDrawable);
        setEnabled(false);
    }
}
