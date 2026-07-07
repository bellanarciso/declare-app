package edu.illinois.cs.cs124.ay2026.project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.R;
import edu.illinois.cs.cs124.ay2026.project.model.Major;

/**
 * Dropdown adapter that groups majors under non-selectable college headers.
 *
 * The flat item list interleaves college name strings (headers) with major
 * name strings (selectable items). isEnabled() returns false for headers so
 * they cannot be chosen.
 */
public class MajorDropdownAdapter extends ArrayAdapter<String> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final Set<Integer> headerPositions = new HashSet<>();

    public MajorDropdownAdapter(@NonNull Context context, @NonNull List<Major> majors) {
        super(context, android.R.layout.simple_dropdown_item_1line);

        // Group majors by college, preserving insertion order.
        Map<String, List<String>> byCollege = new LinkedHashMap<>();
        for (Major major : majors) {
            String college = major.getCollege().isEmpty() ? "Other" : major.getCollege();
            byCollege.computeIfAbsent(college, k -> new ArrayList<>()).add(major.getDisplayName());
        }

        List<String> items = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : byCollege.entrySet()) {
            headerPositions.add(items.size());
            items.add(entry.getKey());
            items.addAll(entry.getValue());
        }

        addAll(items);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return headerPositions.contains(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            if (convertView == null || convertView.getTag() != null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_college_header, parent, false);
                convertView.setTag(null);
            }
            ((TextView) convertView).setText(getItem(position));
            return convertView;
        }

        if (convertView == null || !Integer.valueOf(VIEW_TYPE_ITEM).equals(convertView.getTag())) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            convertView.setTag(VIEW_TYPE_ITEM);
        }
        ((TextView) convertView).setText(getItem(position));
        return convertView;
    }
}