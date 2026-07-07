package edu.illinois.cs.cs124.ay2026.project.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import edu.illinois.cs.cs124.ay2026.project.R;
import edu.illinois.cs.cs124.ay2026.project.data.AppDatabase;
import edu.illinois.cs.cs124.ay2026.project.data.UserProfileEntity;
import edu.illinois.cs.cs124.ay2026.project.model.ApExam;
import edu.illinois.cs.cs124.ay2026.project.model.IbExam;

public class FinancialFragment extends Fragment {

    public static final String TAG = "FinancialFragment";

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();

    private static final double PRESET_IN_STATE    = 17_200.0;
    private static final double PRESET_OUT_OF_STATE = 24_000.0;
    private static final int    SEMESTERS_PER_YEAR  = 2;
    private static final int    STANDARD_YEARS      = 4;
    private static final int    CREDITS_PER_SEMESTER = 15;

    private TextInputEditText tuitionInput;
    private View cardResults;
    private TextView textFullCost;
    private TextView textCreditsLabel;
    private TextView textCreditSavings;
    private TextView textNetCost;

    // AP/IB credits loaded from the stored profile
    private int savedIncomingCredits = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_financial, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        tuitionInput    = view.findViewById(R.id.tuition_input);
        cardResults     = view.findViewById(R.id.card_results);
        textFullCost    = view.findViewById(R.id.text_full_cost);
        textCreditsLabel = view.findViewById(R.id.text_credits_label);
        textCreditSavings = view.findViewById(R.id.text_credit_savings);
        textNetCost     = view.findViewById(R.id.text_net_cost);

        MaterialButton calculateBtn = view.findViewById(R.id.btn_calculate);
        calculateBtn.setOnClickListener(v -> calculate());

        // Preset chips
        ChipGroup presets = view.findViewById(R.id.tuition_presets);
        Chip chipInState     = view.findViewById(R.id.chip_in_state);
        Chip chipOutOfState  = view.findViewById(R.id.chip_out_of_state);

        chipInState.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) tuitionInput.setText(String.valueOf((int) PRESET_IN_STATE));
        });
        chipOutOfState.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) tuitionInput.setText(String.valueOf((int) PRESET_OUT_OF_STATE));
        });
        // Deselect preset chips when the user edits the field manually
        tuitionInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) presets.clearCheck();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadIncomingCredits();
    }

    private void loadIncomingCredits() {
        new Thread(() -> {
            UserProfileEntity entity = AppDatabase.getInstance(requireContext())
                    .userProfileDao().getProfile();
            int credits = computeIncomingCredits(entity);
            requireActivity().runOnUiThread(() -> savedIncomingCredits = credits);
        }).start();
    }

    private void calculate() {
        String raw = tuitionInput.getText() != null
                ? tuitionInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(raw)) {
            Toast.makeText(requireContext(), "Enter a per-semester cost to calculate.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double semesterCost;
        try {
            semesterCost = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (semesterCost <= 0) {
            Toast.makeText(requireContext(), "Enter a cost greater than zero.", Toast.LENGTH_SHORT).show();
            return;
        }

        double fullCost = semesterCost * SEMESTERS_PER_YEAR * STANDARD_YEARS;

        // AP/IB credits reduce total credits needed; each semester is ~15 credits
        double creditSavings = (savedIncomingCredits / (double) CREDITS_PER_SEMESTER) * semesterCost;

        double netCost = fullCost - creditSavings;
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

        textFullCost.setText(currency.format(fullCost));
        textCreditsLabel.setText(savedIncomingCredits + " incoming credits saved");
        textCreditSavings.setText("− " + currency.format(creditSavings));
        textNetCost.setText(currency.format(Math.max(0, netCost)));

        cardResults.setVisibility(View.VISIBLE);
    }

    /** Computes total incoming credit hours from the stored profile entity. */
    private int computeIncomingCredits(UserProfileEntity entity) {
        if (entity == null) return 0;
        int total = 0;

        Map<String, Integer> apScores = GSON.fromJson(entity.apScoresJson, MAP_TYPE);
        if (apScores != null) {
            for (Map.Entry<String, Integer> entry : apScores.entrySet()) {
                try {
                    ApExam exam = ApExam.valueOf(entry.getKey());
                    if (entry.getValue() >= exam.getMinScoreForCredit()) {
                        total += exam.getCreditHours();
                    }
                } catch (IllegalArgumentException ignored) { }
            }
        }

        Map<String, Integer> ibScores = GSON.fromJson(entity.ibScoresJson, MAP_TYPE);
        if (ibScores != null) {
            for (Map.Entry<String, Integer> entry : ibScores.entrySet()) {
                try {
                    IbExam exam = IbExam.valueOf(entry.getKey());
                    if (entry.getValue() >= exam.getMinScoreForCredit()) {
                        total += exam.getCreditHours();
                    }
                } catch (IllegalArgumentException ignored) { }
            }
        }

        // Dual credit: assume 3 hours each
        if (entity.dualCreditCoursesJson != null && !entity.dualCreditCoursesJson.equals("[]")) {
            Type listType = new TypeToken<java.util.List<String>>() {}.getType();
            java.util.List<String> dual = GSON.fromJson(entity.dualCreditCoursesJson, listType);
            if (dual != null) total += dual.size() * 3;
        }

        return total;
    }
}
