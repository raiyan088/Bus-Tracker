package com.rr.bubtbustracker.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent; // এই লাইব্রেরিটি ইভেন্ট হ্যান্ডেলের জন্য প্রয়োজন
import android.view.View;

import androidx.annotation.NonNull;

import com.rr.bubtbustracker.model.RoutePoint;

import java.util.ArrayList;
import java.util.List;


public class RouteMapView extends View {

    private List<RoutePoint> routePoints = new ArrayList<>();

    private Paint solidRoadPaint;
    private Paint dashedLinePaint;
    private Paint pointPaint;
    private Paint pointBorderPaint;
    private Paint textPaint;

    private final float pointRadius = 25f;
    private final float pointBorderThickness = 8f;
    private final float roadThickness = 20f;
    private float dashedLineThickness = 4f;
    private final float EXTRA_TEXT_PADDING = -35f;

    private int pressedPointIndex = -1;
    private Paint clickEffectPaint;
    private final float effectRadius = pointRadius * 1.2f;
    private final float routeTextSize = 36f;

    private final float topTextMargin = 30f;
    private final float bottomTextMargin = 50f;


    public interface OnPointClickListener {
        void onPointClicked(String pointId);
    }
    private OnPointClickListener clickListener;

    private float[] pointXPositions;

    public RouteMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        solidRoadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        solidRoadPaint.setColor(Color.parseColor("#1273C2"));
        solidRoadPaint.setStyle(Paint.Style.STROKE);
        solidRoadPaint.setStrokeWidth(roadThickness);
        solidRoadPaint.setStrokeCap(Paint.Cap.ROUND);

        dashedLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dashedLinePaint.setColor(Color.WHITE);
        dashedLinePaint.setStyle(Paint.Style.STROKE);
        dashedLinePaint.setStrokeWidth(dashedLineThickness);

        float dashInterval = 15f;
        float spaceInterval = 10f;
        dashedLinePaint.setPathEffect(new DashPathEffect(new float[]{dashInterval, spaceInterval}, 0));

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.parseColor("#FDCC29"));
        pointPaint.setStyle(Paint.Style.FILL);

        pointBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointBorderPaint.setColor(Color.WHITE);
        pointBorderPaint.setStyle(Paint.Style.STROKE);
        pointBorderPaint.setStrokeWidth(pointBorderThickness);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#10356C"));
        textPaint.setTextSize(routeTextSize);

        clickEffectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clickEffectPaint.setColor(Color.WHITE);
        clickEffectPaint.setStyle(Paint.Style.FILL);
        clickEffectPaint.setAlpha(150);
    }

    public void setRoutePoints(List<RoutePoint> points) {
        this.routePoints = points;
        this.pointXPositions = new float[points.size()];
        invalidate();
    }

    public void setOnPointClickListener(OnPointClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float textAscent = textPaint.ascent();
        float textDescent = textPaint.descent();

        float heightFromTop = (-textAscent) + topTextMargin + pointRadius;

        float heightFromBottom = pointRadius + bottomTextMargin + textDescent;

        int desiredHeight = (int) (heightFromTop + heightFromBottom + 10f);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int finalHeight;

        if (heightMode == MeasureSpec.EXACTLY) {
            finalHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            finalHeight = Math.min(desiredHeight, heightSize);
        } else {
            finalHeight = desiredHeight;
        }

        setMeasuredDimension(width, finalHeight);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (routePoints.isEmpty()) {
            return;
        }

        int width = getWidth();

        int totalPoints = routePoints.size();

        float textAscent = textPaint.ascent();
        float centerY = (-textAscent) + topTextMargin + pointRadius;

        if (totalPoints < 2) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            float pointX = width / 2f;
            pointXPositions[0] = pointX;

            canvas.drawCircle(pointX, centerY, pointRadius, pointBorderPaint);
            canvas.drawCircle(pointX, centerY, pointRadius - (pointBorderThickness / 2f), pointPaint);

            float textPositionY = centerY + pointRadius + bottomTextMargin;
            canvas.drawText(routePoints.get(0).getName(), pointX, textPositionY, textPaint);
            return;
        }

        float padding = 50f;
        float roadLength = width - (2 * padding);
        float spacing = roadLength / (totalPoints - 1);

        float endX = width - padding;

        canvas.drawLine(padding, centerY, endX, centerY, solidRoadPaint);
        canvas.drawLine(padding, centerY, endX, centerY, dashedLinePaint);


        for (int i = 0; i < totalPoints; i++) {
            float pointX = padding + (i * spacing);
            RoutePoint data = routePoints.get(i);

            pointXPositions[i] = pointX;

            canvas.drawCircle(pointX, centerY, pointRadius, pointBorderPaint);
            canvas.drawCircle(pointX, centerY, pointRadius - (pointBorderThickness / 2f), pointPaint);

            if (i == pressedPointIndex) {
                canvas.drawCircle(pointX, centerY, effectRadius, clickEffectPaint);
            }

            float textPositionX;
            float textPositionY;

            if (i % 2 == 0) {
                float distanceAbovePoint = pointRadius + topTextMargin;
                textPositionY = centerY - distanceAbovePoint;

            } else {
                textPositionY = centerY + pointRadius + bottomTextMargin;
            }

            if (i == 0) {
                textPaint.setTextAlign(Paint.Align.LEFT);
                textPositionX = padding + EXTRA_TEXT_PADDING;
            } else if (i == totalPoints - 1) {
                textPaint.setTextAlign(Paint.Align.RIGHT);
                textPositionX = width - padding - EXTRA_TEXT_PADDING;
            } else {
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPositionX = pointX;
            }

            canvas.drawText(data.getName(), textPositionX, textPositionY, textPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float touchX = event.getX();
        float touchY = event.getY();

        float textAscent = textPaint.ascent();
        float centerY = (-textAscent) + topTextMargin + pointRadius;

        float hitRadius = pointRadius * 1.5f;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            for (int i = 0; i < routePoints.size(); i++) {
                if (pointXPositions == null || pointXPositions.length <= i) continue;

                float pointX = pointXPositions[i];
                double distance = Math.sqrt(Math.pow(pointX - touchX, 2) + Math.pow(centerY - touchY, 2));

                if (distance <= hitRadius) {
                    pressedPointIndex = i;
                    invalidate();
                    return true;
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (pressedPointIndex != -1) {
                if (clickListener != null) {
                    clickListener.onPointClicked(routePoints.get(pressedPointIndex).getId());
                }

                pressedPointIndex = -1;
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (pressedPointIndex != -1) {
                pressedPointIndex = -1;
                invalidate();
            }
        }

        return super.onTouchEvent(event);
    }
}