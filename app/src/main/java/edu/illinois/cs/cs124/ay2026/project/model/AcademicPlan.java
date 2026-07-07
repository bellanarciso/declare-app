package edu.illinois.cs.cs124.ay2026.project.model;

import java.util.Collections;
import java.util.List;

public class AcademicPlan {

    private final String majorName;
    private final List<Semester> semesters;

    public AcademicPlan(String majorName, List<Semester> semesters) {
        this.majorName = majorName;
        this.semesters = Collections.unmodifiableList(semesters);
    }

    public String getMajorName() {
        return majorName;
    }

    public List<Semester> getSemesters() {
        return semesters;
    }

    public int getTotalCredits() {
        int total = 0;
        for (Semester semester : semesters) {
            total += semester.getTotalCredits();
        }
        return total;
    }
}