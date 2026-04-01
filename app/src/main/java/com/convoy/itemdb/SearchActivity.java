package com.convoy.itemdb;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private ItemListAdapter adapter;
    private EditText etSearch;
    private AutoCompleteTextView etTags;
    private AutoCompleteTextView etLocation;
    private Spinner spTagMode;
    private Spinner spColor;
    private TextView tvEmpty;
    private static final String[] COLOR_LABELS = new String[]{
            "Any color", "Slate", "Blue", "Green", "Amber", "Rose", "Lavender", "Gray"
    };
    private static final String[] COLOR_VALUES = new String[]{
            "", "#E2E8F0", "#DBEAFE", "#DCFCE7", "#FEF3C7", "#FFE4E6", "#EDE9FE", "#E5E7EB"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        repository = new ItemDbRepository(this);
        etSearch = findViewById(R.id.etSearch);
        etTags = findViewById(R.id.etTags);
        etLocation = findViewById(R.id.etLocation);
        spTagMode = findViewById(R.id.spTagMode);
        spColor = findViewById(R.id.spColor);
        tvEmpty = findViewById(R.id.tvEmpty);
        etTags.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.listDistinctTags()));
        etLocation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.listDistinctLocations()));
        spTagMode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Match any tag", "Match all tags"}));
        spColor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, COLOR_LABELS));
        ListView listView = findViewById(R.id.lvResults);
        adapter = new ItemListAdapter(this, item -> {
            Intent intent = new Intent(this, ItemEditorActivity.class);
            intent.putExtra("item_id", item.id);
            startActivity(intent);
        }, item -> {
            repository.deleteItem(item.id);
            refresh();
        });
        listView.setAdapter(adapter);
        findViewById(R.id.btnRunSearch).setOnClickListener(v -> refresh());
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
        tvEmpty.setText(items.isEmpty() ? "No matching notes." : "");
    }
}
