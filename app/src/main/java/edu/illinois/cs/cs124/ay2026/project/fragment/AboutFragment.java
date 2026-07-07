package edu.illinois.cs.cs124.ay2026.project.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.R;
import edu.illinois.cs.cs124.ay2026.project.data.AppDatabase;
import edu.illinois.cs.cs124.ay2026.project.data.ProfileConverter;
import edu.illinois.cs.cs124.ay2026.project.data.UserProfileEntity;
import edu.illinois.cs.cs124.ay2026.project.model.Goal;

public class AboutFragment extends Fragment {

    public static final String TAG = "AboutFragment";

    private AutoCompleteTextView semesterDropdown;
    private AutoCompleteTextView yearDropdown;
    private CheckBox cbGraduateEarly;
    private CheckBox cbMinor;
    private CheckBox cbDoubleMajor;
    private CheckBox cbStudyAbroad;
    private CheckBox cbOnlineGenEds;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        semesterDropdown = view.findViewById(R.id.profile_semester_dropdown);
        yearDropdown     = view.findViewById(R.id.profile_year_dropdown);
        cbGraduateEarly  = view.findViewById(R.id.profile_goal_graduate_early);
        cbMinor          = view.findViewById(R.id.profile_goal_minor);
        cbDoubleMajor    = view.findViewById(R.id.profile_goal_double_major);
        cbStudyAbroad    = view.findViewById(R.id.profile_goal_study_abroad);
        cbOnlineGenEds   = view.findViewById(R.id.profile_pref_online_gen_eds);
        MaterialButton saveButton = view.findViewById(R.id.btn_save_profile);

        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new String[]{"Fall", "Spring"});
        semesterDropdown.setAdapter(semesterAdapter);

        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int y = currentYear - 2; y <= currentYear + 4; y++) years.add(String.valueOf(y));
        yearDropdown.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, years));

        saveButton.setOnClickListener(v -> saveProfile());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        new Thread(() -> {
            UserProfileEntity entity = AppDatabase.getInstance(requireContext())
                    .userProfileDao().getProfile();
            requireActivity().runOnUiThread(() -> populateUi(entity));
        }).start();
    }

    private void populateUi(UserProfileEntity entity) {
        if (entity == null) return;
        if (entity.startTerm != null) semesterDropdown.setText(entity.startTerm, false);
        if (entity.startYear > 0) yearDropdown.setText(String.valueOf(entity.startYear), false);

        Set<Goal> goals = EnumSet.noneOf(Goal.class);
        if (entity.goalsJson != null) {
            for (Goal g : Goal.values()) {
                if (entity.goalsJson.contains(g.name())) goals.add(g);
            }
        }
        cbGraduateEarly.setChecked(goals.contains(Goal.GRADUATE_EARLY));
        cbMinor.setChecked(goals.contains(Goal.ADD_MINOR));
        cbDoubleMajor.setChecked(goals.contains(Goal.DOUBLE_MAJOR));
        cbStudyAbroad.setChecked(goals.contains(Goal.STUDY_ABROAD));
        cbOnlineGenEds.setChecked(entity.preferOnlineGenEds);
    }

    private void saveProfile() {
        String term = semesterDropdown.getText().toString().trim();
        String yearStr = yearDropdown.getText().toString().trim();

        if (term.isEmpty() || yearStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a start semester and year.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        int year;
        try {
            year = Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid year.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> goalNames = new ArrayList<>();
        if (cbGraduateEarly.isChecked()) goalNames.add(Goal.GRADUATE_EARLY.name());
        if (cbMinor.isChecked())         goalNames.add(Goal.ADD_MINOR.name());
        if (cbDoubleMajor.isChecked())   goalNames.add(Goal.DOUBLE_MAJOR.name());
        if (cbStudyAbroad.isChecked())   goalNames.add(Goal.STUDY_ABROAD.name());

        final int finalYear = year;
        new Thread(() -> {
            UserProfileEntity existing = AppDatabase.getInstance(requireContext())
                    .userProfileDao().getProfile();
            UserProfileEntity updated = existing != null ? existing : new UserProfileEntity();
            updated.startTerm = term;
            updated.startYear = finalYear;
            updated.goalsJson = ProfileConverter.goalsToJson(goalNames);
            updated.preferOnlineGenEds = cbOnlineGenEds.isChecked();
            if (updated.apScoresJson == null)          updated.apScoresJson = "{}";
            if (updated.ibScoresJson == null)          updated.ibScoresJson = "{}";
            if (updated.dualCreditCoursesJson == null) updated.dualCreditCoursesJson = "[]";
            if (updated.interestsJson == null)         updated.interestsJson = "[]";
            AppDatabase.getInstance(requireContext()).userProfileDao().upsert(updated);
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Saved.", Toast.LENGTH_SHORT).show());
        }).start();
    }
}