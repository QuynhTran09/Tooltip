/*
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2016. Viнt@rь
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tooltip;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.widget.TextViewCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Tooltip
 */
public final class Tooltip {

    private final boolean isCancelable;
    private final boolean isDismissOnClick;

    private final int mGravity;

    private final float mMargin;

    private final View mAnchorView;
    private final PopupWindow mPopupWindow;

    private LinearLayout mContentView;
    private ImageView mArrowView;

    private Tooltip(Builder builder) {
        mGravity = builder.mGravity;
        mMargin = builder.mMargin;
        mAnchorView = builder.mAnchorView;
        isCancelable = builder.isCancelable;
        isDismissOnClick = builder.isDismissOnClick;

        mPopupWindow = new PopupWindow(builder.mContext);
        mPopupWindow.setBackgroundDrawable(null);
        mPopupWindow.setClippingEnabled(false);
        mPopupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setContentView(getContentView(builder));
        mPopupWindow.setOutsideTouchable(builder.isCancelable);
        mPopupWindow.setOnDismissListener(builder.mOnDismissListener);
    }

    private View getContentView(Builder builder) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(builder.mBackgroundColor);
        drawable.setCornerRadius(builder.mCornerRadius);

        int padding = (int) builder.mPadding;

        TextView textView = new TextView(builder.mContext);
        textView.setText(builder.mText);
        textView.setTextSize(TypedValue.TYPE_NULL, builder.mTextSize);
        textView.setTextColor(builder.mTextColor);
        textView.setLineSpacing(builder.mLineSpacingExtra, builder.mLineSpacingMultiplier);
        textView.setPadding(padding, padding, padding, padding);
        textView.setTypeface(builder.mTypeface, builder.mTextStyle);
        TextViewCompat.setTextAppearance(textView, builder.mTextAppearance);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            textView.setBackground(drawable);
        } else {
            //noinspection deprecation
            textView.setBackgroundDrawable(drawable);
        }

        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
        textViewParams.gravity = Gravity.CENTER;
        textView.setLayoutParams(textViewParams);

        mArrowView = new ImageView(builder.mContext);
        mArrowView.setImageDrawable(new ArrowDrawable(builder.mBackgroundColor, builder.mGravity));

        LinearLayout.LayoutParams arrowLayoutParams;
        if (mGravity == Gravity.TOP || mGravity == Gravity.BOTTOM) {
            arrowLayoutParams = new LinearLayout.LayoutParams((int) builder.mArrowWidth, (int) builder.mArrowHeight, 0);
        } else {
            arrowLayoutParams = new LinearLayout.LayoutParams((int) builder.mArrowHeight, (int) builder.mArrowWidth, 0);
        }
        arrowLayoutParams.gravity = Gravity.CENTER;
        mArrowView.setLayoutParams(arrowLayoutParams);

        mContentView = new LinearLayout(builder.mContext);
        mContentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mContentView.setOrientation(mGravity == Gravity.START || mGravity == Gravity.END ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        padding = (int) Utils.pxFromDp(5);
        switch (mGravity) {
            case Gravity.START:
                mContentView.setPadding(0, 0, padding, 0);
                break;
            case Gravity.TOP:
            case Gravity.BOTTOM:
                mContentView.setPadding(padding, 0, padding, 0);
                break;
            case Gravity.END:
                mContentView.setPadding(padding, 0, 0, 0);
                break;
        }

        if (mGravity == Gravity.TOP || mGravity == Gravity.START) {
            mContentView.addView(textView);
            mContentView.addView(mArrowView);
        } else {
            mContentView.addView(mArrowView);
            mContentView.addView(textView);
        }

        if (builder.isCancelable || builder.isDismissOnClick) {
            mContentView.setOnTouchListener(mTouchListener);
        }
        return mContentView;
    }

    public boolean isShowing() {
        return mPopupWindow.isShowing();
    }

    public void show() {
        if (!isShowing()) {
            mContentView.getViewTreeObserver().addOnGlobalLayoutListener(mLocationLayoutListener);

            mAnchorView.post(new Runnable() {
                @Override
                public void run() {
                    mPopupWindow.showAsDropDown(mAnchorView);
                }
            });
        }
    }

    public void dismiss() {
        mPopupWindow.dismiss();
    }

    private PointF calculatePopupLocation() {
        PointF location = new PointF();

        final RectF anchorRect = Utils.calculateRectInWindow(mAnchorView);
        final PointF anchorCenter = new PointF(anchorRect.centerX(), anchorRect.centerY());

        switch (mGravity) {
            case Gravity.START:
                location.x = anchorRect.left - mContentView.getWidth() - mMargin;
                location.y = anchorCenter.y - mContentView.getHeight() / 2f;
                break;
            case Gravity.END:
                location.x = anchorRect.right + mMargin;
                location.y = anchorCenter.y - mContentView.getHeight() / 2f;
                break;
            case Gravity.TOP:
                location.x = anchorCenter.x - mContentView.getWidth() / 2f;
                location.y = anchorRect.top - mContentView.getHeight() - mMargin;
                break;
            case Gravity.BOTTOM:
                location.x = anchorCenter.x - mContentView.getWidth() / 2f;
                location.y = anchorRect.bottom + mMargin;
                break;
            default:
                throw new IllegalArgumentException("Gravity must have be START, END, TOP or BOTTOM.");
        }

        return location;
    }

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (isCancelable && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                dismiss();
            } else if (isDismissOnClick && event.getAction() == MotionEvent.ACTION_UP) {
                dismiss();
            }
            return true;
        }
    };

    private final ViewTreeObserver.OnGlobalLayoutListener mLocationLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            Utils.removeOnGlobalLayoutListener(mContentView, this);

            mContentView.getViewTreeObserver().addOnGlobalLayoutListener(mArrowLayoutListener);
            PointF location = calculatePopupLocation();
            mPopupWindow.setClippingEnabled(true);
            mPopupWindow.update((int) location.x, (int) location.y, mPopupWindow.getWidth(), mPopupWindow.getHeight());
            mContentView.requestLayout();
        }
    };

    private final ViewTreeObserver.OnGlobalLayoutListener mArrowLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            Utils.removeOnGlobalLayoutListener(mContentView, this);

            RectF anchorRect = Utils.calculateRectOnScreen(mAnchorView);
            RectF contentViewRect = Utils.calculateRectOnScreen(mContentView);
            float x, y;
            if (mGravity == Gravity.BOTTOM || mGravity == Gravity.TOP) {
                x = mContentView.getPaddingLeft() + Utils.pxFromDp(2);
                float centerX = (contentViewRect.width() / 2f) - (mArrowView.getWidth() / 2f);
                float newX = centerX - (contentViewRect.centerX() - anchorRect.centerX());
                if (newX > x) {
                    if (newX + mArrowView.getWidth() + x > contentViewRect.width()) {
                        x = contentViewRect.width() - mArrowView.getWidth() - x;
                    } else {
                        x = newX;
                    }
                }
                y = mArrowView.getTop();
                y = y + (mGravity == Gravity.TOP ? -1 : +1);
            } else {
                y = mContentView.getPaddingTop() + Utils.pxFromDp(2);
                float centerY = (contentViewRect.height() / 2f) - (mArrowView.getHeight() / 2f);
                float newY = centerY - (contentViewRect.centerY() - anchorRect.centerY());
                if (newY > y) {
                    if (newY + mArrowView.getHeight() + y > contentViewRect.height()) {
                        y = contentViewRect.height() - mArrowView.getHeight() - y;
                    } else {
                        y = newY;
                    }
                }
                x = mArrowView.getLeft();
                x = x + (mGravity == Gravity.START ? -1 : +1);
            }
            mArrowView.setX(x);
            mArrowView.setY(y);
            mContentView.requestLayout();
        }
    };

    public static final class Builder {
        private boolean isDismissOnClick;
        private boolean isCancelable;

        private int mGravity;
        private int mBackgroundColor;
        private int mTextStyle;
        private int mTextAppearance;

        private float mCornerRadius;
        private float mMargin;
        private float mPadding;
        private float mArrowHeight;
        private float mArrowWidth;
        private float mTextSize;
        private float mLineSpacingExtra;
        private float mLineSpacingMultiplier = 1f;

        private Context mContext;
        private View mAnchorView;
        private String mText;
        private ColorStateList mTextColor;
        private Typeface mTypeface = Typeface.DEFAULT;
        private OnDismissListener mOnDismissListener;

        public Builder(@NonNull Context context, @NonNull MenuItem anchorMenuItem) {
            this(context, anchorMenuItem, 0);
        }

        public Builder(@NonNull Context context, @NonNull MenuItem anchorMenuItem, @StyleRes int resId) {
            View anchorView = anchorMenuItem.getActionView();
            if (anchorView == null) {
                TooltipActionView tooltipActionView = new TooltipActionView(context);
                tooltipActionView.setMenuItem(anchorMenuItem);

                anchorMenuItem.setActionView(tooltipActionView);

                init(context, tooltipActionView, resId);
            } else {
                if (anchorView instanceof TooltipActionView) {
                    TooltipActionView tooltipActionView = (TooltipActionView) anchorView;
                    tooltipActionView.setMenuItem(anchorMenuItem);
                }

                init(context, anchorView, resId);
            }
        }

        public Builder(@NonNull View anchorView) {
            this(anchorView, 0);
        }

        public Builder(@NonNull View anchorView, @StyleRes int resId) {
            init(anchorView.getContext(), anchorView, resId);
        }

        private void init(@NonNull Context context, @NonNull View anchorView, @StyleRes int resId) {
            mContext = context;
            mAnchorView = anchorView;

            TypedArray a = context.obtainStyledAttributes(resId, R.styleable.Tooltip);

            isCancelable = a.getBoolean(R.styleable.Tooltip_cancelable, false);
            isDismissOnClick = a.getBoolean(R.styleable.Tooltip_dismissOnClick, false);
            mBackgroundColor = a.getColor(R.styleable.Tooltip_backgroundColor, Color.GRAY);
            mCornerRadius = a.getDimension(R.styleable.Tooltip_cornerRadius, -1);
            mMargin = a.getDimension(R.styleable.Tooltip_margin, -1);
            mPadding = a.getDimension(R.styleable.Tooltip_android_padding, -1);
            mGravity = a.getInteger(R.styleable.Tooltip_android_gravity, Gravity.BOTTOM);
            mArrowHeight = a.getDimension(R.styleable.Tooltip_arrowHeight, -1);
            mArrowWidth = a.getDimension(R.styleable.Tooltip_arrowWidth, -1);
            mText = a.getString(R.styleable.Tooltip_android_text);
            mTextSize = a.getDimension(R.styleable.Tooltip_android_textSize, -1);
            mTextColor = a.getColorStateList(R.styleable.Tooltip_android_textColor);
            mTextStyle = a.getInteger(R.styleable.Tooltip_android_textStyle, -1);
            mTextAppearance = a.getResourceId(R.styleable.Tooltip_textAppearance, -1);
            mLineSpacingExtra = a.getDimensionPixelSize(R.styleable.Tooltip_android_lineSpacingExtra, 0);
            mLineSpacingMultiplier = a.getFloat(R.styleable.Tooltip_android_lineSpacingMultiplier, mLineSpacingMultiplier);

            final String fontFamily = a.getString(R.styleable.Tooltip_android_fontFamily);
            final int typefaceIndex = a.getInt(R.styleable.Tooltip_android_typeface, -1);
            mTypeface = getTypefaceFromAttr(fontFamily, typefaceIndex, mTextStyle);

            a.recycle();
        }

        /**
         * Sets whether the tooltip is cancelable or not. Default is false.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setCancelable(boolean cancelable) {
            isCancelable = cancelable;
            return this;
        }

        /**
         * Sets whether the tooltip is dismissing on click or not. Default is false.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setDismissOnClick(boolean isDismissOnClick) {
            this.isDismissOnClick = isDismissOnClick;
            return this;
        }

        /**
         * Sets the tooltip background color.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setBackgroundColor(@ColorInt int color) {
            mBackgroundColor = color;
            return this;
        }

        /**
         * Sets the tooltip background drawable corner radius from resource.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setCornerRadius(@DimenRes int resId) {
            return setCornerRadius(mContext.getResources().getDimension(resId));
        }

        /**
         * Sets the tooltip background drawable corner radius.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setCornerRadius(float radius) {
            mCornerRadius = radius;
            return this;
        }

        /**
         * Sets the tooltip arrow height from resource.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setArrowHeight(@DimenRes int resId) {
            return setArrowHeight(mContext.getResources().getDimension(resId));
        }

        /**
         * Sets the tooltip arrow height.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setArrowHeight(float height) {
            mArrowHeight = height;
            return this;
        }

        /**
         * Sets the tooltip arrow width from resource.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setArrowWidth(@DimenRes int resId) {
            return setArrowWidth(mContext.getResources().getDimension(resId));
        }

        /**
         * Sets the tooltip arrow width.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setArrowWidth(float width) {
            mArrowWidth = width;
            return this;
        }

        /**
         * Sets the tooltip margin from resource.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setMargin(@DimenRes int resId) {
            return setMargin(mContext.getResources().getDimension(resId));
        }

        /**
         * Sets the tooltip margin.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setMargin(float margin) {
            mMargin = margin;
            return this;
        }

        /**
         * Sets the tooltip padding from resource.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setPadding(@DimenRes int resId) {
            return setPadding(mContext.getResources().getDimension(resId));
        }

        /**
         * Sets the tooltip padding.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setPadding(float padding) {
            mPadding = padding;
            return this;
        }

        /**
         * Sets tooltip gravity.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setGravity(int gravity) {
            mGravity = gravity;
            return this;
        }

        /**
         * Sets tooltip text from resource.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setText(@StringRes int resId) {
            return setText(mContext.getString(resId));
        }

        /**
         * Sets tooltip text.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setText(String text) {
            mText = text;
            return this;
        }

        /**
         * Sets tooltip text size from resource.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setTextSize(@DimenRes int resId) {
            mTextSize = mContext.getResources().getDimension(resId);
            return this;
        }

        /**
         * Sets tooltip text size.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setTextSize(float size) {
            mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, mContext.getResources().getDisplayMetrics());
            return this;
        }

        /**
         * Sets tooltip text color.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setTextColor(@ColorInt int color) {
            mTextColor = ColorStateList.valueOf(color);
            return this;
        }

        /**
         * Sets tooltip text style.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setTextStyle(int style) {
            mTextStyle = style;
            return this;
        }

        /**
         * Sets tooltip text appearance from the specified style resource.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setTextAppearance(@StyleRes int resId) {
            mTextAppearance = resId;
            return this;
        }

        /**
         * Sets tooltip line spacing. Each line will have its height
         * multiplied by <code>mult</code> and have <code>add</code> added to it.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setLineSpacing(@DimenRes int addResId, float mult) {
            mLineSpacingExtra = mContext.getResources().getDimensionPixelSize(addResId);
            mLineSpacingMultiplier = mult;
            return this;
        }

        /**
         * Sets tooltip line spacing. Each line will have its height
         * multiplied by <code>mult</code> and have <code>add</code> added to it.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setLineSpacing(float add, float mult) {
            mLineSpacingExtra = add;
            mLineSpacingMultiplier = mult;
            return this;
        }

        /**
         * Sets tooltip text typeface.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setTypeface(Typeface typeface) {
            mTypeface = typeface;
            return this;
        }

        /**
         * Sets the listener to be called when the window is dismissed.
         *
         * @param listener The listener.
         */
        public Builder setOnDismissListener(OnDismissListener listener) {
            mOnDismissListener = listener;
            return this;
        }

        /**
         * Creates a {@link Tooltip} with the arguments supplied to this builder. It does not
         * {@link Tooltip#show()} the tooltip. This allows the user to do any extra processing
         * before displaying the tooltip. Use {@link #show()} if you don't have any other processing
         * to do and want this to be created and displayed.
         */
        public Tooltip build() {
            if (mArrowHeight == -1) {
                mArrowHeight = mContext.getResources().getDimension(R.dimen.default_tooltip_arrow_height);
            }
            if (mArrowWidth == -1) {
                mArrowWidth = mContext.getResources().getDimension(R.dimen.default_tooltip_arrow_width);
            }
            if (mMargin == -1) {
                mMargin = mContext.getResources().getDimension(R.dimen.default_tooltip_margin);
            }
            if (mPadding == -1) {
                mPadding = mContext.getResources().getDimension(R.dimen.default_tooltip_padding);
            }
            if (mTextSize == -1) {
                mTextSize = mContext.getResources().getDimension(R.dimen.default_tooltip_text_size);
            }
            if (mTextColor == null) {
                mTextColor = ColorStateList.valueOf(Color.WHITE);
            }
            return new Tooltip(this);
        }

        /**
         * Builds a {@link Tooltip} with builder attributes and {@link Tooltip#show()}'s the tooltip.
         */
        public Tooltip show() {
            Tooltip tooltip = build();
            tooltip.show();
            return tooltip;
        }

        private Typeface getTypefaceFromAttr(String familyName, int typefaceIndex, int styleIndex) {
            Typeface tf = null;
            if (familyName != null) {
                tf = Typeface.create(familyName, styleIndex);
                if (tf != null) {
                    return tf;
                }
            }
            switch (typefaceIndex) {
                case 1: // SANS
                    tf = Typeface.SANS_SERIF;
                    break;
                case 2: // SERIF
                    tf = Typeface.SERIF;
                    break;
                case 3: // MONOSPACE
                    tf = Typeface.MONOSPACE;
                    break;
            }
            return tf;
        }
    }
}
