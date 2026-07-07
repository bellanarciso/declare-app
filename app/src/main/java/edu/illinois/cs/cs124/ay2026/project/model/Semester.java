package edu.illinois.cs.cs124.ay2026.project.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Semester {

    private final int year;
    private final String term;
    private final List<Course> courses = new ArrayList<>();
    private boolean studyAbroad = false;

    public Semester(int year, String term) {
        this.year = year;
        this.term = term;
    }

    public void setStudyAbroad(boolean studyAbroad) {
        this.studyAbroad = studyAbroad;
    }

    public boolean isStudyAbroad() {
        return studyAbroad;
    }

    public int getYear() {
        return year;
    }

    public String getTerm() {
        return term;
    }

    public List<Course> getCourses() {
        return Collections.unmodifiableList(courses);
    }

    public void addCourse(Course course) {
        courses.add(course);
    }

    public int getTotalCredits() {
        int total = 0;
        for (Course course : courses) {
            total += course.getCreditHours();
        }
        return total;
    }

    /** Returns a human-readable name like "Fall - Year 1". */
    public String getDisplayName() {
        return term + " \u2014 Year " + year;
    }

    /**
     * Returns a display name with the real calendar year, e.g. "Fall 2024 - Year 1".
     *
     * For a Fall start: Fall Y -> startYear+(Y-1), Spring Y -> startYear+Y.
     * For a Spring start: both terms in year Y -> startYear+(Y-1).
     */
    public String getDisplayName(String startTerm, int startYear) {
        int calendarYear;
        if ("Spring".equals(term) && "Fall".equals(startTerm)) {
            calendarYear = startYear + year;
        } else {
            calendarYear = startYear + (year - 1);
        }
        String label = isStudyAbroad() ? term + " " + calendarYear + " \u2014 Study Abroad" : term + " " + calendarYear + " \u2014 Year " + year;
        return label;
    }

    public boolean isFall() {
        return "Fall".equals(term);
    }
}