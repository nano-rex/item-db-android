package com.convoy.itemdb;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferences.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Switch swDarkMode = findViewById(R.id.swDarkMode);
        swDarkMode.setChecked(ThemePreferences.isDarkMode(this));
        swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemePreferences.setDarkMode(this, isChecked);
            recreate();
        });

        findViewById(R.id.btnImport).setOnClickListener(v -> {
            Intent intent = new Intent(this, ImportExportActivity.class);
            intent.putExtra("mode", "import");
            startActivity(intent);
        });
        findViewById(R.id.btnExport).setOnClickListener(v -> {
            Intent intent = new Intent(this, ImportExportActivity.class);
            intent.putExtra("mode", "export");
            startActivity(intent);
        });
    }
}
