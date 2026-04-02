package com.convoy.itemdb;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemListAdapter extends BaseAdapter {
    public interface OpenListener { void open(ItemRecord item); }
    public interface AnalyzeListener { void analyze(ItemRecord item); }
    public interface DeleteListener { void delete(ItemRecord item); }
    public interface CheckedListener { void changed(ItemRecord item, boolean checked); }

    private final LayoutInflater inflater;
    private final OpenListener openListener;
    private final AnalyzeListener analyzeListener;
    private final DeleteListener deleteListener;
    private final CheckedListener checkedListener;
    private final Set<Long> checkedIds;
    private final Context context;
    private final List<ItemRecord> items = new ArrayList<>();

    public ItemListAdapter(Context context, OpenListener openListener, AnalyzeListener analyzeListener, DeleteListener deleteListener, CheckedListener checkedListener, Set<Long> checkedIds) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.openListener = openListener;
        this.analyzeListener = analyzeListener;
        this.deleteListener = deleteListener;
        this.checkedListener = checkedListener;
        this.checkedIds = checkedIds;
    }

    public void setItems(List<ItemRecord> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public Object getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return items.get(position).id; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : inflater.inflate(R.layout.item_item_note, parent, false);
        ItemRecord item = items.get(position);
        ((TextView) view.findViewById(R.id.tvTitle)).setText(item.title);
        String body = item.body == null || item.body.trim().isEmpty() ? "No body text yet" : item.body.trim();
        TextView tvSummary = view.findViewById(R.id.tvSummary);
        TextView tvMeta = view.findViewById(R.id.tvMeta);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        tvSummary.setText(body);
        tvMeta.setText(item.rowCount + " rows  •  " + item.topicCount + " tags");
        int baseColor = ColorThemeUtil.parseOrDefault(item.colorHex, "#E2E8F0");
        boolean darkMode = ThemePreferences.isDarkMode(context);
        int surfaceColor = darkMode
                ? ColorThemeUtil.blendTowardBlack(baseColor, 0.58f)
                : ColorThemeUtil.blendTowardWhite(baseColor, 0.45f);
        int accentColor = darkMode
                ? ColorThemeUtil.blendTowardBlack(baseColor, 0.72f)
                : ColorThemeUtil.darken(baseColor, 0.28f);
        int textColor = ColorThemeUtil.idealTextColor(surfaceColor);
        view.findViewById(R.id.itemRoot).setBackgroundColor(surfaceColor);
        view.findViewById(R.id.itemBody).setBackgroundColor(surfaceColor);
        tvTitle.setTextColor(textColor);
        tvSummary.setTextColor(textColor);
        tvMeta.setTextColor(textColor);
        View colorView = view.findViewById(R.id.viewColor);
        colorView.setBackgroundColor(accentColor);
        CheckBox checkBox = view.findViewById(R.id.cbSelect);
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(checkedIds.contains(item.id));
        checkBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> checkedListener.changed(item, isChecked));
        Button open = view.findViewById(R.id.btnOpen);
        Button analyze = view.findViewById(R.id.btnAnalyze);
        Button delete = view.findViewById(R.id.btnDelete);
        open.setOnClickListener(v -> openListener.open(item));
        analyze.setOnClickListener(v -> analyzeListener.analyze(item));
        delete.setOnClickListener(v -> deleteListener.delete(item));
        return view;
    }
}
