package com.fxyan.demo.rulerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.widget.Scroller;

/**
 * @author fxYan
 */
public final class RulerView extends View {

    private final int DEFAULT_SCALE_LINE_COLOR = Color.parseColor("#ffffff");
    private final float DEFAULT_LARGE_SCALE_HEIGHT = 50f;
    private final float DEFAULT_MIN_SCALE_HEIGHT = 25f;
    private final float DEFAULT_SCALE_WIDTH = 50;

    private final int DEFAULT_INDICATOR_LINE_COLOR = Color.parseColor("#fff08d");

    private final float DEFAULT_STROKE_WIDTH = 2;

    private final int DEFAULT_MAX_VALUE = 3000;
    private final int DEFAULT_MIN_VALUE = 500;
    private final int DEFAULT_SCALE_VALUE = 100;
    private final int DEFAULT_LARGE_SCALE_INTERVAL = 5;

    private final int DEFAULT_LARGE_SCALE_TEXT_COLOR = Color.parseColor("#ffffff");
    private final float DEFAULT_LARGE_SCALE_TEXT_SIZE = 12;

    private Paint scaleLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float largeScaleHeight = DEFAULT_LARGE_SCALE_HEIGHT;
    private float minScaleHeight = DEFAULT_MIN_SCALE_HEIGHT;
    private float scaleWidth = DEFAULT_SCALE_WIDTH;

    private Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap indicatorBitmap;
    private Rect indicatorSrc;
    private Rect indicatorDst;

    private float strokeWidth = DEFAULT_STROKE_WIDTH;

    private int maxValue = DEFAULT_MAX_VALUE;
    private int minValue = DEFAULT_MIN_VALUE;
    private int scaleValue = DEFAULT_SCALE_VALUE;
    private int lineCount;
    private int largeScaleInterval = DEFAULT_LARGE_SCALE_INTERVAL;

    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private float largeScaleTextSize = DEFAULT_LARGE_SCALE_TEXT_SIZE;
    private int largeScaleTextColor = DEFAULT_LARGE_SCALE_TEXT_COLOR;

    private DisplayMetrics metrics = new DisplayMetrics();

    private Scroller scroller;

    private float downX;
    private float leftOffset;
    private int selectIndex;

    private VelocityTracker tracker;
    private OnValueChangedListener listener;

    private int state;

    public RulerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (manager != null) {
            manager.getDefaultDisplay().getMetrics(metrics);
        }

        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.RulerView);
        int scaleLineColor = DEFAULT_SCALE_LINE_COLOR;
        if (array.hasValue(R.styleable.RulerView_scaleLineColor)) {
            scaleLineColor = array.getColor(R.styleable.RulerView_scaleLineColor, DEFAULT_SCALE_LINE_COLOR);
        }
        scaleLinePaint.setColor(scaleLineColor);
        indicatorPaint.setColor(DEFAULT_INDICATOR_LINE_COLOR);

        if (array.hasValue(R.styleable.RulerView_largeScaleHeight)) {
            largeScaleHeight = array.getDimension(R.styleable.RulerView_largeScaleHeight, DEFAULT_LARGE_SCALE_HEIGHT);
        }
        if (array.hasValue(R.styleable.RulerView_minScaleHeight)) {
            minScaleHeight = array.getDimension(R.styleable.RulerView_minScaleHeight, DEFAULT_MIN_SCALE_HEIGHT);
        }

        if (array.hasValue(R.styleable.RulerView_scaleWidth)) {
            scaleWidth = array.getDimension(R.styleable.RulerView_scaleWidth, DEFAULT_SCALE_WIDTH);
        }
        if (array.hasValue(R.styleable.RulerView_strokeWidth)) {
            strokeWidth = array.getDimension(R.styleable.RulerView_strokeWidth, DEFAULT_STROKE_WIDTH);
        }
        scaleLinePaint.setStrokeWidth(strokeWidth);
        indicatorPaint.setStrokeWidth(strokeWidth * 2);
        if (array.hasValue(R.styleable.RulerView_maxValue)) {
            maxValue = array.getInt(R.styleable.RulerView_maxValue, DEFAULT_MAX_VALUE);
        }
        if (array.hasValue(R.styleable.RulerView_minValue)) {
            minValue = array.getInt(R.styleable.RulerView_minValue, DEFAULT_MIN_VALUE);
        }
        if (array.hasValue(R.styleable.RulerView_scaleValue)) {
            scaleValue = array.getInt(R.styleable.RulerView_scaleValue, DEFAULT_SCALE_VALUE);
        }
        lineCount = (maxValue - minValue) / scaleValue + 1;
        if (array.hasValue(R.styleable.RulerView_largeScaleInterval)) {
            largeScaleInterval = array.getInt(R.styleable.RulerView_largeScaleInterval, DEFAULT_LARGE_SCALE_INTERVAL);
        }
        if (array.hasValue(R.styleable.RulerView_largeScaleTextSize)) {
            largeScaleTextSize = array.getDimension(R.styleable.RulerView_largeScaleTextSize, DEFAULT_LARGE_SCALE_TEXT_SIZE);
        }
        textPaint.setTextSize(largeScaleTextSize);
        if (array.hasValue(R.styleable.RulerView_largeScaleTextColor)) {
            largeScaleTextColor = array.getColor(R.styleable.RulerView_largeScaleTextColor, DEFAULT_LARGE_SCALE_TEXT_COLOR);
        }
        textPaint.setColor(largeScaleTextColor);
        array.recycle();

        indicatorSrc = new Rect();
        indicatorDst = new Rect();

        scroller = new Scroller(getContext());
    }

    public void setListener(OnValueChangedListener listener) {
        this.listener = listener;
    }

    public void setBorderValue(int maxValue, int minValue, int value) {
        this.maxValue = maxValue;
        this.minValue = minValue;
        this.lineCount = (this.maxValue - this.minValue) / scaleValue + 1;
        selectIndex = (value - this.minValue) / scaleValue;
        leftOffset = (scaleWidth + strokeWidth) * selectIndex;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        indicatorBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_tab_apply_coin);
        indicatorSrc.left = 0;
        indicatorSrc.top = 0;
        indicatorSrc.right = indicatorBitmap.getWidth();
        indicatorSrc.bottom = indicatorBitmap.getHeight();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (indicatorBitmap != null) {
            indicatorBitmap.recycle();
            indicatorBitmap = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (listener != null) {
            listener.onValueChanged(selectIndex * scaleValue + minValue);
        }
        drawLine(canvas);
        drawIndicator(canvas);
    }

    private void drawLine(Canvas canvas) {
        canvas.save();
        canvas.translate(-leftOffset, 0);
        int cx = getWidth() / 2;
        int startX = cx;
        for (int i = 0; i < lineCount; i++) {
            if (i % largeScaleInterval == 0) {
                String text = String.valueOf(minValue + i * scaleValue);
                float width = textPaint.measureText(text);
                canvas.drawText(text, startX - width / 2, getHeight() - largeScaleHeight * 1.5f, textPaint);
                canvas.drawLine(startX, getHeight(), startX, getHeight() - largeScaleHeight, scaleLinePaint);
            } else {
                canvas.drawLine(startX, getHeight(), startX, getHeight() - minScaleHeight, scaleLinePaint);
            }
            startX = startX + (int) (strokeWidth + scaleWidth + 0.5f);
        }
        canvas.restore();
    }

    private void drawIndicator(Canvas canvas) {
        int cx = getWidth() / 2;
        indicatorDst.left = cx - indicatorBitmap.getWidth() / 2;
        indicatorDst.top = 0;
        indicatorDst.right = indicatorDst.left + indicatorBitmap.getWidth();
        indicatorDst.bottom = indicatorBitmap.getHeight();
        canvas.drawBitmap(indicatorBitmap, indicatorSrc, indicatorDst, indicatorPaint);

        canvas.drawLine(cx, indicatorBitmap.getHeight(), cx, getHeight(), indicatorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                tracker = VelocityTracker.obtain();
                scroller.abortAnimation();
                downX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                state = State.MOVING;
                getParent().requestDisallowInterceptTouchEvent(true);

                tracker.addMovement(event);
                tracker.computeCurrentVelocity(1000);

                float moveX = event.getX();
                float offsetX = moveX - downX;
                leftOffset -= offsetX / 1.5f;
                selectIndex = (int) (leftOffset / (strokeWidth + scaleWidth) + 0.5f);
                adjustSelectIndex();
                downX = moveX;
                invalidate();

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);

                int xVelocity = (int) tracker.getXVelocity();

                int startX = (int) (leftOffset + 0.5f);
                state = State.FLING;
                scroller.fling(startX, 0, -xVelocity, 0, Integer.MIN_VALUE, Integer.MAX_VALUE,
                        0, 0);
                invalidate();

                tracker.clear();
                tracker.recycle();

                break;
            default:
        }
        return true;
    }

    private void adjustSelectIndex() {
        if (selectIndex < 0) {
            selectIndex = 0;
        } else if (selectIndex > lineCount - 1) {
            selectIndex = lineCount - 1;
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            leftOffset = scroller.getCurrX();
            if (State.FLING == state) {
                selectIndex = (int) (leftOffset / (strokeWidth + scaleWidth) + 0.5f);
                adjustSelectIndex();
            }
            invalidate();
        } else {
            if (State.FLING == state) {
                int tempX = selectIndex * (int) (strokeWidth + scaleWidth + 0.5f);
                leftOffset = scroller.getFinalX();
                int startX = (int) (leftOffset + 0.5f);
                int dx = startX - tempX;
                state = State.SCROLLER;
                scroller.startScroll(startX, 0, -dx, 0, 150);
                invalidate();
            } else if (State.SCROLLER == state) {
                leftOffset = scroller.getFinalX();
                invalidate();
                state = State.NORMAL;
            }
        }
    }

    static class State {
        static final int NORMAL = 0;
        static final int MOVING = 1;
        static final int FLING = 2;
        static final int SCROLLER = 3;
    }

    public interface OnValueChangedListener {
        /**
         * ruler view的值发生变化的回调
         *
         * @param value
         */
        void onValueChanged(int value);
    }
}
