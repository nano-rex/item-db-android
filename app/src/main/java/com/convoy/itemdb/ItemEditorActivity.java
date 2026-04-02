package com.convoy.itemdb;

import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ItemEditorActivity extends AppCompatActivity {
    private static final String[] COLOR_LABELS = new String[]{
            "Slate", "Blue", "Green", "Amber", "Rose", "Lavender", "Gray"
    };
    private static final String[] COLOR_VALUES = new String[]{
            "#E2E8F0", "#DBEAFE", "#DCFCE7", "#FEF3C7", "#FFE4E6", "#EDE9FE", "#E5E7EB"
    };

    private ItemDbRepository repository;
    private long itemId;
    private EditText etTitle;
    private EditText etBody;
    private Spinner spColor;
    private View editorRoot;
    private View editorSurface;
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
        editorRoot = findViewById(R.id.editorRoot);
        editorSurface = findViewById(R.id.editorSurface);
        etTitle = findViewById(R.id.etTitle);
        etBody = findViewById(R.id.etBody);
        spColor = findViewById(R.id.spColor);
        tvStatus = findViewById(R.id.tvStatus);
        tvMeta = findViewById(R.id.tvMeta);
        rowsContainer = findViewById(R.id.rowsContainer);
        topicsContainer = findViewById(R.id.topicsContainer);
        spColor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, COLOR_LABELS));
        spColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyEditorTheme(selectedColor());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveItem());
        findViewById(R.id.btnAddRow).setOnClickListener(v -> showAddRowDialog());
        findViewById(R.id.btnAddTag).setOnClickListener(v -> showAddTagDialog());
        findViewById(R.id.btnDeleteItem).setOnClickListener(v -> confirmDeleteItem());

        refresh();
    }

    private void refresh() {
        if (itemId == 0) {
            tvMeta.setText("New note");
            applyEditorTheme(selectedColor());
            renderEmptySections();
            return;
        }
        ItemDetail detail = repository.getItemDetail(itemId);
        etTitle.setText(detail.item.title);
        etBody.setText(detail.item.body == null ? "" : detail.item.body);
        spColor.setSelection(colorIndex(detail.item.colorHex));
        applyEditorTheme(detail.item.colorHex);
        tvMeta.setText("Rows: " + detail.item.rowCount + "   Tags: " + detail.item.topicCount);
        renderRows(detail);
        renderNamedEntries(topicsContainer, detail.topics);
    }

    private void renderEmptySections() {
        rowsContainer.removeAllViews();
        topicsContainer.removeAllViews();
        addPlaceholder(rowsContainer, "Save the note first, then add structured rows.");
        addPlaceholder(topicsContainer, "Save the note first, then add tags.");
    }

    private void saveItem() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            tvStatus.setText("Title is required");
            return;
        }
        if (itemId == 0) {
            itemId = repository.createItem(title, etBody.getText().toString(), selectedColor());
            tvStatus.setText("Note created");
        } else {
            repository.updateItem(itemId, title, etBody.getText().toString(), selectedColor());
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
                .setMessage("Remove this note and all linked rows and tags?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteItem(itemId);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddRowDialog() {
        showRowDialog(null);
    }

    private void showRowDialog(ItemRowEntry existingRow) {
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
        if (existingRow != null) {
            etPrice.setText(existingRow.hasPrice ? String.valueOf(existingRow.price) : "");
            etLocation.setText(existingRow.location == null ? "" : existingRow.location);
            etDate.setText(existingRow.entryDate == null ? "" : existingRow.entryDate);
            etRanking.setText(existingRow.ranking == null ? "" : existingRow.ranking);
            etProgress.setText(existingRow.progressText == null ? "" : existingRow.progressText);
        }

        new AlertDialog.Builder(this)
                .setTitle(existingRow == null ? "Add structured row" : "Edit structured row")
                .setView(layout)
                .setPositiveButton(existingRow == null ? "Add" : "Save", (dialog, which) -> {
                    if (existingRow == null) {
                        repository.addRow(itemId,
                                etPrice.getText().toString(),
                                etLocation.getText().toString(),
                                etDate.getText().toString(),
                                etRanking.getText().toString(),
                                etProgress.getText().toString());
                        tvStatus.setText("Row added");
                    } else {
                        repository.updateRow(existingRow.id,
                                etPrice.getText().toString(),
                                etLocation.getText().toString(),
                                etDate.getText().toString(),
                                etRanking.getText().toString(),
                                etProgress.getText().toString());
                        tvStatus.setText("Row updated");
                    }
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddTagDialog() {
        if (!ensureSaved()) return;
        AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setHint("Tag");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.listDistinctTags()));
        new AlertDialog.Builder(this)
                .setTitle("Add tag")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        repository.addTag(itemId, value);
                        tvStatus.setText("Tag saved");
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
                    v -> showRowDialog(row),
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
            addPlaceholder(container, "No tags yet.");
            return;
        }
        for (NamedEntry entry : entries) {
            container.addView(buildRowView(entry.value, null, v -> {
                repository.deleteTag(entry.id);
                tvStatus.setText("Tag removed");
                refresh();
            }));
        }
    }

    private View buildRowView(String text, View.OnClickListener editListener, View.OnClickListener deleteListener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(text);
        if (editListener != null) {
            Button edit = new Button(this);
            edit.setText("Edit");
            edit.setOnClickListener(editListener);
            row.addView(tv);
            row.addView(edit);
        } else {
            row.addView(tv);
        }
        Button delete = new Button(this);
        delete.setText("Remove");
        delete.setOnClickListener(deleteListener);
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

    private String selectedColor() {
        return COLOR_VALUES[Math.max(0, spColor.getSelectedItemPosition())];
    }

    private int colorIndex(String colorHex) {
        if (colorHex == null) return 0;
        for (int i = 0; i < COLOR_VALUES.length; i++) {
            if (COLOR_VALUES[i].equalsIgnoreCase(colorHex)) return i;
        }
        return 0;
    }

    private void applyEditorTheme(String colorHex) {
        int baseColor = ColorThemeUtil.parseOrDefault(colorHex, "#E2E8F0");
        int rootColor = ColorThemeUtil.blendTowardWhite(baseColor, 0.75f);
        int surfaceColor = ColorThemeUtil.blendTowardWhite(baseColor, 0.4f);
        editorRoot.setBackgroundColor(rootColor);
        editorSurface.setBackgroundColor(surfaceColor);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ColorThemeUtil.darken(baseColor, 0.18f)));
        }
    }
}
