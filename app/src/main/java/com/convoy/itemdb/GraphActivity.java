package com.convoy.itemdb;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GraphActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private AutoCompleteTextView etGraphItemId;
    private TextView tvStatus;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        repository = new ItemDbRepository(this);
        etGraphItemId = findViewById(R.id.etGraphItemId);
        tvStatus = findViewById(R.id.tvStatus);
        tvOutput = findViewById(R.id.tvOutput);
        etGraphItemId.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.itemChoices()));
        findViewById(R.id.btnGraph).setOnClickListener(v -> renderGraph());
    }

    private void renderGraph() {
        String raw = etGraphItemId.getText().toString().trim();
        if (raw.isEmpty()) {
            tvStatus.setText("Graph requires item id");
            return;
        }
        int itemId = Integer.parseInt(raw.split(" - ")[0]);
        tvOutput.setText(repository.renderGraph(itemId));
        tvStatus.setText("Graph rendered");
    }
}
