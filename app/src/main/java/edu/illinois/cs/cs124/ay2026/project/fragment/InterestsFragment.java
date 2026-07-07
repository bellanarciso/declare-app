package edu.illinois.cs.cs124.ay2026.project.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.PlanGenerator;
import edu.illinois.cs.cs124.ay2026.project.R;
import edu.illinois.cs.cs124.ay2026.project.data.AppDatabase;
import edu.illinois.cs.cs124.ay2026.project.data.SavedPlanEntity;
import edu.illinois.cs.cs124.ay2026.project.data.UserProfileEntity;

public class InterestsFragment extends Fragment {

    public static final String TAG = "InterestsFragment";

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    private static final int[] CHIP_GROUP_IDS = {
        R.id.chips_humanities,
        R.id.chips_sciences,
        R.id.chips_social,
        R.id.chips_cultural,
        R.id.chips_languages,
        R.id.chips_quant
    };

    private final List<ChipGroup> chipGroups = new ArrayList<>();
    private View cardSuggestions;
    private LinearLayout suggestionsContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_interests, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        for (int id : CHIP_GROUP_IDS) {
            chipGroups.add(view.findViewById(id));
        }
        cardSuggestions      = view.findViewById(R.id.card_suggestions);
        suggestionsContainer = view.findViewById(R.id.suggestions_container);

        view.findViewById(R.id.btn_save_interests).setOnClickListener(v -> saveInterests());
        view.findViewById(R.id.btn_apply_to_plans).setOnClickListener(v -> applyInterestsToSavedPlans());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInterests();
    }

    private void loadInterests() {
        new Thread(() -> {
            UserProfileEntity entity = AppDatabase.getInstance(requireContext())
                    .userProfileDao().getProfile();
            requireActivity().runOnUiThread(() -> {
                restoreUi(entity);
                // Re-show suggestions if interests were already saved
                if (entity != null && entity.interestsJson != null) {
                    List<String> saved = GSON.fromJson(entity.interestsJson, LIST_TYPE);
                    if (saved != null && !saved.isEmpty()) {
                        showSuggestions(new HashSet<>(saved));
                    }
                }
            });
        }).start();
    }

    private void restoreUi(UserProfileEntity entity) {
        if (entity == null || entity.interestsJson == null) return;
        List<String> interests = GSON.fromJson(entity.interestsJson, LIST_TYPE);
        if (interests == null) return;
        for (ChipGroup group : chipGroups) {
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip chip = (Chip) group.getChildAt(i);
                chip.setChecked(interests.contains(chip.getText().toString()));
            }
        }
    }

    private void saveInterests() {
        List<String> selected = new ArrayList<>();
        for (ChipGroup group : chipGroups) {
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip chip = (Chip) group.getChildAt(i);
                if (chip.isChecked()) selected.add(chip.getText().toString());
            }
        }
        String interestsJson = GSON.toJson(selected);

        new Thread(() -> {
            UserProfileEntity existing = AppDatabase.getInstance(requireContext())
                    .userProfileDao().getProfile();
            UserProfileEntity updated = existing != null ? existing : new UserProfileEntity();

            updated.interestsJson = interestsJson;
            if (updated.goalsJson == null)             updated.goalsJson = "[]";
            if (updated.apScoresJson == null)          updated.apScoresJson = "{}";
            if (updated.ibScoresJson == null)          updated.ibScoresJson = "{}";
            if (updated.dualCreditCoursesJson == null) updated.dualCreditCoursesJson = "[]";
            if (updated.startTerm == null)             updated.startTerm = "Fall";

            AppDatabase.getInstance(requireContext()).userProfileDao().upsert(updated);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Interests saved.", Toast.LENGTH_SHORT).show();
                showSuggestions(new HashSet<>(selected));
            });
        }).start();
    }

    private void showSuggestions(Set<String> interests) {
        List<PlanGenerator.GenEdSuggestion> suggestions =
                PlanGenerator.suggestedGenEds(interests);

        if (suggestions.isEmpty()) {
            cardSuggestions.setVisibility(View.GONE);
            return;
        }

        suggestionsContainer.removeAllViews();
        for (PlanGenerator.GenEdSuggestion suggestion : suggestions) {
            suggestionsContainer.addView(buildSuggestionRow(suggestion));
        }
        cardSuggestions.setVisibility(View.VISIBLE);
    }

    private View buildSuggestionRow(PlanGenerator.GenEdSuggestion suggestion) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dpToPx(12));
        row.setLayoutParams(rowParams);

        TextView categoryView = new TextView(requireContext());
        categoryView.setText(suggestion.categoryLabel);
        categoryView.setTextColor(getResources().getColor(R.color.declared_forest, null));
        categoryView.setTextSize(12f);
        categoryView.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(categoryView);

        TextView courseView = new TextView(requireContext());
        String courseText = suggestion.course.getId() + "  ·  " + suggestion.course.getName();
        courseView.setText(courseText);
        courseView.setTextColor(0xFF333333);
        courseView.setTextSize(14f);
        LinearLayout.LayoutParams courseParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        courseParams.setMargins(0, dpToPx(2), 0, 0);
        courseView.setLayoutParams(courseParams);
        row.addView(courseView);

        return row;
    }

    private void applyInterestsToSavedPlans() {
        List<String> selected = new ArrayList<>();
        for (ChipGroup group : chipGroups) {
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip chip = (Chip) group.getChildAt(i);
                if (chip.isChecked()) selected.add(chip.getText().toString());
            }
        }
        String interestsJson = GSON.toJson(selected);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            // Always persist to the live profile first so the chip state and profile stay in sync.
            UserProfileEntity existing = db.userProfileDao().getProfile();
            UserProfileEntity updated = existing != null ? existing : new UserProfileEntity();
            updated.interestsJson = interestsJson;
            if (updated.goalsJson == null)             updated.goalsJson = "[]";
            if (updated.apScoresJson == null)          updated.apScoresJson = "{}";
            if (updated.ibScoresJson == null)          updated.ibScoresJson = "{}";
            if (updated.dualCreditCoursesJson == null) updated.dualCreditCoursesJson = "[]";
            if (updated.startTerm == null)             updated.startTerm = "Fall";
            db.userProfileDao().upsert(updated);

            // Propagate updated interests into every saved plan snapshot.
            List<SavedPlanEntity> plans = db.savedPlanDao().getAll();
            int count = 0;
            for (SavedPlanEntity plan : plans) {
                UserProfileEntity snap = plan.profileSnapshot != null
                        ? GSON.fromJson(plan.profileSnapshot, UserProfileEntity.class)
                        : new UserProfileEntity();
                if (snap == null) snap = new UserProfileEntity();
                snap.interestsJson = interestsJson;
                plan.profileSnapshot = GSON.toJson(snap);
                db.savedPlanDao().update(plan);
                count++;
            }

            int finalCount = count;
            requireActivity().runOnUiThread(() -> {
                String msg = finalCount == 0
                        ? "Interests saved. No saved plans to update."
                        : "Interests saved and applied to "
                                + finalCount + " plan" + (finalCount == 1 ? "" : "s") + ".";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}