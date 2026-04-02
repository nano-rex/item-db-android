package com.convoy.itemdb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SimpleLineChartView extends View {
    private final List<ChartLinePoint> points = new ArrayList<>();
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SimpleLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleLineChartView(Context context) {
        super(context);
        init();
    }

    private void init() {
        axisPaint.setColor(Color.parseColor("#94A3B8"));
        axisPaint.setStrokeWidth(dp(1.5f));
        linePaint.setColor(Color.parseColor("#10B981"));
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStyle(Paint.Style.STROKE);
        pointPaint.setColor(Color.parseColor("#047857"));
        labelPaint.setColor(Color.parseColor("#0F172A"));
        labelPaint.setTextSize(sp(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setColor(Color.parseColor("#64748B"));
        emptyPaint.setTextSize(sp(12));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setPoints(List<ChartLinePoint> values) {
        points.clear();
        if (values != null) points.addAll(values);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        float left = dp(28);
        float right = width - dp(12);
        float top = dp(16);
        float bottom = height - dp(36);
        canvas.drawColor(Color.WHITE);

        if (points.size() < 2) {
            canvas.drawText("Need at least 2 dated price rows", width / 2f, height / 2f, emptyPaint);
            return;
        }

        double min = points.get(0).value;
        double max = points.get(0).value;
        for (ChartLinePoint point : points) {
            min = Math.min(min, point.value);
            max = Math.max(max, point.value);
        }
        if (Math.abs(max - min) < 0.0001) max = min + 1;

        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        float usableWidth = right - left;
        float usableHeight = bottom - top;
        Path path = new Path();

        for (int i = 0; i < points.size(); i++) {
            ChartLinePoint point = points.get(i);
            float x = left + (usableWidth * i / (points.size() - 1));
            float y = (float) (bottom - ((point.value - min) / (max - min) * usableHeight));
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);

        for (int i = 0; i < points.size(); i++) {
            ChartLinePoint point = points.get(i);
            float x = left + (usableWidth * i / (points.size() - 1));
            float y = (float) (bottom - ((point.value - min) / (max - min) * usableHeight));
            canvas.drawCircle(x, y, dp(3.5f), pointPaint);
            canvas.drawText(trimLabel(point.label, 10), x, height - dp(16), labelPaint);
        }
    }

    private String trimLabel(String text, int max) {
        if (text == null) return "";
        String value = text.trim();
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
