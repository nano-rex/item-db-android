package com.convoy.itemdb;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ItemEditorActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private long itemId;
    private EditText etTitle;
    private EditText etBody;
    private TextView tvStatus;
    private TextView tvMeta;
    private LinearLayout rowsContainer;
    private LinearLayout topicsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_editor);

        repository = new ItemDbRepository(this);
        itemId = getIntent().getLongExtra("item_id", 0);
        etTitle = findViewById(R.id.etTitle);
        etBody = findViewById(R.id.etBody);
        tvStatus = findViewById(R.id.tvStatus);
        tvMeta = findViewById(R.id.tvMeta);
        rowsContainer = findViewById(R.id.rowsContainer);
        topicsContainer = findViewById(R.id.topicsContainer);

        findViewById(R.id.btnSave).setOnClickListener(v -> saveItem());
        findViewById(R.id.btnAddRow).setOnClickListener(v -> showAddRowDialog());
        findViewById(R.id.btnAddTopic).setOnClickListener(v -> showAddSimpleDialog("Add topic", "Topic", value -> repository.addTopic(itemId, value)));
        findViewById(R.id.btnDeleteItem).setOnClickListener(v -> confirmDeleteItem());

        refresh();
    }

    private void refresh() {
        if (itemId == 0) {
            tvMeta.setText("New note");
            renderEmptySections();
            return;
        }
        ItemDetail detail = repository.getItemDetail(itemId);
        etTitle.setText(detail.item.title);
        etBody.setText(detail.item.body == null ? "" : detail.item.body);
        tvMeta.setText("Rows: " + detail.item.rowCount + "   Topics: " + detail.item.topicCount);
        renderRows(detail);
        renderNamedEntries(topicsContainer, detail.topics);
    }

    private void renderEmptySections() {
        rowsContainer.removeAllViews();
        topicsContainer.removeAllViews();
        addPlaceholder(rowsContainer, "Save the note first, then add structured rows.");
        addPlaceholder(topicsContainer, "Save the note first, then add topics.");
    }

    private void saveItem() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            tvStatus.setText("Title is required");
            return;
        }
        if (itemId == 0) {
            itemId = repository.createItem(title, etBody.getText().toString());
            tvStatus.setText("Note created");
        } else {
            repository.updateItem(itemId, title, etBody.getText().toString());
            tvStatus.setText("Note updated");
        }
        refresh();
    }

    private void confirmDeleteItem() {
        if (itemId == 0) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete note")
                .setMessage("Remove this note and all linked rows and topics?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteItem(itemId);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddRowDialog() {
        if (!ensureSaved()) return;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(12);
        layout.setPadding(padding, padding, padding, padding);

        EditText etPrice = field("Price", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText etLocation = field("Location", InputType.TYPE_CLASS_TEXT);
        EditText etDate = field("Date", InputType.TYPE_CLASS_DATETIME);
        EditText etRanking = field("Ranking", InputType.TYPE_CLASS_TEXT);
        EditText etProgress = field("Progress", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(etPrice);
        layout.addView(etLocation);
        layout.addView(etDate);
        layout.addView(etRanking);
        layout.addView(etProgress);

        new AlertDialog.Builder(this)
                .setTitle("Add structured row")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    repository.addRow(itemId,
                            etPrice.getText().toString(),
                            etLocation.getText().toString(),
                            etDate.getText().toString(),
                            etRanking.getText().toString(),
                            etProgress.getText().toString());
                    tvStatus.setText("Row added");
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddSimpleDialog(String title, String hint, ValueConsumer consumer) {
        if (!ensureSaved()) return;
        EditText input = field(hint, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        consumer.accept(value);
                        tvStatus.setText(title + " saved");
                        refresh();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean ensureSaved() {
        if (itemId != 0) return true;
        tvStatus.setText("Save the note first");
        return false;
    }

    private void renderRows(ItemDetail detail) {
        rowsContainer.removeAllViews();
        if (detail.rows.isEmpty()) {
            addPlaceholder(rowsContainer, "No structured rows yet.");
            return;
        }
        for (ItemRowEntry row : detail.rows) {
            rowsContainer.addView(buildRowView(
                    (row.hasPrice ? "Price: " + row.price : "Price: -") + "\n" +
                            "Location: " + dash(row.location) + "\n" +
                            "Date: " + dash(row.entryDate) + "\n" +
                            "Ranking: " + dash(row.ranking) + "\n" +
                            "Progress: " + dash(row.progressText),
                    v -> {
                        repository.deleteRow(row.id);
                        tvStatus.setText("Row removed");
                        refresh();
                    }));
        }
    }

    private void renderNamedEntries(LinearLayout container, java.util.List<NamedEntry> entries) {
        container.removeAllViews();
        if (entries.isEmpty()) {
            addPlaceholder(container, "No topics yet.");
            return;
        }
        for (NamedEntry entry : entries) {
            container.addView(buildRowView(entry.value, v -> {
                repository.deleteTopic(entry.id);
                tvStatus.setText("Topic removed");
                refresh();
            }));
        }
    }

    private View buildRowView(String text, View.OnClickListener deleteListener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(text);
        Button delete = new Button(this);
        delete.setText("Remove");
        delete.setOnClickListener(deleteListener);
        row.addView(tv);
        row.addView(delete);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 0, 0, dp(8));
        wrapper.addView(row);
        return wrapper;
    }

    private void addPlaceholder(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(0, 0, 0, dp(8));
        container.addView(tv);
    }

    private EditText field(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(inputType);
        return input;
    }

    private String dash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private interface ValueConsumer { void accept(String value); }
}
