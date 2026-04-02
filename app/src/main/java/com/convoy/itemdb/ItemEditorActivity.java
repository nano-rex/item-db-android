package com.convoy.itemdb;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class ItemEditorActivity extends AppCompatActivity {
    private static final String[] COLOR_LABELS = new String[]{
            "Slate", "Blue", "Green", "Amber", "Rose", "Lavender", "Gray"
    };
    private static final String[] COLOR_VALUES = new String[]{
            "#E2E8F0", "#DBEAFE", "#DCFCE7", "#FEF3C7", "#FFE4E6", "#EDE9FE", "#E5E7EB"
    };
    private static final String WORLD_LOCATIONS_ASSET = "world_locations.txt";

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
        ThemePreferences.apply(this);
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

        EditText etPrice = field("Enter amount", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        AutoCompleteTextView etLocation = new AutoCompleteTextView(this);
        etLocation.setHint("Choose or type a city, state, or province");
        etLocation.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etLocation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, worldLocationSuggestions()));
        EditText etDate = field("YYYY-MM-DD", InputType.TYPE_CLASS_DATETIME);
        EditText etRanking = field("0-100", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText etProgress = field("0-100", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> showDatePicker(etDate));
        etDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.clearFocus();
                showDatePicker(etDate);
            }
        });
        attachPriceFormatting(etPrice);

        addLabeledField(layout, "Price", etPrice);
        addLabeledField(layout, "Location", etLocation);
        addLabeledField(layout, "Date", etDate);
        addLabeledField(layout, "Ranking (%)", etRanking);
        addLabeledField(layout, "Progress (%)", etProgress);
        if (existingRow != null) {
            etPrice.setText(existingRow.hasPrice ? formatPrice(existingRow.price) : "");
            etLocation.setText(existingRow.location == null ? "" : existingRow.location);
            etDate.setText(existingRow.entryDate == null ? "" : existingRow.entryDate);
            etRanking.setText(stripPercent(existingRow.ranking));
            etProgress.setText(stripPercent(existingRow.progressText));
        }

        new AlertDialog.Builder(this)
                .setTitle(existingRow == null ? "Add structured row" : "Edit structured row")
                .setView(layout)
                .setPositiveButton(existingRow == null ? "Add" : "Save", (dialog, which) -> {
                    String priceValue = normalizePrice(etPrice.getText().toString());
                    String rankingValue = normalizePercent(etRanking.getText().toString());
                    String progressValue = normalizePercent(etProgress.getText().toString());
                    if (existingRow == null) {
                        repository.addRow(itemId,
                                priceValue,
                                etLocation.getText().toString(),
                                etDate.getText().toString(),
                                rankingValue,
                                progressValue);
                        tvStatus.setText("Row added");
                    } else {
                        repository.updateRow(existingRow.id,
                                priceValue,
                                etLocation.getText().toString(),
                                etDate.getText().toString(),
                                rankingValue,
                                progressValue);
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
                    (row.hasPrice ? "Price: " + formatPrice(row.price) : "Price: -") + "\n" +
                            "Location: " + dash(row.location) + "\n" +
                            "Date: " + dash(row.entryDate) + "\n" +
                            "Ranking: " + formatPercentForDisplay(row.ranking) + "\n" +
                            "Progress: " + formatPercentForDisplay(row.progressText),
                    v -> showRowDialog(row),
                    v -> {
                        new AlertDialog.Builder(this)
                                .setTitle("Remove row")
                                .setMessage("Remove this structured row?")
                                .setPositiveButton("Remove", (dialog, which) -> {
                                    repository.deleteRow(row.id);
                                    tvStatus.setText("Row removed");
                                    refresh();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
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
                new AlertDialog.Builder(this)
                        .setTitle("Remove tag")
                        .setMessage("Remove this tag?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            repository.deleteTag(entry.id);
                            tvStatus.setText("Tag removed");
                            refresh();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
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

    private void addLabeledField(LinearLayout parent, String label, View field) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setPadding(0, dp(8), 0, dp(4));
        parent.addView(tv);
        parent.addView(field);
    }

    private void showDatePicker(EditText target) {
        LocalDate date = parseDate(target.getText().toString());
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                target.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
        dialog.show();
    }

    private LocalDate parseDate(String raw) {
        try {
            return raw == null || raw.trim().isEmpty() ? LocalDate.now() : LocalDate.parse(raw.trim());
        } catch (Exception ignored) {
            return LocalDate.now();
        }
    }

    private List<String> worldLocationSuggestions() {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(WORLD_LOCATIONS_ASSET)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String value = line.trim();
                if (!value.isEmpty()) values.add(value);
            }
        } catch (IOException ignored) {
        }
        values.addAll(repository.listDistinctLocations());
        return new ArrayList<>(values);
    }

    private void attachPriceFormatting(EditText editText) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String normalized = normalizePrice(editText.getText().toString());
                editText.setText(normalized);
            }
        });
    }

    private String normalizePrice(String raw) {
        String cleaned = raw == null ? "" : raw.replace(",", "").trim();
        if (cleaned.isEmpty()) return "";
        try {
            double value = Double.parseDouble(cleaned);
            return formatPrice(value);
        } catch (Exception ignored) {
            return raw == null ? "" : raw.trim();
        }
    }

    private String formatPrice(double value) {
        return new DecimalFormat("#,##0.00").format(value);
    }

    private String normalizePercent(String raw) {
        String cleaned = stripPercent(raw);
        if (cleaned.isEmpty()) return "";
        try {
            double value = Double.parseDouble(cleaned);
            if (value == Math.rint(value)) {
                return String.format(Locale.US, "%.0f%%", value);
            }
            return String.format(Locale.US, "%.2f%%", value);
        } catch (Exception ignored) {
            return cleaned + "%";
        }
    }

    private String stripPercent(String raw) {
        return raw == null ? "" : raw.replace("%", "").trim();
    }

    private String formatPercentForDisplay(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return "-";
        return value.endsWith("%") ? value : value + "%";
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
        boolean darkMode = ThemePreferences.isDarkMode(this);
        int rootColor = darkMode
                ? ColorThemeUtil.blendTowardBlack(baseColor, 0.72f)
                : ColorThemeUtil.blendTowardWhite(baseColor, 0.75f);
        int surfaceColor = darkMode
                ? ColorThemeUtil.blendTowardBlack(baseColor, 0.52f)
                : ColorThemeUtil.blendTowardWhite(baseColor, 0.4f);
        editorRoot.setBackgroundColor(rootColor);
        editorSurface.setBackgroundColor(surfaceColor);
        if (getSupportActionBar() != null) {
            int barColor = darkMode
                    ? ColorThemeUtil.blendTowardBlack(baseColor, 0.78f)
                    : ColorThemeUtil.darken(baseColor, 0.18f);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(barColor));
        }
    }
}
