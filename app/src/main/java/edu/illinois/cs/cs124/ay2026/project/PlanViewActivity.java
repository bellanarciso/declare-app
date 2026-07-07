package edu.illinois.cs.cs124.ay2026.project;

import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.adapter.SemesterAdapter;
import edu.illinois.cs.cs124.ay2026.project.data.AppDatabase;
import edu.illinois.cs.cs124.ay2026.project.data.MajorRepository;
import edu.illinois.cs.cs124.ay2026.project.data.ProfileConverter;
import edu.illinois.cs.cs124.ay2026.project.data.SavedPlanEntity;
import edu.illinois.cs.cs124.ay2026.project.model.AcademicPlan;
import edu.illinois.cs.cs124.ay2026.project.model.Course;
import edu.illinois.cs.cs124.ay2026.project.model.Goal;
import edu.illinois.cs.cs124.ay2026.project.model.Major;
import edu.illinois.cs.cs124.ay2026.project.model.Semester;
import edu.illinois.cs.cs124.ay2026.project.model.UserProfile;

public class PlanViewActivity extends AppCompatActivity {

    public static final String EXTRA_SECONDARY_MAJOR_NAME = "secondary_major_name";
    public static final String EXTRA_PROFILE_SNAPSHOT     = "profile_snapshot";
    public static final String EXTRA_SAVED_PLAN_ID        = "saved_plan_id";

    private RecyclerView recyclerView;
    private CircularProgressIndicator progressIndicator;
    private ExtendedFloatingActionButton fabAddMajor;

    private Major primaryMajor;
    private Major secondaryMajor;
    private UserProfile profile;
    private String startTerm;
    private int startYear;

    /** -1 means this plan hasn't been saved yet. */
    private long savedPlanId = -1;

    private int scheduleMajorReqCap = 3;
    private int scheduleCreditCap   = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_view);

        MaterialToolbar toolbar    = findViewById(R.id.toolbar);
        recyclerView               = findViewById(R.id.recycler_semesters);
        progressIndicator          = findViewById(R.id.progress_indicator);
        fabAddMajor                = findViewById(R.id.fab_add_major);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String majorName          = getIntent().getStringExtra(NewPlanActivity.EXTRA_MAJOR_NAME);
        String secondaryMajorName = getIntent().getStringExtra(EXTRA_SECONDARY_MAJOR_NAME);
        String profileSnapshot    = getIntent().getStringExtra(EXTRA_PROFILE_SNAPSHOT);
        savedPlanId               = getIntent().getLongExtra(EXTRA_SAVED_PLAN_ID, -1);

        // Resolve the primary major (use concentration name if available, fall back to major name)
        primaryMajor = MajorRepository.findByName(this, majorName);
        if (primaryMajor == null) { finish(); return; }

        if (secondaryMajorName != null) {
            secondaryMajor = MajorRepository.findByName(this, secondaryMajorName);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fabAddMajor.setOnClickListener(v -> showAddMajorDialog());

        if (savedPlanId != -1) {
            // Opened from a saved plan - always read from DB to get the latest snapshot
            // (avoids stale in-memory data if interests were applied after the list was loaded)
            loadProfileFromSavedPlan(savedPlanId);
        } else if (profileSnapshot != null) {
            profile = ProfileConverter.fromSnapshot(profileSnapshot);
            onProfileReady();
        } else {
            // Opened via "New Plan" - load the live profile from Room
            loadProfileFromDatabase();
        }
    }

    private void loadProfileFromSavedPlan(long planId) {
        new Thread(() -> {
            SavedPlanEntity saved = AppDatabase.getInstance(this).savedPlanDao().getById(planId);
            String snapshot = saved != null ? saved.profileSnapshot : null;
            runOnUiThread(() -> {
                profile = ProfileConverter.fromSnapshot(snapshot);
                onProfileReady();
            });
        }).start();
    }

    /** Called on the background thread after the profile has been loaded. */
    private void loadProfileFromDatabase() {
        new Thread(() -> {
            UserProfile loaded = ProfileConverter.fromEntity(
                    AppDatabase.getInstance(this).userProfileDao().getProfile());
            runOnUiThread(() -> {
                profile = loaded;
                onProfileReady();
            });
        }).start();
    }

    /** Generates and displays the plan once the profile is available. */
    private void onProfileReady() {
        startTerm = (profile != null) ? profile.getStartTerm() : "Fall";
        startYear = (profile != null) ? profile.getStartYear()
                : java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        scheduleMajorReqCap = (profile != null && profile.hasGoal(Goal.GRADUATE_EARLY)) ? 4 : 3;
        scheduleCreditCap = 18;
        rebuildPlan();
    }

    // -------------------------------------------------------------------------
    // Toolbar menu
    // -------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_plan_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save_plan) {
            showSavePlanDialog();
            return true;
        }
        if (item.getItemId() == R.id.action_adjust_schedule) {
            showAdjustScheduleDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSavePlanDialog() {
        EditText nameField = new EditText(this);
        nameField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameField.setHint("e.g. CS + Math, Fall 2024");
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        nameField.setPadding(pad, pad, pad, pad);

        // Pre-fill with major name as a sensible default
        String suggestion = primaryMajor.getDisplayName();
        if (secondaryMajor != null) suggestion += " + " + secondaryMajor.getDisplayName();
        nameField.setText(suggestion);
        nameField.selectAll();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Save Plan")
                .setView(nameField)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameField.getText().toString().trim();
                    if (!name.isEmpty()) savePlan(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void savePlan(String name) {
        String snapshotJson = profile != null ? ProfileConverter.toSnapshot(profile) : "{}";
        String secName = secondaryMajor != null ? secondaryMajor.getDisplayName() : null;

        new Thread(() -> {
            SavedPlanEntity entity = new SavedPlanEntity();
            entity.name                = name;
            entity.majorName           = primaryMajor.getDisplayName();
            entity.secondaryMajorName  = secName;
            entity.profileSnapshot     = snapshotJson;
            entity.createdAt           = System.currentTimeMillis();

            long id = AppDatabase.getInstance(this).savedPlanDao().insert(entity);
            savedPlanId = id;

            runOnUiThread(() ->
                    Snackbar.make(recyclerView, "\"" + name + "\" saved!", Snackbar.LENGTH_SHORT).show());
        }).start();
    }

    private void showAdjustScheduleDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_adjust_schedule, null);
        MaterialButtonToggleGroup togglePacing = dialogView.findViewById(R.id.toggle_pacing);
        MaterialButtonToggleGroup toggleLoad   = dialogView.findViewById(R.id.toggle_load);

        switch (scheduleMajorReqCap) {
            case 2:  togglePacing.check(R.id.btn_pacing_relaxed); break;
            case 4:  togglePacing.check(R.id.btn_pacing_fast);    break;
            default: togglePacing.check(R.id.btn_pacing_normal);  break;
        }
        switch (scheduleCreditCap) {
            case 15: toggleLoad.check(R.id.btn_load_light);    break;
            case 20: toggleLoad.check(R.id.btn_load_heavy);    break;
            default: toggleLoad.check(R.id.btn_load_standard); break;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Adjust Schedule")
                .setView(dialogView)
                .setPositiveButton("Regenerate", (d, w) -> {
                    int pacing = togglePacing.getCheckedButtonId();
                    if (pacing == R.id.btn_pacing_relaxed)      scheduleMajorReqCap = 2;
                    else if (pacing == R.id.btn_pacing_fast)    scheduleMajorReqCap = 4;
                    else                                         scheduleMajorReqCap = 3;

                    int load = toggleLoad.getCheckedButtonId();
                    if (load == R.id.btn_load_light)             scheduleCreditCap = 15;
                    else if (load == R.id.btn_load_heavy)        scheduleCreditCap = 20;
                    else                                         scheduleCreditCap = 18;

                    rebuildPlan();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // -------------------------------------------------------------------------
    // Plan generation & display
    // -------------------------------------------------------------------------

    private void rebuildPlan() {
        final Major planMajor;
        final Set<String> secondaryIds;
        final Set<String> sharedElectives;

        if (secondaryMajor != null) {
            planMajor       = PlanGenerator.mergeMajors(primaryMajor, secondaryMajor);
            secondaryIds    = PlanGenerator.secondaryOnlyIds(primaryMajor, secondaryMajor);
            sharedElectives = PlanGenerator.sharedElectiveIds(primaryMajor, secondaryMajor);
        } else {
            planMajor       = primaryMajor;
            secondaryIds    = Collections.emptySet();
            sharedElectives = Collections.emptySet();
        }

        updateToolbarTitle();
        progressIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        new Thread(() -> {
            AcademicPlan plan = PlanGenerator.generate(planMajor, profile,
                    new PlanGenerator.PlanOptions(scheduleMajorReqCap, scheduleCreditCap));
            Map<String, Course> courseMap = new HashMap<>();
            for (Course course : planMajor.getCourses()) courseMap.put(course.getId(), course);

            List<Semester> displaySemesters = new ArrayList<>(plan.getSemesters());
            boolean trimmed = false;
            while (!displaySemesters.isEmpty()
                    && displaySemesters.get(displaySemesters.size() - 1).getCourses().isEmpty()) {
                displaySemesters.remove(displaySemesters.size() - 1);
                trimmed = true;
            }

            String graduationLabel = null;
            if (trimmed && profile != null && profile.hasGoal(Goal.GRADUATE_EARLY)
                    && !displaySemesters.isEmpty()) {
                Semester last = displaySemesters.get(displaySemesters.size() - 1);
                String label = last.getDisplayName(startTerm, startYear);
                // Convert "Fall 2026" -> "December 2026", "Spring 2027" -> "May 2027"
                if (label.startsWith("Fall")) {
                    graduationLabel = "December" + label.substring(4);
                } else {
                    graduationLabel = "May" + label.substring(6);
                }
            }

            final String finalLabel = graduationLabel;
            runOnUiThread(() -> {
                progressIndicator.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(new SemesterAdapter(
                        displaySemesters, courseMap, startTerm, startYear,
                        secondaryIds, sharedElectives, finalLabel));
            });
        }).start();
    }

    private void updateToolbarTitle() {
        if (getSupportActionBar() == null) return;
        if (secondaryMajor != null) {
            getSupportActionBar().setTitle(
                    primaryMajor.getDisplayName() + " + " + secondaryMajor.getDisplayName());
        } else {
            getSupportActionBar().setTitle(primaryMajor.getDisplayName());
        }
        getSupportActionBar().setSubtitle(startTerm + " " + startYear + " - 4-Year Plan");
    }

    // -------------------------------------------------------------------------
    // Add / remove second major
    // -------------------------------------------------------------------------

    private void showAddMajorDialog() {
        List<String> colleges = MajorRepository.colleges(this);
        String[] options = colleges.toArray(new String[0]);
        int[] selected = {-1};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select a College")
                .setSingleChoiceItems(options, -1, (d, which) -> selected[0] = which)
                .setPositiveButton("Next", (d, which) -> {
                    if (selected[0] >= 0) showSecondaryMajorDialog(colleges.get(selected[0]));
                })
                .setNeutralButton("Remove Second Major", (d, which) -> {
                    secondaryMajor = null;
                    fabAddMajor.setText(R.string.fab_add_major);
                    rebuildPlan();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSecondaryMajorDialog(String college) {
        List<String> majorNames = MajorRepository.majorNamesByCollege(this, college);
        majorNames.remove(primaryMajor.getDisplayName());
        String[] options = majorNames.toArray(new String[0]);
        int current = secondaryMajor != null ? majorNames.indexOf(secondaryMajor.getDisplayName()) : -1;
        int[] selected = {current};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select a Major")
                .setSingleChoiceItems(options, current, (d, which) -> selected[0] = which)
                .setPositiveButton("Apply", (d, which) -> {
                    if (selected[0] >= 0) {
                        secondaryMajor = MajorRepository.findByName(this, majorNames.get(selected[0]));
                        if (secondaryMajor != null) {
                            fabAddMajor.setText(secondaryMajor.getDisplayName());
                            rebuildPlan();
                        }
                    }
                })
                .setNegativeButton("Back", (d, which) -> showAddMajorDialog())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload when returning to this activity so changes made elsewhere (e.g. interests
        // applied from the Profile tab) are reflected without requiring the user to close
        // and re-open the plan. The profile != null guard skips the duplicate load that
        // would otherwise happen immediately after onCreate.
        if (savedPlanId != -1 && profile != null) {
            loadProfileFromSavedPlan(savedPlanId);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
