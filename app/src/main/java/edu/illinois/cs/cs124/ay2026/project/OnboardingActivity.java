package edu.illinois.cs.cs124.ay2026.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import edu.illinois.cs.cs124.ay2026.project.data.AppDatabase;
import edu.illinois.cs.cs124.ay2026.project.data.ProfileConverter;
import edu.illinois.cs.cs124.ay2026.project.data.UserProfileEntity;
import edu.illinois.cs.cs124.ay2026.project.model.Goal;

public class OnboardingActivity extends AppCompatActivity {

    private static final String[][] STEP_CONTENT = {
        {"About you", "Tell us when you're starting and what you want to achieve."},
        {"Prior credits", "Let us know about any credits you're bringing in."},
        {"Your interests", "Pick topics you'd enjoy exploring for gen-ed requirements."}
    };

    private static final Gson GSON = new Gson();

    private ViewFlipper viewFlipper;
    private View dot1, dot2, dot3;
    private android.widget.TextView textStepTitle;
    private android.widget.TextView textStepSubtitle;
    private MaterialButton btnBack;
    private MaterialButton btnNext;

    // Step 1
    private AutoCompleteTextView semesterDropdown;
    private AutoCompleteTextView yearDropdown;
    private CheckBox cbGraduateEarly;
    private CheckBox cbMinor;
    private CheckBox cbDoubleMajor;
    private CheckBox cbStudyAbroad;

    // Step 2
    private CheckBox cbHasAp;
    private CheckBox cbHasIb;
    private CheckBox cbHasDual;
    private CheckBox cbHasNone;

    // Step 3
    private static final int[] CHIP_GROUP_IDS = {
        R.id.onboard_chips_humanities,
        R.id.onboard_chips_sciences,
        R.id.onboard_chips_social,
        R.id.onboard_chips_cultural,
        R.id.onboard_chips_languages,
        R.id.onboard_chips_quant
    };
    private final List<ChipGroup> chipGroups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewFlipper     = findViewById(R.id.view_flipper);
        dot1            = findViewById(R.id.dot_1);
        dot2            = findViewById(R.id.dot_2);
        dot3            = findViewById(R.id.dot_3);
        textStepTitle   = findViewById(R.id.text_step_title);
        textStepSubtitle = findViewById(R.id.text_step_subtitle);
        btnBack         = findViewById(R.id.btn_onboard_back);
        btnNext         = findViewById(R.id.btn_onboard_next);

        setupStep1();
        setupStep3();

        // Step 2 views
        cbHasAp   = findViewById(R.id.onboard_has_ap);
        cbHasIb   = findViewById(R.id.onboard_has_ib);
        cbHasDual = findViewById(R.id.onboard_has_dual);
        cbHasNone = findViewById(R.id.onboard_has_none);
        cbHasNone.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                cbHasAp.setChecked(false);
                cbHasIb.setChecked(false);
                cbHasDual.setChecked(false);
            }
        });

        btnBack.setOnClickListener(v -> navigate(-1));
        btnNext.setOnClickListener(v -> navigate(1));

        updateHeader(0);
    }

    private void setupStep1() {
        semesterDropdown = findViewById(R.id.onboard_semester_dropdown);
        yearDropdown     = findViewById(R.id.onboard_year_dropdown);
        cbGraduateEarly  = findViewById(R.id.onboard_goal_graduate_early);
        cbMinor          = findViewById(R.id.onboard_goal_minor);
        cbDoubleMajor    = findViewById(R.id.onboard_goal_double_major);
        cbStudyAbroad    = findViewById(R.id.onboard_goal_study_abroad);

        semesterDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new String[]{"Fall", "Spring"}));

        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int y = currentYear - 2; y <= currentYear + 5; y++) years.add(String.valueOf(y));
        yearDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, years));
    }

    private void setupStep3() {
        for (int id : CHIP_GROUP_IDS) {
            chipGroups.add(findViewById(id));
        }
    }

    private void navigate(int direction) {
        int current = viewFlipper.getDisplayedChild();
        int next = current + direction;

        if (next < 0) return;

        if (current == 0 && direction > 0) {
            saveStep1();
        } else if (current == 1 && direction > 0) {
            saveStep2();
        }

        if (next >= viewFlipper.getChildCount()) {
            saveStep3AndFinish();
            return;
        }

        viewFlipper.setDisplayedChild(next);
        updateHeader(next);
    }

    private void updateHeader(int step) {
        textStepTitle.setText(STEP_CONTENT[step][0]);
        textStepSubtitle.setText(STEP_CONTENT[step][1]);
        btnBack.setVisibility(step == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(step == viewFlipper.getChildCount() - 1 ? "Get started" : "Next");

        int butterColor  = getResources().getColor(R.color.declared_butter, null);
        int sageColor    = getResources().getColor(R.color.declared_sage, null);
        dot1.setBackgroundColor(step == 0 ? butterColor : sageColor);
        dot2.setBackgroundColor(step == 1 ? butterColor : sageColor);
        dot3.setBackgroundColor(step == 2 ? butterColor : sageColor);
    }

    private void saveStep1() {
        String term    = semesterDropdown.getText().toString().trim();
        String yearStr = yearDropdown.getText().toString().trim();

        List<String> goalNames = new ArrayList<>();
        if (cbGraduateEarly.isChecked()) goalNames.add(Goal.GRADUATE_EARLY.name());
        if (cbMinor.isChecked())         goalNames.add(Goal.ADD_MINOR.name());
        if (cbDoubleMajor.isChecked())   goalNames.add(Goal.DOUBLE_MAJOR.name());
        if (cbStudyAbroad.isChecked())   goalNames.add(Goal.STUDY_ABROAD.name());

        int year = 0;
        try { year = Integer.parseInt(yearStr); } catch (NumberFormatException ignored) { }

        final String finalTerm  = term.isEmpty() ? "Fall" : term;
        final int    finalYear  = year == 0 ? java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) : year;
        final String goalsJson  = ProfileConverter.goalsToJson(goalNames);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            UserProfileEntity entity = db.userProfileDao().getProfile();
            if (entity == null) entity = new UserProfileEntity();
            entity.startTerm = finalTerm;
            entity.startYear = finalYear;
            entity.goalsJson = goalsJson;
            if (entity.apScoresJson == null)          entity.apScoresJson = "{}";
            if (entity.ibScoresJson == null)          entity.ibScoresJson = "{}";
            if (entity.dualCreditCoursesJson == null) entity.dualCreditCoursesJson = "[]";
            if (entity.interestsJson == null)         entity.interestsJson = "[]";
            db.userProfileDao().upsert(entity);
        }).start();
    }

    private void saveStep2() {
        boolean hasAp   = cbHasAp.isChecked();
        boolean hasIb   = cbHasIb.isChecked();
        boolean hasDual = cbHasDual.isChecked();

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            UserProfileEntity entity = db.userProfileDao().getProfile();
            if (entity == null) entity = new UserProfileEntity();
            if (entity.apScoresJson == null || entity.apScoresJson.equals("{}")) {
                entity.apScoresJson = hasAp ? "{}" : "{}";
            }
            if (entity.ibScoresJson == null || entity.ibScoresJson.equals("{}")) {
                entity.ibScoresJson = hasIb ? "{}" : "{}";
            }
            if (entity.dualCreditCoursesJson == null || entity.dualCreditCoursesJson.equals("[]")) {
                entity.dualCreditCoursesJson = hasDual ? "[]" : "[]";
            }
            if (entity.goalsJson == null)     entity.goalsJson = "[]";
            if (entity.interestsJson == null) entity.interestsJson = "[]";
            if (entity.startTerm == null)     entity.startTerm = "Fall";
            db.userProfileDao().upsert(entity);
        }).start();
    }

    private void saveStep3AndFinish() {
        List<String> selected = new ArrayList<>();
        for (ChipGroup group : chipGroups) {
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip chip = (Chip) group.getChildAt(i);
                if (chip.isChecked()) selected.add(chip.getText().toString());
            }
        }
        String interestsJson = GSON.toJson(selected);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            UserProfileEntity entity = db.userProfileDao().getProfile();
            if (entity == null) entity = new UserProfileEntity();
            entity.interestsJson = interestsJson;
            if (entity.goalsJson == null)             entity.goalsJson = "[]";
            if (entity.apScoresJson == null)          entity.apScoresJson = "{}";
            if (entity.ibScoresJson == null)          entity.ibScoresJson = "{}";
            if (entity.dualCreditCoursesJson == null) entity.dualCreditCoursesJson = "[]";
            if (entity.startTerm == null)             entity.startTerm = "Fall";
            db.userProfileDao().upsert(entity);

            runOnUiThread(() -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        }).start();
    }
}