package edu.illinois.cs.cs124.ay2026.project.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import edu.illinois.cs.cs124.ay2026.project.CompareActivity;
import edu.illinois.cs.cs124.ay2026.project.NewPlanActivity;
import edu.illinois.cs.cs124.ay2026.project.PlanViewActivity;
import edu.illinois.cs.cs124.ay2026.project.R;
import edu.illinois.cs.cs124.ay2026.project.adapter.SavedPlanAdapter;
import edu.illinois.cs.cs124.ay2026.project.data.AppDatabase;
import edu.illinois.cs.cs124.ay2026.project.data.SavedPlanEntity;

public class PlansFragment extends Fragment implements SavedPlanAdapter.OnPlanClickListener {

    public static final String TAG = "PlansFragment";

    private RecyclerView recyclerView;
    private View emptyState;
    private SavedPlanAdapter adapter;
    private final List<SavedPlanEntity> plans = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plans, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recycler_plans);
        emptyState   = view.findViewById(R.id.empty_state);
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_new_plan);
        MaterialButton compareButton     = view.findViewById(R.id.btn_compare);

        adapter = new SavedPlanAdapter(plans, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NewPlanActivity.class)));
        compareButton.setOnClickListener(v -> showComparePicker());
    }

    private void showComparePicker() {
        if (plans.size() < 2) {
            Toast.makeText(requireContext(),
                    "Save at least 2 plans to compare", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[plans.size()];
        for (int i = 0; i < plans.size(); i++) names[i] = plans.get(i).name;

        int[] selectedA = {0};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Plan A")
                .setSingleChoiceItems(names, 0, (d, which) -> selectedA[0] = which)
                .setPositiveButton("Next", (d, which) -> pickPlanB(selectedA[0]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickPlanB(int indexA) {
        List<SavedPlanEntity> others = new ArrayList<>();
        for (int i = 0; i < plans.size(); i++) {
            if (i != indexA) others.add(plans.get(i));
        }

        String[] names = new String[others.size()];
        for (int i = 0; i < others.size(); i++) names[i] = others.get(i).name;

        int[] selectedB = {0};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Plan B")
                .setSingleChoiceItems(names, 0, (d, which) -> selectedB[0] = which)
                .setPositiveButton("Compare", (d, which) -> {
                    Intent intent = new Intent(requireContext(), CompareActivity.class);
                    intent.putExtra(CompareActivity.EXTRA_PLAN_A_ID, plans.get(indexA).id);
                    intent.putExtra(CompareActivity.EXTRA_PLAN_B_ID, others.get(selectedB[0]).id);
                    startActivity(intent);
                })
                .setNegativeButton("Back", (d, which) -> showComparePicker())
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlans();
    }

    private void loadPlans() {
        new Thread(() -> {
            List<SavedPlanEntity> loaded = AppDatabase
                    .getInstance(requireContext())
                    .savedPlanDao()
                    .getAll();
            requireActivity().runOnUiThread(() -> {
                plans.clear();
                if (loaded != null) plans.addAll(loaded);
                adapter.notifyDataSetChanged();
                boolean isEmpty = plans.isEmpty();
                emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    @Override
    public void onPlanClick(SavedPlanEntity plan) {
        Intent intent = new Intent(requireContext(), PlanViewActivity.class);
        intent.putExtra(NewPlanActivity.EXTRA_MAJOR_NAME, plan.majorName);
        intent.putExtra(PlanViewActivity.EXTRA_SECONDARY_MAJOR_NAME, plan.secondaryMajorName);
        intent.putExtra(PlanViewActivity.EXTRA_PROFILE_SNAPSHOT, plan.profileSnapshot);
        intent.putExtra(PlanViewActivity.EXTRA_SAVED_PLAN_ID, plan.id);
        startActivity(intent);
    }

    @Override
    public void onPlanDelete(SavedPlanEntity plan) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Plan")
                .setMessage("Delete \"" + plan.name + "\"? This can't be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePlan(plan))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePlan(SavedPlanEntity plan) {
        new Thread(() -> {
            AppDatabase.getInstance(requireContext()).savedPlanDao().deleteById(plan.id);
            requireActivity().runOnUiThread(this::loadPlans);
        }).start();
    }

}
