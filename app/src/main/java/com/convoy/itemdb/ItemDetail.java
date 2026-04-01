package com.convoy.itemdb;

import java.util.ArrayList;
import java.util.List;

public class ItemDetail {
    public final ItemRecord item = new ItemRecord();
    public final List<ItemRowEntry> rows = new ArrayList<>();
    public final List<NamedEntry> topics = new ArrayList<>();
    public final List<NamedEntry> progressEntries = new ArrayList<>();
}
