package edu.illinois.cs.cs124.ay2026.project.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.R;
import edu.illinois.cs.cs124.ay2026.project.model.Course;
import edu.illinois.cs.cs124.ay2026.project.model.CourseType;
import edu.illinois.cs.cs124.ay2026.project.model.Semester;

public class SemesterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SEMESTER   = 0;
    private static final int VIEW_TYPE_GRADUATION = 1;

    private final List<Semester> semesters;
    private final Map<String, Course> allCourses;
    private final String startTerm;
    private final int startYear;
    private final Set<String> secondaryMajorIds;
    private final Set<String> sharedElectiveIds;
    private final String graduationLabel;

    public SemesterAdapter(List<Semester> semesters, Map<String, Course> allCourses,
                           String startTerm, int startYear, Set<String> secondaryMajorIds,
                           Set<String> sharedElectiveIds, String graduationLabel) {
        this.semesters = semesters;
        this.allCourses = allCourses;
        this.startTerm = startTerm;
        this.startYear = startYear;
        this.secondaryMajorIds = secondaryMajorIds;
        this.sharedElectiveIds = sharedElectiveIds;
        this.graduationLabel = graduationLabel;
    }

    @Override
    public int getItemViewType(int position) {
        return (graduationLabel != null && position == semesters.size())
                ? VIEW_TYPE_GRADUATION : VIEW_TYPE_SEMESTER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_GRADUATION) {
            View view = inflater.inflate(R.layout.item_graduation_banner, parent, false);
            return new GraduationViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_semester, parent, false);
        return new SemesterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof GraduationViewHolder) {
            ((GraduationViewHolder) holder).graduationDate.setText(graduationLabel);
            return;
        }

        SemesterViewHolder semHolder = (SemesterViewHolder) holder;
        Semester semester = semesters.get(position);

        semHolder.semesterName.setText(semester.getDisplayName(startTerm, startYear));
        semHolder.creditCount.setText(semester.getTotalCredits() + " cr");

        semHolder.courseContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        for (Course course : semester.getCourses()) {
            View courseView = inflater.inflate(R.layout.item_course, semHolder.courseContainer, false);
            bindCourseView(courseView, course);
            semHolder.courseContainer.addView(courseView);
        }
    }

    @Override
    public int getItemCount() {
        return semesters.size() + (graduationLabel != null ? 1 : 0);
    }

    private void bindCourseView(View view, Course course) {
        TextView courseId = view.findViewById(R.id.text_course_id);
        TextView courseName = view.findViewById(R.id.text_course_name);
        TextView creditHours = view.findViewById(R.id.text_credit_hours);
        View typeIndicator = view.findViewById(R.id.course_type_indicator);

        courseId.setText(course.getId());
        courseName.setText(course.getName());
        creditHours.setText(course.getCreditHours() + " cr");
        typeIndicator.setBackgroundColor(colorForType(view, course.getType()));

        android.widget.TextView groupLabel = view.findViewById(R.id.text_group_label);
        String label = course.getGroupLabel();
        if (label != null && !label.isEmpty()) {
            groupLabel.setText(label);
            groupLabel.setVisibility(android.view.View.VISIBLE);
        } else if (course.getType() == CourseType.GEN_ED) {
            groupLabel.setText("General Education");
            groupLabel.setVisibility(android.view.View.VISIBLE);
        } else if (course.getType() == CourseType.ELECTIVE) {
            groupLabel.setText("Elective");
            groupLabel.setVisibility(android.view.View.VISIBLE);
        } else {
            groupLabel.setVisibility(android.view.View.GONE);
        }

        android.widget.TextView badge = view.findViewById(R.id.text_secondary_badge);
        badge.setVisibility(secondaryMajorIds.contains(course.getId())
                ? android.view.View.VISIBLE : android.view.View.GONE);

        android.widget.TextView sharedBadge = view.findViewById(R.id.text_shared_badge);
        sharedBadge.setVisibility(sharedElectiveIds.contains(course.getId())
                ? android.view.View.VISIBLE : android.view.View.GONE);

        view.setOnClickListener(v -> showPrereqChain(v, course));
    }

    private void showPrereqChain(View anchor, Course course) {
        List<Course> chain = buildPrereqChain(course);

        View dialogView = LayoutInflater.from(anchor.getContext())
                .inflate(R.layout.dialog_prereq_chain, null);
        LinearLayout container = dialogView.findViewById(R.id.chain_container);

        if (chain.size() == 1) {
            TextView noPrereqs = new TextView(anchor.getContext());
            noPrereqs.setText("No prerequisites.");
            noPrereqs.setTextSize(15);
            noPrereqs.setPadding(32, 16, 32, 16);
            container.addView(noPrereqs);
        } else {
            LayoutInflater inflater = LayoutInflater.from(anchor.getContext());
            for (int i = 0; i < chain.size(); i++) {
                Course c = chain.get(i);
                boolean isSelected = c.getId().equals(course.getId());

                View courseView = inflater.inflate(R.layout.item_course, container, false);
                courseView.setClickable(false);
                courseView.setFocusable(false);

                TextView courseId = courseView.findViewById(R.id.text_course_id);
                TextView courseName = courseView.findViewById(R.id.text_course_name);
                TextView creditHours = courseView.findViewById(R.id.text_credit_hours);
                View typeIndicator = courseView.findViewById(R.id.course_type_indicator);

                courseId.setText(c.getId());
                courseName.setText(c.getName());
                creditHours.setText(c.getCreditHours() + " cr");
                typeIndicator.setBackgroundColor(colorForType(courseView, c.getType()));
                courseView.findViewById(R.id.text_group_label).setVisibility(android.view.View.GONE);

                if (isSelected) {
                    courseId.setTextColor(
                            anchor.getContext().getResources().getColor(R.color.declared_butter, null));
                }

                container.addView(courseView);

                if (i < chain.size() - 1) {
                    TextView arrow = new TextView(anchor.getContext());
                    arrow.setText("↓");
                    arrow.setTextSize(16);
                    arrow.setGravity(Gravity.CENTER);
                    arrow.setPadding(0, 0, 0, 0);
                    container.addView(arrow);
                }
            }
        }

        new MaterialAlertDialogBuilder(anchor.getContext())
                .setTitle("Prerequisite Chain")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    /**
     * Returns all courses in the prerequisite chain leading to this course,
     * in topological order (earliest prerequisites first, selected course last).
     */
    private List<Course> buildPrereqChain(Course course) {
        Set<String> ordered = new LinkedHashSet<>();
        collectAncestors(course.getId(), ordered);
        List<Course> chain = new ArrayList<>();
        for (String id : ordered) {
            Course c = allCourses.get(id);
            if (c != null) {
                chain.add(c);
            }
        }
        return chain;
    }

    /**
     * Post-order DFS: adds a course to the set only after all its prerequisites
     * have been added, producing a valid topological ordering.
     */
    private void collectAncestors(String courseId, Set<String> visited) {
        if (visited.contains(courseId)) {
            return;
        }
        Course course = allCourses.get(courseId);
        if (course == null) {
            return;
        }
        for (String prereqId : course.getPrerequisites()) {
            collectAncestors(prereqId, visited);
        }
        visited.add(courseId);
    }

    private int colorForType(View view, CourseType type) {
        switch (type) {
            case MAJOR_REQUIREMENT:
                return view.getContext().getResources().getColor(R.color.color_major_req, null);
            case GEN_ED:
                return view.getContext().getResources().getColor(R.color.color_gen_ed, null);
            case ELECTIVE:
                return view.getContext().getResources().getColor(R.color.color_elective, null);
            default:
                return view.getContext().getResources().getColor(R.color.color_major_req, null);
        }
    }

    static class GraduationViewHolder extends RecyclerView.ViewHolder {
        final TextView graduationDate;

        GraduationViewHolder(@NonNull View itemView) {
            super(itemView);
            graduationDate = itemView.findViewById(R.id.text_graduation_date);
        }
    }

    static class SemesterViewHolder extends RecyclerView.ViewHolder {
        final TextView semesterName;
        final TextView creditCount;
        final LinearLayout courseContainer;

        SemesterViewHolder(@NonNull View itemView) {
            super(itemView);
            semesterName = itemView.findViewById(R.id.text_semester_name);
            creditCount = itemView.findViewById(R.id.text_credit_count);
            courseContainer = itemView.findViewById(R.id.course_container);
        }
    }
}