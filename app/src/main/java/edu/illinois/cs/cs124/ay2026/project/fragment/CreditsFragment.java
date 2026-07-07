package edu.illinois.cs.cs124.ay2026.project.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.illinois.cs.cs124.ay2026.project.R;
import edu.illinois.cs.cs124.ay2026.project.data.AppDatabase;
import edu.illinois.cs.cs124.ay2026.project.data.UserProfileEntity;
import edu.illinois.cs.cs124.ay2026.project.model.ApExam;
import edu.illinois.cs.cs124.ay2026.project.model.IbExam;
import edu.illinois.cs.cs124.ay2026.project.util.ExamRowHelper;

public class CreditsFragment extends Fragment {

    public static final String TAG = "CreditsFragment";

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    private final Map<ApExam, CheckBox> apCheckboxes = new LinkedHashMap<>();
    private final Map<ApExam, int[]>   apScoreState  = new LinkedHashMap<>();
    private final Map<IbExam, CheckBox> ibCheckboxes = new LinkedHashMap<>();
    private final Map<IbExam, int[]>   ibScoreState  = new LinkedHashMap<>();
    private final List<String> dualCreditCourses = new ArrayList<>();

    private ChipGroup dualChipGroup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_credits, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // AP section
        CheckBox cbHasAp = view.findViewById(R.id.credits_cb_has_ap);
        LinearLayout apContainer = view.findViewById(R.id.credits_ap_container);
        for (ApExam exam : ApExam.values()) {
            ExamRowHelper.addExamRow(requireContext(), exam.getDisplayName(),
                    3, 1, 5, apContainer, exam, apCheckboxes, apScoreState);
        }
        cbHasAp.setOnCheckedChangeListener((btn, checked) ->
                apContainer.setVisibility(checked ? View.VISIBLE : View.GONE));

        // IB section
        CheckBox cbHasIb = view.findViewById(R.id.credits_cb_has_ib);
        View ibHint = view.findViewById(R.id.credits_ib_hint);
        LinearLayout ibContainer = view.findViewById(R.id.credits_ib_container);
        for (IbExam exam : IbExam.values()) {
            ExamRowHelper.addExamRow(requireContext(), exam.getDisplayName(),
                    5, 1, 7, ibContainer, exam, ibCheckboxes, ibScoreState);
        }
        cbHasIb.setOnCheckedChangeListener((btn, checked) -> {
            ibHint.setVisibility(checked ? View.VISIBLE : View.GONE);
            ibContainer.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        // Dual credit section
        CheckBox cbHasDual = view.findViewById(R.id.credits_cb_has_dual);
        LinearLayout dualContainer = view.findViewById(R.id.credits_dual_container);
        TextInputEditText dualInput = view.findViewById(R.id.credits_dual_input);
        MaterialButton addDualBtn = view.findViewById(R.id.credits_btn_add_dual);
        dualChipGroup = view.findViewById(R.id.credits_dual_chips);

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
            addDualChip(raw);
        });

        view.findViewById(R.id.btn_save_credits).setOnClickListener(v -> saveCredits());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCredits();
    }

    private void loadCredits() {
        new Thread(() -> {
            UserProfileEntity entity = AppDatabase.getInstance(requireContext())
                    .userProfileDao().getProfile();
            requireActivity().runOnUiThread(() -> restoreUi(entity));
        }).start();
    }

    private void restoreUi(UserProfileEntity entity) {
        if (entity == null) return;

        // Restore AP scores
        Map<String, Integer> apScores = GSON.fromJson(entity.apScoresJson, MAP_TYPE);
        if (apScores != null) {
            for (Map.Entry<String, Integer> entry : apScores.entrySet()) {
                try {
                    ApExam exam = ApExam.valueOf(entry.getKey());
                    ExamRowHelper.restoreExamRow(exam, entry.getValue(), apCheckboxes, apScoreState);
                } catch (IllegalArgumentException ignored) { }
            }
            if (!apScores.isEmpty()) {
                // Expand the AP section
                CheckBox cbHasAp = requireView().findViewById(R.id.credits_cb_has_ap);
                cbHasAp.setChecked(true);
            }
        }

        // Restore IB scores
        Map<String, Integer> ibScores = GSON.fromJson(entity.ibScoresJson, MAP_TYPE);
        if (ibScores != null) {
            for (Map.Entry<String, Integer> entry : ibScores.entrySet()) {
                try {
                    IbExam exam = IbExam.valueOf(entry.getKey());
                    ExamRowHelper.restoreExamRow(exam, entry.getValue(), ibCheckboxes, ibScoreState);
                } catch (IllegalArgumentException ignored) { }
            }
            if (!ibScores.isEmpty()) {
                CheckBox cbHasIb = requireView().findViewById(R.id.credits_cb_has_ib);
                cbHasIb.setChecked(true);
            }
        }

        // Restore dual credit chips
        List<String> dualCourses = GSON.fromJson(entity.dualCreditCoursesJson, LIST_TYPE);
        if (dualCourses != null && !dualCourses.isEmpty()) {
            dualCreditCourses.clear();
            dualChipGroup.removeAllViews();
            for (String course : dualCourses) {
                dualCreditCourses.add(course);
                addDualChip(course);
            }
            CheckBox cbHasDual = requireView().findViewById(R.id.credits_cb_has_dual);
            cbHasDual.setChecked(true);
        }
    }

    private void addDualChip(String courseId) {
        Chip chip = new Chip(requireContext());
        chip.setText(courseId);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            dualCreditCourses.remove(courseId);
            dualChipGroup.removeView(v);
        });
        dualChipGroup.addView(chip);
    }

    private void saveCredits() {
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

        String apJson    = GSON.toJson(apScores);
        String ibJson    = GSON.toJson(ibScores);
        String dualJson  = GSON.toJson(dualCreditCourses);

        new Thread(() -> {
            UserProfileEntity existing = AppDatabase.getInstance(requireContext())
                    .userProfileDao().getProfile();
            UserProfileEntity updated = existing != null ? existing : new UserProfileEntity();

            updated.apScoresJson           = apJson;
            updated.ibScoresJson           = ibJson;
            updated.dualCreditCoursesJson  = dualJson;
            if (updated.goalsJson == null)     updated.goalsJson = "[]";
            if (updated.interestsJson == null) updated.interestsJson = "[]";
            if (updated.startTerm == null)     updated.startTerm = "Fall";

            AppDatabase.getInstance(requireContext()).userProfileDao().upsert(updated);
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Credits saved.", Toast.LENGTH_SHORT).show());
        }).start();
    }
}
