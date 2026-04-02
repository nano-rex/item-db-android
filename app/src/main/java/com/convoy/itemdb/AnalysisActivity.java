package com.convoy.itemdb;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AnalysisActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private TextView tvDescription;
    private TextView tvBarTitle;
    private TextView tvLineTitle;
    private TextView tvOutput;
    private SimpleBarChartView barChart;
    private SimpleLineChartView lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferences.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);
        repository = new ItemDbRepository(this);
        tvDescription = findViewById(R.id.tvDescription);
        tvBarTitle = findViewById(R.id.tvBarTitle);
        tvLineTitle = findViewById(R.id.tvLineTitle);
        tvOutput = findViewById(R.id.tvOutput);
        barChart = findViewById(R.id.barChart);
        lineChart = findViewById(R.id.lineChart);
        runAnalysis();
    }

    private void runAnalysis() {
        long itemId = getIntent().getLongExtra("item_id", 0);
        if (itemId > 0) {
            runSingleItemAnalysis(itemId);
            return;
        }
        runGroupAnalysis();
    }

    private void runGroupAnalysis() {
        String query = getIntent().getStringExtra("query");
        String tags = getIntent().getStringExtra("tags");
        String location = getIntent().getStringExtra("location");
        String color = getIntent().getStringExtra("color");
        boolean matchAll = getIntent().getBooleanExtra("match_all_tags", false);
        tvDescription.setText("Charts below are based on the currently applied filters.");
        tvOutput.setText(repository.buildAnalysisReport(query == null ? "" : query, tags == null ? "" : tags, location == null ? "" : location, color == null ? "" : color, matchAll));
        barChart.setBars(repository.buildLatestPriceBars(query == null ? "" : query, tags == null ? "" : tags, location == null ? "" : location, color == null ? "" : color, matchAll));
        ChartLineSeries series = repository.buildPriceTimeline(query == null ? "" : query, tags == null ? "" : tags, location == null ? "" : location, color == null ? "" : color, matchAll);
        tvBarTitle.setText("Latest Price Comparison");
        tvLineTitle.setText(series.title == null || series.title.trim().isEmpty()
                ? "Price Over Time"
                : "Price Over Time: " + series.title);
        lineChart.setPoints(series.points);
    }

    private void runSingleItemAnalysis(long itemId) {
        ChartLineSeries series = repository.buildItemPriceTimeline(itemId);
        tvDescription.setText("Charts below are based on the selected item only.");
        tvOutput.setText(repository.buildItemAnalysisReport(itemId));
        barChart.setBars(repository.buildItemPriceBars(itemId));
        tvBarTitle.setText("Price By Entry");
        tvLineTitle.setText(series.title == null || series.title.trim().isEmpty()
                ? "Price Over Time"
                : "Price Over Time: " + series.title);
        lineChart.setPoints(series.points);
    }
}
