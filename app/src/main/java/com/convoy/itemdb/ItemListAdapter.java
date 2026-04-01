package com.convoy.itemdb;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ItemListAdapter extends BaseAdapter {
    public interface OpenListener { void open(ItemRecord item); }
    public interface DeleteListener { void delete(ItemRecord item); }

    private final LayoutInflater inflater;
    private final OpenListener openListener;
    private final DeleteListener deleteListener;
    private final List<ItemRecord> items = new ArrayList<>();

    public ItemListAdapter(Context context, OpenListener openListener, DeleteListener deleteListener) {
        this.inflater = LayoutInflater.from(context);
        this.openListener = openListener;
        this.deleteListener = deleteListener;
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
        ((TextView) view.findViewById(R.id.tvSummary)).setText(body);
        ((TextView) view.findViewById(R.id.tvMeta)).setText(item.rowCount + " rows  •  " + item.topicCount + " tags");
        View colorView = view.findViewById(R.id.viewColor);
        try {
            colorView.setBackgroundColor(Color.parseColor(item.colorHex == null ? "#E2E8F0" : item.colorHex));
        } catch (IllegalArgumentException ignored) {
            colorView.setBackgroundColor(Color.parseColor("#E2E8F0"));
        }
        Button open = view.findViewById(R.id.btnOpen);
        Button delete = view.findViewById(R.id.btnDelete);
        open.setOnClickListener(v -> openListener.open(item));
        delete.setOnClickListener(v -> deleteListener.delete(item));
        return view;
    }
}
