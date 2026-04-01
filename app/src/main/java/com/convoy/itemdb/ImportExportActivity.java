package com.convoy.itemdb;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ImportExportActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private EditText etImportJson;
    private TextView tvStatus;
    private TextView tvOutput;
    private String importPreviewJson = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_export);
        repository = new ItemDbRepository(this);
        etImportJson = findViewById(R.id.etImportJson);
        tvStatus = findViewById(R.id.tvStatus);
        tvOutput = findViewById(R.id.tvOutput);

        findViewById(R.id.btnExport).setOnClickListener(v -> exportJson());
        findViewById(R.id.btnPreviewImport).setOnClickListener(v -> previewImport());
        findViewById(R.id.btnImportMerge).setOnClickListener(v -> importJson(false));
        findViewById(R.id.btnImportReplace).setOnClickListener(v -> importJson(true));
        findViewById(R.id.btnImportDeny).setOnClickListener(v -> denyImport());
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

    private void previewImport() {
        importPreviewJson = etImportJson.getText().toString().trim();
        if (importPreviewJson.isEmpty()) {
            tvStatus.setText("Paste JSON into import box first");
            return;
        }
        try {
            tvStatus.setText(repository.previewImport(importPreviewJson));
        } catch (Exception e) {
            tvStatus.setText("Invalid JSON: " + e.getMessage());
        }
    }

    private void importJson(boolean replace) {
        if (importPreviewJson.isEmpty()) {
            tvStatus.setText("No import preview loaded");
            return;
        }
        try {
            repository.importJson(importPreviewJson, replace);
            tvStatus.setText(replace ? "Replace import applied" : "Merge import applied");
        } catch (Exception e) {
            tvStatus.setText("Import failed: " + e.getMessage());
        }
    }

    private void denyImport() {
        importPreviewJson = "";
        tvStatus.setText("Import canceled");
    }
}
