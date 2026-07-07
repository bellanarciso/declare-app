package edu.illinois.cs.cs124.ay2026.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.data.AppDatabase;
import edu.illinois.cs.cs124.ay2026.project.data.MajorRepository;
import edu.illinois.cs.cs124.ay2026.project.data.ProfileConverter;
import edu.illinois.cs.cs124.ay2026.project.data.SavedPlanEntity;
import edu.illinois.cs.cs124.ay2026.project.model.AcademicPlan;
import edu.illinois.cs.cs124.ay2026.project.model.Course;
import edu.illinois.cs.cs124.ay2026.project.model.CourseType;
import edu.illinois.cs.cs124.ay2026.project.model.Major;
import edu.illinois.cs.cs124.ay2026.project.model.Semester;
import edu.illinois.cs.cs124.ay2026.project.model.UserProfile;

public class CompareActivity extends AppCompatActivity {

    public static final String EXTRA_PLAN_A_ID = "plan_a_id";
    public static final String EXTRA_PLAN_B_ID = "plan_b_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        long planAId = getIntent().getLongExtra(EXTRA_PLAN_A_ID, -1);
        long planBId = getIntent().getLongExtra(EXTRA_PLAN_B_ID, -1);

        if (planAId == -1 || planBId == -1) {
            finish();
            return;
        }

        loadAndCompare(planAId, planBId);
    }

    private void loadAndCompare(long planAId, long planBId) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            SavedPlanEntity entityA = db.savedPlanDao().getById(planAId);
            SavedPlanEntity entityB = db.savedPlanDao().getById(planBId);

            if (entityA == null || entityB == null) {
                runOnUiThread(this::finish);
                return;
            }

            Major majorA = resolveMajor(entityA);
            Major majorB = resolveMajor(entityB);

            if (majorA == null || majorB == null) {
                runOnUiThread(this::finish);
                return;
            }

            UserProfile profileA = ProfileConverter.fromSnapshot(entityA.profileSnapshot);
            UserProfile profileB = ProfileConverter.fromSnapshot(entityB.profileSnapshot);

            AcademicPlan planA = PlanGenerator.generate(majorA, profileA);
            AcademicPlan planB = PlanGenerator.generate(majorB, profileB);

            runOnUiThread(() -> showComparison(entityA, entityB, planA, planB));
        }).start();
    }

    private Major resolveMajor(SavedPlanEntity entity) {
        if (entity.secondaryMajorName != null) {
            Major primary   = MajorRepository.findByName(this, entity.majorName);
            Major secondary = MajorRepository.findByName(this, entity.secondaryMajorName);
            if (primary != null && secondary != null) {
                return PlanGenerator.mergeMajors(primary, secondary);
            }
            return primary;
        }
        return MajorRepository.findByName(this, entity.majorName);
    }

    private void showComparison(SavedPlanEntity entityA, SavedPlanEntity entityB,
                                AcademicPlan planA, AcademicPlan planB) {
        // Plan name cards
        TextView nameA  = findViewById(R.id.text_plan_a_name);
        TextView majorA = findViewById(R.id.text_plan_a_major);
        TextView nameB  = findViewById(R.id.text_plan_b_name);
        TextView majorB = findViewById(R.id.text_plan_b_major);

        nameA.setText(entityA.name);
        majorA.setText(majorLabel(entityA));
        nameB.setText(entityB.name);
        majorB.setText(majorLabel(entityB));

        // Stats
        bindStat(R.id.text_a_credits,   String.valueOf(planA.getTotalCredits()));
        bindStat(R.id.text_b_credits,   String.valueOf(planB.getTotalCredits()));
        bindStat(R.id.text_a_semesters, String.valueOf(nonEmptySemesters(planA)));
        bindStat(R.id.text_b_semesters, String.valueOf(nonEmptySemesters(planB)));
        bindStat(R.id.text_a_required,  String.valueOf(countType(planA, CourseType.MAJOR_REQUIREMENT)));
        bindStat(R.id.text_b_required,  String.valueOf(countType(planB, CourseType.MAJOR_REQUIREMENT)));
        bindStat(R.id.text_a_geneds,    String.valueOf(countType(planA, CourseType.GEN_ED)));
        bindStat(R.id.text_b_geneds,    String.valueOf(countType(planB, CourseType.GEN_ED)));

        // Highlight winner for total credits (lower = better) and semesters (lower = better)
        highlightLower(R.id.text_a_credits, R.id.text_b_credits,
                planA.getTotalCredits(), planB.getTotalCredits());
        highlightLower(R.id.text_a_semesters, R.id.text_b_semesters,
                nonEmptySemesters(planA), nonEmptySemesters(planB));

        // Course overlap
        Set<String> idsA = courseIds(planA);
        Set<String> idsB = courseIds(planB);
        Set<String> shared = new HashSet<>(idsA);
        shared.retainAll(idsB);

        TextView overlapSummary = findViewById(R.id.text_overlap_summary);
        TextView overlapCourses = findViewById(R.id.text_overlap_courses);

        if (shared.isEmpty()) {
            overlapSummary.setText("No courses in common between these two plans.");
            overlapCourses.setVisibility(View.GONE);
        } else {
            int pct = Math.round(100f * shared.size() / Math.max(idsA.size(), idsB.size()));
            overlapSummary.setText(shared.size() + " course" + (shared.size() == 1 ? "" : "s")
                    + " in common (" + pct + "% overlap)");
            overlapCourses.setText(String.join("\n", new ArrayList<>(shared)));
            overlapCourses.setVisibility(View.VISIBLE);
        }

        // Semester breakdown
        buildSemesterBreakdown(planA, planB);

        // Swap progress for content
        findViewById(R.id.progress_indicator).setVisibility(View.GONE);
        findViewById(R.id.scroll_content).setVisibility(View.VISIBLE);
    }

    private void buildSemesterBreakdown(AcademicPlan planA, AcademicPlan planB) {
        LinearLayout container = findViewById(R.id.semester_breakdown_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        List<Semester> semestersA = planA.getSemesters();
        List<Semester> semestersB = planB.getSemesters();
        int rows = Math.max(semestersA.size(), semestersB.size());

        for (int i = 0; i < rows; i++) {
            Semester semA = i < semestersA.size() ? semestersA.get(i) : null;
            Semester semB = i < semestersB.size() ? semestersB.get(i) : null;

            // Skip rows where both semesters are empty
            boolean aEmpty = semA == null || semA.getCourses().isEmpty();
            boolean bEmpty = semB == null || semB.getCourses().isEmpty();
            if (aEmpty && bEmpty) continue;

            View row = inflater.inflate(R.layout.item_compare_semester, container, false);

            String label = semA != null ? semA.getDisplayName() : (semB != null ? semB.getDisplayName() : "Semester " + (i + 1));
            ((TextView) row.findViewById(R.id.text_semester_label)).setText(label);

            LinearLayout colA = row.findViewById(R.id.container_a);
            LinearLayout colB = row.findViewById(R.id.container_b);

            fillColumn(colA, semA, inflater);
            fillColumn(colB, semB, inflater);

            container.addView(row);
        }
    }

    private void fillColumn(LinearLayout col, Semester semester, LayoutInflater inflater) {
        if (semester == null || semester.getCourses().isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("-");
            empty.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
            empty.setTextColor(getResources().getColor(R.color.declared_sage, null));
            col.addView(empty);
            return;
        }
        for (Course course : semester.getCourses()) {
            View item = inflater.inflate(R.layout.item_course, col, false);
            ((TextView) item.findViewById(R.id.text_course_id)).setText(course.getId());
            ((TextView) item.findViewById(R.id.text_course_name)).setText(course.getName());
            TextView credits = item.findViewById(R.id.text_credit_hours);
            credits.setText(course.getCreditHours() + " cr");

            // Color indicator
            int indicatorColor;
            if (course.getType() == CourseType.MAJOR_REQUIREMENT) {
                indicatorColor = getResources().getColor(R.color.color_major_req, null);
            } else if (course.getType() == CourseType.GEN_ED) {
                indicatorColor = getResources().getColor(R.color.color_gen_ed, null);
            } else {
                indicatorColor = getResources().getColor(R.color.color_elective, null);
            }
            item.findViewById(R.id.course_type_indicator).setBackgroundColor(indicatorColor);

            col.addView(item);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void bindStat(int viewId, String value) {
        ((TextView) findViewById(viewId)).setText(value);
    }

    private void highlightLower(int viewIdA, int viewIdB, int valA, int valB) {
        int winColor = getResources().getColor(R.color.color_gen_ed, null);
        if (valA < valB) {
            ((TextView) findViewById(viewIdA)).setTextColor(winColor);
        } else if (valB < valA) {
            ((TextView) findViewById(viewIdB)).setTextColor(winColor);
        }
        // equal - both stay default
    }

    private static String majorLabel(SavedPlanEntity entity) {
        return entity.secondaryMajorName != null
                ? entity.majorName + " + " + entity.secondaryMajorName
                : entity.majorName;
    }

    private static int nonEmptySemesters(AcademicPlan plan) {
        int count = 0;
        for (Semester s : plan.getSemesters()) {
            if (!s.getCourses().isEmpty()) count++;
        }
        return count;
    }

    private static int countType(AcademicPlan plan, CourseType type) {
        int count = 0;
        for (Semester s : plan.getSemesters()) {
            for (Course c : s.getCourses()) {
                if (c.getType() == type) count++;
            }
        }
        return count;
    }

    private static Set<String> courseIds(AcademicPlan plan) {
        Set<String> ids = new HashSet<>();
        for (Semester s : plan.getSemesters()) {
            for (Course c : s.getCourses()) {
                ids.add(c.getId());
            }
        }
        return ids;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}