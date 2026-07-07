package edu.illinois.cs.cs124.ay2026.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import edu.illinois.cs.cs124.ay2026.project.data.MajorRepository;

public class NewPlanActivity extends AppCompatActivity {

    public static final String EXTRA_MAJOR_NAME = "major_name";

    private AutoCompleteTextView collegeDropdown;
    private AutoCompleteTextView majorDropdown;
    private View majorInputLayout;
    private List<String> colleges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_plan);

        ImageButton buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> finish());

        collegeDropdown  = findViewById(R.id.college_dropdown);
        majorDropdown    = findViewById(R.id.major_dropdown);
        majorInputLayout = findViewById(R.id.major_input_layout);
        MaterialButton buttonNext = findViewById(R.id.button_next);

        colleges = MajorRepository.colleges(this);
        ArrayAdapter<String> collegeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, colleges);
        collegeDropdown.setAdapter(collegeAdapter);

        collegeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String college = colleges.get(position);
            List<String> majorNames = MajorRepository.majorNamesByCollege(this, college);
            ArrayAdapter<String> majorAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, majorNames);
            majorDropdown.setAdapter(majorAdapter);
            majorDropdown.setText("", false);
            majorInputLayout.setVisibility(View.VISIBLE);
        });

        buttonNext.setOnClickListener(v -> onNextClicked());
    }

    private void onNextClicked() {
        String selectedMajor = majorDropdown.getText().toString().trim();
        if (selectedMajor.isEmpty()) {
            Toast.makeText(this, "Please select a major first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, QuestionnaireActivity.class);
        intent.putExtra(EXTRA_MAJOR_NAME, selectedMajor);
        startActivity(intent);
    }
}