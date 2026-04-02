package com.convoy.itemdb;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ExportActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private TextView tvStatus;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferences.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        repository = new ItemDbRepository(this);
        tvStatus = findViewById(R.id.tvStatus);
        tvOutput = findViewById(R.id.tvOutput);

        findViewById(R.id.btnExport).setOnClickListener(v -> exportJson());
        exportJson();
    }

    private void exportJson() {
        try {
            String out = repository.exportJsonToFile();
            tvStatus.setText("Export completed");
            tvOutput.setText(out);
        } catch (Exception e) {
            tvStatus.setText("Export failed: " + e.getMessage());
        }
    }
}
