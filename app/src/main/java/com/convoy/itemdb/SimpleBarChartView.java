package com.convoy.itemdb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SimpleBarChartView extends View {
    private final List<ChartBarEntry> bars = new ArrayList<>();
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SimpleBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleBarChartView(Context context) {
        super(context);
        init();
    }

    private void init() {
        axisPaint.setColor(Color.parseColor("#94A3B8"));
        axisPaint.setStrokeWidth(dp(1.5f));
        barPaint.setColor(Color.parseColor("#3B82F6"));
        labelPaint.setColor(Color.parseColor("#0F172A"));
        labelPaint.setTextSize(sp(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(Color.parseColor("#334155"));
        valuePaint.setTextSize(sp(10));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setColor(Color.parseColor("#64748B"));
        emptyPaint.setTextSize(sp(12));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setBars(List<ChartBarEntry> values) {
        bars.clear();
        if (values != null) bars.addAll(values);
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

        if (bars.isEmpty()) {
            canvas.drawText("No bar chart data", width / 2f, height / 2f, emptyPaint);
            return;
        }

        double max = 0;
        for (ChartBarEntry entry : bars) max = Math.max(max, entry.value);
        if (max <= 0) max = 1;

        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        int count = bars.size();
        float slot = (right - left) / Math.max(1, count);
        float barWidth = Math.min(dp(36), slot * 0.65f);

        for (int i = 0; i < count; i++) {
            ChartBarEntry entry = bars.get(i);
            float cx = left + (slot * i) + slot / 2f;
            float usableHeight = bottom - top;
            float barHeight = (float) ((entry.value / max) * usableHeight);
            float barTop = bottom - barHeight;
            canvas.drawRect(cx - barWidth / 2f, barTop, cx + barWidth / 2f, bottom, barPaint);
            canvas.drawText(trimLabel(entry.label, 10), cx, height - dp(16), labelPaint);
            canvas.drawText(String.format(Locale.US, "%.2f", entry.value), cx, barTop - dp(4), valuePaint);
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
