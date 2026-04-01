package com.convoy.itemdb;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ItemsActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private EditText etName;
    private EditText etDescription;
    private EditText etTopics;
    private TextView tvStatus;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);
        repository = new ItemDbRepository(this);
        etName = findViewById(R.id.etItemName);
        etDescription = findViewById(R.id.etItemDesc);
        etTopics = findViewById(R.id.etItemTopics);
        tvStatus = findViewById(R.id.tvStatus);
        tvOutput = findViewById(R.id.tvOutput);

        findViewById(R.id.btnAddItem).setOnClickListener(v -> addItem());
        findViewById(R.id.btnRefreshItems).setOnClickListener(v -> refresh());
        refresh();
    }

    private void addItem() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            tvStatus.setText("Item name required");
            return;
        }
        repository.addItem(name, etDescription.getText().toString().trim(), etTopics.getText().toString().trim());
        etName.setText("");
        etDescription.setText("");
        etTopics.setText("");
        tvStatus.setText("Item saved");
        refresh();
    }

    private void refresh() {
        tvOutput.setText(repository.listItems("", ""));
    }
}
