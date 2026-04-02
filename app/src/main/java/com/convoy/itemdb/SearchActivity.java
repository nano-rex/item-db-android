package com.convoy.itemdb;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Collections;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private ItemListAdapter adapter;
    private EditText etSearch;
    private AutoCompleteTextView etTags;
    private AutoCompleteTextView etLocation;
    private Spinner spTagMode;
    private Spinner spColor;
    private Button btnRunAnalysis;
    private TextView tvEmpty;
    private static final String[] COLOR_LABELS = new String[]{
            "Any color", "Slate", "Blue", "Green", "Amber", "Rose", "Lavender", "Gray"
    };
    private static final String[] COLOR_VALUES = new String[]{
            "", "#E2E8F0", "#DBEAFE", "#DCFCE7", "#FEF3C7", "#FFE4E6", "#EDE9FE", "#E5E7EB"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferences.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        repository = new ItemDbRepository(this);
        etSearch = findViewById(R.id.etSearch);
        etTags = findViewById(R.id.etTags);
        etLocation = findViewById(R.id.etLocation);
        spTagMode = findViewById(R.id.spTagMode);
        spColor = findViewById(R.id.spColor);
        btnRunAnalysis = findViewById(R.id.btnRunAnalysis);
        tvEmpty = findViewById(R.id.tvEmpty);
        String initialQuery = getIntent().getStringExtra("query");
        if (initialQuery != null) etSearch.setText(initialQuery);
        String initialTags = getIntent().getStringExtra("tags");
        if (initialTags != null) etTags.setText(initialTags);
        String initialLocation = getIntent().getStringExtra("location");
        if (initialLocation != null) etLocation.setText(initialLocation);
        etTags.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.listDistinctTags()));
        etLocation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.listDistinctLocations()));
        spTagMode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Match any tag", "Match all tags"}));
        spColor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, COLOR_LABELS));
        spTagMode.setSelection(getIntent().getBooleanExtra("match_all_tags", false) ? 1 : 0);
        String initialColor = getIntent().getStringExtra("color");
        if (initialColor != null) {
            for (int i = 0; i < COLOR_VALUES.length; i++) {
                if (COLOR_VALUES[i].equalsIgnoreCase(initialColor)) {
                    spColor.setSelection(i);
                    break;
                }
            }
        }
        ListView listView = findViewById(R.id.lvResults);
        adapter = new ItemListAdapter(this, item -> {
            Intent intent = new Intent(this, ItemEditorActivity.class);
            intent.putExtra("item_id", item.id);
            startActivity(intent);
        }, item -> {
            Intent intent = new Intent(this, AnalysisActivity.class);
            intent.putExtra("item_id", item.id);
            startActivity(intent);
        }, item -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete item")
                    .setMessage("Delete \"" + item.title + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        repository.deleteItem(item.id);
                        refresh();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }, (item, checked) -> {}, Collections.emptySet());
        listView.setAdapter(adapter);
        findViewById(R.id.btnRunSearch).setOnClickListener(v -> refresh());
        btnRunAnalysis.setOnClickListener(v -> openAnalysis());
        updateAnalysisButtonState();
        refresh();
    }

    private void refresh() {
        List<ItemRecord> items = repository.listItemsFiltered(
                etSearch.getText().toString(),
                etTags.getText().toString(),
                etLocation.getText().toString(),
                COLOR_VALUES[spColor.getSelectedItemPosition()],
                spTagMode.getSelectedItemPosition() == 1
        );
        adapter.setItems(items);
        tvEmpty.setText(items.isEmpty() ? "No matching items." : "");
        updateAnalysisButtonState();
    }

    private void openAnalysis() {
        if (!canRunAnalysis()) return;
        Intent intent = new Intent(this, AnalysisActivity.class);
        intent.putExtra("query", etSearch.getText().toString());
        intent.putExtra("tags", etTags.getText().toString());
        intent.putExtra("location", etLocation.getText().toString());
        intent.putExtra("color", COLOR_VALUES[spColor.getSelectedItemPosition()]);
        intent.putExtra("match_all_tags", spTagMode.getSelectedItemPosition() == 1);
        startActivity(intent);
    }

    private void updateAnalysisButtonState() {
        btnRunAnalysis.setEnabled(canRunAnalysis());
    }

    private boolean canRunAnalysis() {
        return !etTags.getText().toString().trim().isEmpty()
                || !etLocation.getText().toString().trim().isEmpty();
    }
}
