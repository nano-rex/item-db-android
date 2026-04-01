package com.convoy.itemdb;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AnalysisActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private AutoCompleteTextView etTags;
    private AutoCompleteTextView etLocation;
    private Spinner spMode;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);
        repository = new ItemDbRepository(this);
        etTags = findViewById(R.id.etTopics);
        etLocation = findViewById(R.id.etLocation);
        spMode = findViewById(R.id.spMode);
        tvOutput = findViewById(R.id.tvOutput);

        spMode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Match any tag", "Match all tags"}));
        etTags.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.listDistinctTags()));
        etLocation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.listDistinctLocations()));

        findViewById(R.id.btnRunAnalysis).setOnClickListener(v -> runAnalysis());
        runAnalysis();
    }

    private void runAnalysis() {
        boolean matchAll = spMode.getSelectedItemPosition() == 1;
        tvOutput.setText(repository.buildAnalysisReport(etTags.getText().toString(), etLocation.getText().toString(), matchAll));
    }
}
