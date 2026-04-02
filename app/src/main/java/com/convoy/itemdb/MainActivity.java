package com.convoy.itemdb;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private ItemListAdapter adapter;
    private EditText etQuickSearch;
    private TextView tvEmpty;
    private LinearLayout selectionActions;
    private TextView tvSelectionCount;
    private boolean sortNewestFirst = true;
    private boolean sortAscending = true;
    private boolean matchAllTags = false;
    private String filterLocation = "";
    private String filterColorHex = "";
    private final Set<String> selectedTags = new HashSet<>();
    private final Set<Long> selectedItemIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferences.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new ItemDbRepository(this);
        etQuickSearch = findViewById(R.id.etQuickSearch);
        tvEmpty = findViewById(R.id.tvEmpty);
        selectionActions = findViewById(R.id.selectionActions);
        tvSelectionCount = findViewById(R.id.tvSelectionCount);
        ListView listView = findViewById(R.id.lvItems);
        adapter = new ItemListAdapter(this, item -> openEditor(item.id), item -> openItemAnalysis(item.id), this::confirmDeleteSingleItem, (item, checked) -> {
            if (checked) selectedItemIds.add(item.id); else selectedItemIds.remove(item.id);
            updateSelectionUi();
        }, selectedItemIds);
        listView.setAdapter(adapter);

        findViewById(R.id.btnNewItem).setOnClickListener(v -> openEditor(0));
        findViewById(R.id.btnAnalysis).setOnClickListener(v -> openAnalysis());
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());
        findViewById(R.id.btnBulkAddTag).setOnClickListener(v -> showBulkAddTagDialog());
        findViewById(R.id.btnBulkRemoveTag).setOnClickListener(v -> showBulkRemoveTagDialog());
        findViewById(R.id.btnBulkDelete).setOnClickListener(v -> confirmBulkDelete());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<ItemRecord> items = new ArrayList<>(repository.listItemsFiltered(
                etQuickSearch.getText().toString(),
                String.join(", ", selectedTags),
                filterLocation,
                filterColorHex,
                matchAllTags
        ));
        if (!selectedTags.isEmpty()) {
            ArrayList<ItemRecord> filtered = new ArrayList<>();
            for (ItemRecord item : items) {
                if (matchesSelectedTags(item.id)) filtered.add(item);
            }
            items = filtered;
        }
        items.sort(buildComparator());
        adapter.setItems(items);
        tvEmpty.setText(items.isEmpty() ? "No items yet. Create one from the top bar." : "");
        updateSelectionUi();
        updateAnalysisVisibility();
    }

    private void openEditor(long itemId) {
        Intent intent = new Intent(this, ItemEditorActivity.class);
        intent.putExtra("item_id", itemId);
        startActivity(intent);
    }

    private void openAnalysis() {
        Intent intent = new Intent(this, AnalysisActivity.class);
        intent.putExtra("query", etQuickSearch.getText().toString());
        intent.putExtra("tags", String.join(", ", selectedTags));
        intent.putExtra("location", filterLocation);
        intent.putExtra("color", filterColorHex);
        intent.putExtra("match_all_tags", matchAllTags);
        startActivity(intent);
    }

    private void openItemAnalysis(long itemId) {
        Intent intent = new Intent(this, AnalysisActivity.class);
        intent.putExtra("item_id", itemId);
        startActivity(intent);
    }

    private void showFilterDialog() {
        List<String> tags = repository.listDistinctTags();
        boolean[] checked = new boolean[tags.size()];
        for (int i = 0; i < tags.size(); i++) checked[i] = selectedTags.contains(tags.get(i));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root);

        TextView newestHeader = sectionLabel("Newest/latest first");
        CheckBox newestCheck = new CheckBox(this);
        newestCheck.setText("Prioritize latest updated items first");
        newestCheck.setChecked(sortNewestFirst);
        root.addView(newestHeader);
        root.addView(newestCheck);

        TextView orderHeader = sectionLabel("Order");
        RadioGroup orderGroup = new RadioGroup(this);
        orderGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton asc = new RadioButton(this);
        asc.setText("Ascending");
        RadioButton desc = new RadioButton(this);
        desc.setText("Descending");
        orderGroup.addView(asc);
        orderGroup.addView(desc);
        if (sortAscending) asc.setChecked(true); else desc.setChecked(true);
        root.addView(orderHeader);
        root.addView(orderGroup);

        TextView matchHeader = sectionLabel("Tag match mode");
        RadioGroup matchGroup = new RadioGroup(this);
        matchGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton anyTags = new RadioButton(this);
        anyTags.setText("Match any selected tag");
        RadioButton allTags = new RadioButton(this);
        allTags.setText("Match all selected tags");
        matchGroup.addView(anyTags);
        matchGroup.addView(allTags);
        if (matchAllTags) allTags.setChecked(true); else anyTags.setChecked(true);
        root.addView(matchHeader);
        root.addView(matchGroup);

        TextView tagsHeader = sectionLabel("Tags");
        root.addView(tagsHeader);
        for (int i = 0; i < tags.size(); i++) {
            CheckBox tagBox = new CheckBox(this);
            tagBox.setText(tags.get(i));
            tagBox.setChecked(checked[i]);
            final int index = i;
            tagBox.setOnCheckedChangeListener((buttonView, isChecked) -> checked[index] = isChecked);
            root.addView(tagBox);
        }

        TextView locationHeader = sectionLabel("Location");
        AutoCompleteTextView locationInput = new AutoCompleteTextView(this);
        locationInput.setHint("Optional location filter");
        locationInput.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.listDistinctLocations()));
        locationInput.setText(filterLocation);
        root.addView(locationHeader);
        root.addView(locationInput);

        TextView colorHeader = sectionLabel("Color");
        Spinner colorSpinner = new Spinner(this);
        String[] colorLabels = new String[]{"Any color", "Slate", "Blue", "Green", "Amber", "Rose", "Lavender", "Gray"};
        String[] colorValues = new String[]{"", "#E2E8F0", "#DBEAFE", "#DCFCE7", "#FEF3C7", "#FFE4E6", "#EDE9FE", "#E5E7EB"};
        colorSpinner.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, colorLabels));
        int selectedColorIndex = 0;
        for (int i = 0; i < colorValues.length; i++) {
            if (colorValues[i].equalsIgnoreCase(filterColorHex)) {
                selectedColorIndex = i;
                break;
            }
        }
        colorSpinner.setSelection(selectedColorIndex);
        root.addView(colorHeader);
        root.addView(colorSpinner);

        new AlertDialog.Builder(this)
                .setTitle("Filter Items")
                .setView(scrollView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    sortNewestFirst = newestCheck.isChecked();
                    sortAscending = asc.isChecked();
                    matchAllTags = allTags.isChecked();
                    selectedTags.clear();
                    for (int i = 0; i < tags.size(); i++) {
                        if (checked[i]) selectedTags.add(tags.get(i));
                    }
                    filterLocation = locationInput.getText().toString().trim();
                    filterColorHex = colorValues[colorSpinner.getSelectedItemPosition()];
                    refresh();
                })
                .setNeutralButton("Clear", (dialog, which) -> {
                    sortNewestFirst = true;
                    sortAscending = true;
                    matchAllTags = false;
                    selectedTags.clear();
                    filterLocation = "";
                    filterColorHex = "";
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBulkAddTagDialog() {
        if (selectedItemIds.isEmpty()) return;
        AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setHint("Tag");
        input.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, repository.listDistinctTags()));
        new AlertDialog.Builder(this)
                .setTitle("Add tag to selected items")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String tag = input.getText().toString().trim();
                    if (tag.isEmpty()) return;
                    for (Long itemId : selectedItemIds) repository.addTag(itemId, tag);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBulkRemoveTagDialog() {
        if (selectedItemIds.isEmpty()) return;
        List<String> tags = collectSelectedItemTags();
        if (tags.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Remove tag")
                    .setMessage("The selected items have no tags to remove.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String[] items = tags.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Remove tag from selected items")
                .setItems(items, (dialog, which) -> {
                    String tag = items[which];
                    for (Long itemId : selectedItemIds) repository.deleteTagByValue(itemId, tag);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmBulkDelete() {
        if (selectedItemIds.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete selected items")
                .setMessage("Delete " + selectedItemIds.size() + " selected items?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    for (Long itemId : new ArrayList<>(selectedItemIds)) repository.deleteItem(itemId);
                    selectedItemIds.clear();
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteSingleItem(ItemRecord item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete item")
                .setMessage("Delete \"" + item.title + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteItem(item.id);
                    selectedItemIds.remove(item.id);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean matchesSelectedTags(long itemId) {
        ItemDetail detail = repository.getItemDetail(itemId);
        for (NamedEntry entry : detail.topics) {
            if (entry.value != null && selectedTags.contains(entry.value)) return true;
        }
        return false;
    }

    private Comparator<ItemRecord> buildComparator() {
        Comparator<ItemRecord> titleComparator = (a, b) -> compareText(a.title, b.title, sortAscending);
        Comparator<ItemRecord> updatedComparator = (a, b) -> compareText(b.updatedAt, a.updatedAt, true);
        if (sortNewestFirst) {
            return updatedComparator.thenComparing(titleComparator);
        }
        return titleComparator.thenComparing(updatedComparator);
    }

    private int compareText(String a, String b, boolean ascending) {
        String left = a == null ? "" : a.toLowerCase(Locale.US);
        String right = b == null ? "" : b.toLowerCase(Locale.US);
        return ascending ? left.compareTo(right) : right.compareTo(left);
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setPadding(0, dp(12), 0, dp(6));
        return tv;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private void updateSelectionUi() {
        selectionActions.setVisibility(selectedItemIds.isEmpty() ? View.GONE : View.VISIBLE);
        tvSelectionCount.setText(selectedItemIds.size() + " selected");
    }

    private void updateAnalysisVisibility() {
        findViewById(R.id.btnAnalysis).setVisibility(hasAnalysisFilters() ? View.VISIBLE : View.GONE);
    }

    private boolean hasAnalysisFilters() {
        return !selectedTags.isEmpty() || !filterLocation.isEmpty() || !filterColorHex.isEmpty();
    }

    private List<String> collectSelectedItemTags() {
        Set<String> tags = new HashSet<>();
        for (Long itemId : selectedItemIds) {
            for (NamedEntry entry : repository.getItemDetail(itemId).topics) {
                if (entry.value != null && !entry.value.trim().isEmpty()) tags.add(entry.value.trim());
            }
        }
        ArrayList<String> result = new ArrayList<>(tags);
        result.sort(String::compareToIgnoreCase);
        return result;
    }
}
