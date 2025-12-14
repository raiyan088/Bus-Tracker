package com.rr.bubtbustracker.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rr.bubtbustracker.R;

public class CustomInputView extends LinearLayout {

    private ImageView logoView;
    private EditText editText;

    public CustomInputView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public CustomInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CustomInputView);

            int defPad = ta.getDimensionPixelSize(R.styleable.CustomInputView_padding, 0);
            int padStart = ta.getDimensionPixelSize(R.styleable.CustomInputView_paddingStart, defPad);
            int padEnd = ta.getDimensionPixelSize(R.styleable.CustomInputView_paddingEnd, defPad);
            int padTop = ta.getDimensionPixelSize(R.styleable.CustomInputView_paddingTop, defPad);
            int padBottom = ta.getDimensionPixelSize(R.styleable.CustomInputView_paddingBottom, defPad);
            int txtSize = ta.getDimensionPixelSize(R.styleable.CustomInputView_textSize, 16);
            boolean isEmail = ta.getBoolean(R.styleable.CustomInputView_isEmail, false);
            boolean isPasswordField = ta.getBoolean(R.styleable.CustomInputView_isPassword, false);

            int logoRes = ta.getResourceId(R.styleable.CustomInputView_logoSrc, 0);
            int secondaryLogoRes = ta.getResourceId(R.styleable.CustomInputView_secondaryLogoSrc, logoRes);
            int bgRes = ta.getResourceId(R.styleable.CustomInputView_background, 0);
            float elev = ta.getDimension(R.styleable.CustomInputView_elevation, 0);
            String hint = ta.getString(R.styleable.CustomInputView_hintText);
            ta.recycle();

            logoView = new ImageView(context);
            if (logoRes != 0) logoView.setImageResource(logoRes);
            LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
            logoView.setLayoutParams(logoParams);
            addView(logoView);

            editText = new EditText(context);
            editText.setHint(hint != null ? hint : "");
            editText.setBackground(null);
            editText.setTextColor(Color.BLACK);
            editText.setHintTextColor(Color.parseColor("#757575"));
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, txtSize);
            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            editParams.setMarginStart(dpToPx(12));
            editParams.setMarginEnd(dpToPx(28));
            editText.setLayoutParams(editParams);
            addView(editText);


            if (isPasswordField) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                logoView.setImageResource(logoRes);
                logoView.setOnClickListener(v -> togglePasswordVisibilityWithAnimation(logoRes, secondaryLogoRes));
            } else if (isEmail) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT);
            }

            editText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    animateFocus(editText, 1.03f);
                    animateFocus(logoView, 1.1f);
                }
            });

            setPadding(padStart, padTop, padEnd, padBottom);
            if (bgRes != 0) setBackgroundResource(bgRes);
            setElevation(elev);
        }
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    public String getText() {
        if (editText == null) return "";
        Editable editable = editText.getText();
        if (editable == null) return "";
        return editable.toString();
    }

    public void clearText() {
        if (editText == null) return;
        editText.setText("");
    }

    private void togglePasswordVisibilityWithAnimation(int hideIcon, int showIcon) {
        if (logoView == null || editText == null) return;

        logoView.animate().scaleX(0.7f).scaleY(0.7f).setDuration(100).withEndAction(() -> {
            if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                logoView.setImageResource(showIcon);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                logoView.setImageResource(hideIcon);
            }
            editText.setSelection(editText.getText().length());
            logoView.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
        }).start();
    }

    private void animateFocus(View target, float value) {
        target.animate()
                .scaleX(value)
                .scaleY(value)
                .setDuration(100)
                .withEndAction(() -> target.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }
}

