package com.convoy.itemdb;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SearchActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private EditText etSearch;
    private EditText etSort;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        repository = new ItemDbRepository(this);
        etSearch = findViewById(R.id.etSearch);
        etSort = findViewById(R.id.etSort);
        tvOutput = findViewById(R.id.tvOutput);
        findViewById(R.id.btnRunSearch).setOnClickListener(v -> refresh());
        refresh();
    }

    private void refresh() {
        tvOutput.setText(repository.listItems(etSearch.getText().toString().trim(), etSort.getText().toString().trim()));
    }
}
