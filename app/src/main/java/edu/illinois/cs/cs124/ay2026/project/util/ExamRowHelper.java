package edu.illinois.cs.cs124.ay2026.project.util;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Map;

/**
 * Builds AP/IB exam rows for both QuestionnaireActivity and CreditsFragment.
 *
 * Each row is a horizontal LinearLayout containing:
 *  - A CheckBox labeled with the exam name
 *  - A score Spinner (hidden until the checkbox is checked)
 *
 * Call addExamRow() once per exam and pass in the shared state maps;
 * the method populates them for later collection by the caller.
 */
public final class ExamRowHelper {

    private ExamRowHelper() { }

    /**
     * Creates and adds a single exam row to {@code container}.
     *
     * @param context      activity or fragment context
     * @param displayName  exam name shown on the checkbox
     * @param defaultScore score pre-selected when the checkbox is first checked
     * @param minScore     lowest valid score (1 for AP, 1 for IB)
     * @param maxScore     highest valid score (5 for AP, 7 for IB)
     * @param container    LinearLayout to add the row into
     * @param examKey      enum value used as the key in the maps below
     * @param checkboxMap  caller-owned map: exam -> CheckBox (to read checked state later)
     * @param scoreMap     caller-owned map: exam -> int[1] mutable score holder
     */
    public static <E extends Enum<E>> void addExamRow(
            Context context,
            String displayName,
            int defaultScore,
            int minScore,
            int maxScore,
            LinearLayout container,
            E examKey,
            Map<E, CheckBox> checkboxMap,
            Map<E, int[]> scoreMap) {

        int[] scoreState = {defaultScore};
        scoreMap.put(examKey, scoreState);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dp(context, 2), 0, dp(context, 2));
        row.setLayoutParams(rowParams);

        // Checkbox (takes remaining width)
        CheckBox cb = new CheckBox(context);
        LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cb.setLayoutParams(cbParams);
        cb.setText(displayName);
        cb.setTextSize(14f);
        checkboxMap.put(examKey, cb);

        // Score selector (hidden until checkbox is checked)
        LinearLayout scoreLayout = new LinearLayout(context);
        scoreLayout.setOrientation(LinearLayout.HORIZONTAL);
        scoreLayout.setGravity(Gravity.CENTER_VERTICAL);
        scoreLayout.setVisibility(View.GONE);
        scoreLayout.setPadding(dp(context, 4), 0, 0, 0);

        TextView scoreLabel = new TextView(context);
        scoreLabel.setText("Score:");
        scoreLabel.setTextSize(13f);
        scoreLabel.setPadding(0, 0, dp(context, 4), 0);

        String[] options = new String[maxScore - minScore + 1];
        for (int i = 0; i < options.length; i++) {
            options[i] = String.valueOf(minScore + i);
        }

        Spinner scoreSpinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scoreSpinner.setAdapter(adapter);
        scoreSpinner.setSelection(defaultScore - minScore);
        scoreSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                scoreState[0] = minScore + position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        scoreLayout.addView(scoreLabel);
        scoreLayout.addView(scoreSpinner);

        cb.setOnCheckedChangeListener((btn, checked) ->
                scoreLayout.setVisibility(checked ? View.VISIBLE : View.GONE));

        row.addView(cb);
        row.addView(scoreLayout);
        container.addView(row);
    }

    /**
     * Pre-selects a checkbox and sets the spinner to the given score.
     * Call this after adding all rows when loading saved data.
     */
    public static <E extends Enum<E>> void restoreExamRow(
            E examKey,
            int savedScore,
            Map<E, CheckBox> checkboxMap,
            Map<E, int[]> scoreMap) {
        CheckBox cb = checkboxMap.get(examKey);
        int[] state = scoreMap.get(examKey);
        if (cb != null && state != null) {
            state[0] = savedScore;
            cb.setChecked(true); // triggers the score layout visibility listener
        }
    }

    private static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
