package edu.illinois.cs.cs124.ay2026.project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.ImageButton;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.data.MajorRepository;
import edu.illinois.cs.cs124.ay2026.project.model.ApExam;
import edu.illinois.cs.cs124.ay2026.project.model.Goal;
import edu.illinois.cs.cs124.ay2026.project.model.IbExam;
import edu.illinois.cs.cs124.ay2026.project.model.Major;
import edu.illinois.cs.cs124.ay2026.project.model.UserProfile;
import edu.illinois.cs.cs124.ay2026.project.util.ExamRowHelper;

public class QuestionnaireActivity extends AppCompatActivity {

    public static final String EXTRA_USER_PROFILE = "user_profile";

    private final Map<ApExam, int[]>   apScoreState  = new LinkedHashMap<>();
    private final Map<ApExam, CheckBox> apCheckboxes = new LinkedHashMap<>();
    private final Map<IbExam, int[]>   ibScoreState  = new LinkedHashMap<>();
    private final Map<IbExam, CheckBox> ibCheckboxes = new LinkedHashMap<>();
    private final List<String> dualCreditCourses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        ImageButton buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> finish());

        // Semester dropdown
        AutoCompleteTextView semesterDropdown = findViewById(R.id.semester_dropdown);
        semesterDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new String[]{"Fall", "Spring"}));

        // Year dropdown
        AutoCompleteTextView yearDropdown = findViewById(R.id.year_dropdown);
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int y = currentYear - 2; y <= currentYear + 4; y++) years.add(String.valueOf(y));
        yearDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, years));

        // AP Exams section
        CheckBox cbHasAp = findViewById(R.id.cb_has_ap);
        LinearLayout apContainer = findViewById(R.id.ap_exams_container);
        for (ApExam exam : ApExam.values()) {
            ExamRowHelper.addExamRow(this, exam.getDisplayName(), 3, 1, 5,
                    apContainer, exam, apCheckboxes, apScoreState);
        }
        cbHasAp.setOnCheckedChangeListener((btn, checked) ->
                apContainer.setVisibility(checked ? View.VISIBLE : View.GONE));

        // IB Exams section
        CheckBox cbHasIb = findViewById(R.id.cb_has_ib);
        android.widget.TextView ibHint = findViewById(R.id.ib_hint_text);
        LinearLayout ibContainer = findViewById(R.id.ib_exams_container);
        for (IbExam exam : IbExam.values()) {
            ExamRowHelper.addExamRow(this, exam.getDisplayName(), 5, 1, 7,
                    ibContainer, exam, ibCheckboxes, ibScoreState);
        }
        cbHasIb.setOnCheckedChangeListener((btn, checked) -> {
            ibHint.setVisibility(checked ? View.VISIBLE : View.GONE);
            ibContainer.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        // Dual credit section
        CheckBox cbHasDual = findViewById(R.id.cb_has_dual);
        LinearLayout dualContainer = findViewById(R.id.dual_credit_container);
        TextInputEditText dualInput = findViewById(R.id.dual_credit_input);
        MaterialButton addDualBtn = findViewById(R.id.btn_add_dual_credit);
        ChipGroup dualChipGroup = findViewById(R.id.dual_credit_chips);

        cbHasDual.setOnCheckedChangeListener((btn, checked) ->
                dualContainer.setVisibility(checked ? View.VISIBLE : View.GONE));

        addDualBtn.setOnClickListener(v -> {
            String raw = dualInput.getText() != null
                    ? dualInput.getText().toString().trim().toUpperCase() : "";
            if (TextUtils.isEmpty(raw) || dualCreditCourses.contains(raw)) {
                dualInput.setText("");
                return;
            }
            dualCreditCourses.add(raw);
            dualInput.setText("");
            Chip chip = new Chip(this);
            chip.setText(raw);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(chipView -> {
                dualCreditCourses.remove(raw);
                dualChipGroup.removeView(chipView);
            });
            dualChipGroup.addView(chip);
        });

        // Goals
        CheckBox cbGraduateEarly = findViewById(R.id.goal_graduate_early);
        CheckBox cbMinor         = findViewById(R.id.goal_minor);
        CheckBox cbDoubleMajor   = findViewById(R.id.goal_double_major);
        CheckBox cbStudyAbroad   = findViewById(R.id.goal_study_abroad);
        CheckBox cbCpaTrack      = findViewById(R.id.goal_cpa_track);
        CheckBox cbOnlineGenEds  = findViewById(R.id.pref_online_gen_eds);

        String majorName = getIntent().getStringExtra(NewPlanActivity.EXTRA_MAJOR_NAME);
        if ("Accounting".equals(majorName)) cbCpaTrack.setVisibility(View.VISIBLE);

        // Concentration picker
        View cardConcentration = findViewById(R.id.card_concentration);
        AutoCompleteTextView concentrationDropdown = findViewById(R.id.concentration_dropdown);
        final List<Major> concentrations = new ArrayList<>();
        final String[] selectedConcentrationName = {null};

        Major selectedMajor = MajorRepository.findByName(this, majorName);
        if (selectedMajor != null && selectedMajor.hasConcentrations()) {
            concentrations.addAll(selectedMajor.getConcentrations());
            List<String> labels = new ArrayList<>();
            for (Major c : concentrations) labels.add(Major.concentrationLabel(selectedMajor, c));
            concentrationDropdown.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, labels));
            concentrationDropdown.setOnItemClickListener((parent, view, position, id) ->
                    selectedConcentrationName[0] = concentrations.get(position).getName());
            cardConcentration.setVisibility(View.VISIBLE);
        }

        // Interests
        ChipGroup chipGroup = findViewById(R.id.interest_chips);

        // Build Plan button
        MaterialButton buildButton = findViewById(R.id.button_build_plan);
        buildButton.setOnClickListener(v -> {
            String term = semesterDropdown.getText().toString().trim();
            String yearStr = yearDropdown.getText().toString().trim();

            if (term.isEmpty() || yearStr.isEmpty()) {
                Toast.makeText(this, "Please select a start semester and year.", Toast.LENGTH_SHORT).show();
                return;
            }

            int startYear;
            try {
                startYear = Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid year.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Integer> apScores = new HashMap<>();
            for (Map.Entry<ApExam, CheckBox> entry : apCheckboxes.entrySet()) {
                if (entry.getValue().isChecked()) {
                    apScores.put(entry.getKey().name(), apScoreState.get(entry.getKey())[0]);
                }
            }

            Map<String, Integer> ibScores = new HashMap<>();
            for (Map.Entry<IbExam, CheckBox> entry : ibCheckboxes.entrySet()) {
                if (entry.getValue().isChecked()) {
                    ibScores.put(entry.getKey().name(), ibScoreState.get(entry.getKey())[0]);
                }
            }

            Set<Goal> goals = EnumSet.noneOf(Goal.class);
            if (cbGraduateEarly.isChecked()) goals.add(Goal.GRADUATE_EARLY);
            if (cbMinor.isChecked())         goals.add(Goal.ADD_MINOR);
            if (cbDoubleMajor.isChecked())   goals.add(Goal.DOUBLE_MAJOR);
            if (cbStudyAbroad.isChecked())   goals.add(Goal.STUDY_ABROAD);
            if (cbCpaTrack.isChecked())      goals.add(Goal.CPA_TRACK);

            Set<String> interests = new HashSet<>();
            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                Chip chip = (Chip) chipGroup.getChildAt(i);
                if (chip.isChecked()) interests.add(chip.getText().toString());
            }

            UserProfile profile = new UserProfile(term, startYear, goals, interests,
                    apScores, ibScores, new ArrayList<>(dualCreditCourses),
                    selectedConcentrationName[0], cbOnlineGenEds.isChecked());

            Intent intent = new Intent(this, PlanViewActivity.class);
            intent.putExtra(NewPlanActivity.EXTRA_MAJOR_NAME, majorName);
            intent.putExtra(EXTRA_USER_PROFILE, profile);
            startActivity(intent);
        });
    }
}
