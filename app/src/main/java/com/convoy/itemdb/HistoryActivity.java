package com.convoy.itemdb;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HistoryActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private AutoCompleteTextView etItemId;
    private EditText etPrice;
    private EditText etDate;
    private EditText etLocation;
    private TextView tvStatus;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        repository = new ItemDbRepository(this);
        etItemId = findViewById(R.id.etHistoryItemId);
        etPrice = findViewById(R.id.etHistoryPrice);
        etDate = findViewById(R.id.etHistoryDate);
        etLocation = findViewById(R.id.etHistoryLocation);
        tvStatus = findViewById(R.id.tvStatus);
        tvOutput = findViewById(R.id.tvOutput);
        refreshChoices();
        findViewById(R.id.btnAddHistory).setOnClickListener(v -> addHistory());
        findViewById(R.id.btnRefreshHistory).setOnClickListener(v -> tvOutput.setText(repository.listItems("", "price")));
        tvOutput.setText(repository.listItems("", "price"));
    }

    private void refreshChoices() {
        etItemId.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.itemChoices()));
    }

    private void addHistory() {
        String raw = etItemId.getText().toString().trim();
        if (raw.isEmpty()) {
            tvStatus.setText("Item id required");
            return;
        }
        int itemId = Integer.parseInt(raw.split(" - ")[0]);
        Double price = etPrice.getText().toString().trim().isEmpty() ? null : Double.parseDouble(etPrice.getText().toString().trim());
        repository.addHistory(itemId, price, etDate.getText().toString().trim(), etLocation.getText().toString().trim());
        etPrice.setText("");
        etDate.setText("");
        etLocation.setText("");
        tvStatus.setText("History entry saved");
        tvOutput.setText(repository.listItems("", "price"));
    }
}
