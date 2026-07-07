package edu.illinois.cs.cs124.ay2026.project.model;

import java.util.Collections;
import java.util.List;

public class Course {

    private final String id;
    private final String name;
    private final int creditHours;
    private final List<String> prerequisites;
    private final CourseType type;
    private final boolean offeredFall;
    private final boolean offeredSpring;
    private final int minYear;
    private final boolean cpaTrack;
    /** Suggested academic year from the catalog's sample 4-year sequence (1-4). 0 = no suggestion. */
    private final int suggestedYear;
    /** Suggested semester from the catalog's sample sequence ("Fall", "Spring"). Null = no suggestion. */
    private final String suggestedSemester;
    /** Display label for elective/gen-ed courses showing which requirement this fulfills. Null for required courses. */
    private final String groupLabel;

    public Course(
            String id,
            String name,
            int creditHours,
            List<String> prerequisites,
            CourseType type,
            boolean offeredFall,
            boolean offeredSpring,
            int minYear,
            boolean cpaTrack,
            int suggestedYear,
            String suggestedSemester) {
        this(id, name, creditHours, prerequisites, type, offeredFall, offeredSpring,
                minYear, cpaTrack, suggestedYear, suggestedSemester, null);
    }

    public Course(
            String id,
            String name,
            int creditHours,
            List<String> prerequisites,
            CourseType type,
            boolean offeredFall,
            boolean offeredSpring,
            int minYear,
            boolean cpaTrack,
            int suggestedYear,
            String suggestedSemester,
            String groupLabel) {
        this.id = id;
        this.name = name;
        this.creditHours = creditHours;
        this.prerequisites = Collections.unmodifiableList(prerequisites);
        this.type = type;
        this.offeredFall = offeredFall;
        this.offeredSpring = offeredSpring;
        this.minYear = minYear;
        this.cpaTrack = cpaTrack;
        this.suggestedYear = suggestedYear;
        this.suggestedSemester = suggestedSemester;
        this.groupLabel = groupLabel;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCreditHours() {
        return creditHours;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public CourseType getType() {
        return type;
    }

    public boolean isOfferedFall() {
        return offeredFall;
    }

    public boolean isOfferedSpring() {
        return offeredSpring;
    }

    /** Minimum academic year (1=freshman, 2=sophomore, 3=junior, 4=senior). 0 means no requirement. */
    public int getMinYear() {
        return minYear;
    }

    /** True if this course is only required for students pursuing the CPA track. */
    public boolean isCpaTrack() {
        return cpaTrack;
    }

    /** Label shown in the plan view indicating which requirement this elective/gen-ed fulfills. Null for required courses. */
    public String getGroupLabel() {
        return groupLabel;
    }

    /** Suggested year (1-4) from the catalog's sample sequence. 0 if not specified. */
    public int getSuggestedYear() {
        return suggestedYear;
    }

    /** Suggested semester ("Fall" or "Spring") from the catalog's sample sequence. Null if not specified. */
    public String getSuggestedSemester() {
        return suggestedSemester;
    }
}