package edu.illinois.cs.cs124.ay2026.project.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.illinois.cs.cs124.ay2026.project.R;
import edu.illinois.cs.cs124.ay2026.project.data.SavedPlanEntity;

public class SavedPlanAdapter extends RecyclerView.Adapter<SavedPlanAdapter.ViewHolder> {

    public interface OnPlanClickListener {
        void onPlanClick(SavedPlanEntity plan);
        void onPlanDelete(SavedPlanEntity plan);
    }

    private final List<SavedPlanEntity> plans;
    private final OnPlanClickListener listener;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM d, yyyy", Locale.US);

    public SavedPlanAdapter(List<SavedPlanEntity> plans, OnPlanClickListener listener) {
        this.plans = plans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_plan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedPlanEntity plan = plans.get(position);

        holder.name.setText(plan.name);

        String majorDisplay = plan.secondaryMajorName != null
                ? plan.majorName + " + " + plan.secondaryMajorName
                : plan.majorName;
        holder.major.setText(majorDisplay);
        holder.date.setText("Saved " + DATE_FORMAT.format(new Date(plan.createdAt)));

        holder.itemView.setOnClickListener(v -> listener.onPlanClick(plan));
        holder.deleteButton.setOnClickListener(v -> listener.onPlanDelete(plan));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView major;
        final TextView date;
        final MaterialButton deleteButton;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.text_plan_name);
            major = view.findViewById(R.id.text_plan_major);
            date = view.findViewById(R.id.text_plan_date);
            deleteButton = view.findViewById(R.id.btn_delete_plan);
        }
    }
}
