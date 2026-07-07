package edu.illinois.cs.cs124.ay2026.project;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.data.GenEdCoursePool;
import edu.illinois.cs.cs124.ay2026.project.model.AcademicPlan;
import edu.illinois.cs.cs124.ay2026.project.model.Course;
import edu.illinois.cs.cs124.ay2026.project.model.CourseType;
import edu.illinois.cs.cs124.ay2026.project.model.Major;
import edu.illinois.cs.cs124.ay2026.project.model.RequirementGroup;
import edu.illinois.cs.cs124.ay2026.project.model.Semester;
import edu.illinois.cs.cs124.ay2026.project.model.UserProfile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that majors with empty gen-ed requirement groups (like Finance) still
 * get gen-ed courses scheduled via GenEdCoursePool, and that interest scoring
 * influences which courses are chosen.
 *
 * INVARIANT: These tests must pass after any change to PlanGenerator or GenEdCoursePool.
 */
public class GenEdCoverageTest {

    /** A Finance-like major: required courses plus empty gen-ed requirement groups. */
    private static Major buildFinanceLikeMajor() {
        List<Course> required = Arrays.asList(
                course("BUS 101", "Professional Responsibility", 3),
                course("ECON 102", "Microeconomics", 3),
                course("ECON 103", "Macroeconomics", 3),
                course("FIN 221", "Corporate Finance", 3),
                course("ACCY 201", "Accounting I", 3),
                course("BADM 210", "Business Statistics", 3)
        );

        List<RequirementGroup> genEdGroups = Arrays.asList(
                group("humanities_and_arts",             "Humanities & Arts",        6, 0),
                group("natural_sciences_and_technology", "Natural Sciences",         6, 0),
                group("cultural_studies_non_western",    "Cultural Studies (NW)",    3, 0),
                group("cultural_studies_us_minority",    "Cultural Studies (US)",    3, 0),
                group("cultural_studies_western_comparative", "Cultural Studies (WCC)", 3, 0),
                group("language_other_than_english",     "Language",                 0, 1)
        );

        return new Major("Finance", "Gies College of Business", required, genEdGroups,
                Collections.emptyList());
    }

    @Test
    public void emptyGenEdGroupsGetCoursesFromPool() {
        Major major = buildFinanceLikeMajor();
        AcademicPlan plan = PlanGenerator.generate(major);
        Set<String> scheduled = scheduledIds(plan);

        for (RequirementGroup group : major.getRequirementGroups()) {
            Set<String> poolIds = poolIdsFor(group.getId());
            assertFalse(
                    "Pool is empty for gen-ed group: " + group.getId(),
                    poolIds.isEmpty());
            assertTrue(
                    "No gen-ed courses scheduled for group '" + group.getId()
                            + "'. Pool has: " + poolIds,
                    !Collections.disjoint(scheduled, poolIds));
        }
    }

    @Test
    public void interestsSteersNaturalSciencesGenEd() {
        Major major = buildFinanceLikeMajor();

        Set<String> astroInterest = new HashSet<>(
                Collections.singletonList("Astronomy & Space Science"));
        UserProfile profile = new UserProfile("Fall", 2025,
                Collections.emptySet(), astroInterest,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyList(), null, false);

        AcademicPlan plan = PlanGenerator.generate(major, profile);
        Set<String> scheduled = scheduledIds(plan);

        // ASTR 100 scores highest for "Astronomy & Space Science" - should be preferred
        // over generic life/physical sciences options.
        assertTrue("ASTR 100 should be scheduled when Astronomy interest is set",
                scheduled.contains("ASTR 100"));
    }

    @Test
    public void interestsSteersCulturalStudiesGenEd() {
        Major major = buildFinanceLikeMajor();

        Set<String> eastAsiaInterest = new HashSet<>(
                Collections.singletonList("East Asian Cultures"));
        UserProfile profile = new UserProfile("Fall", 2025,
                Collections.emptySet(), eastAsiaInterest,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyList(), null, false);

        AcademicPlan plan = PlanGenerator.generate(major, profile);
        Set<String> scheduled = scheduledIds(plan);

        // EALC 120 scores highest for "East Asian Cultures" in the NW cultural studies group.
        assertTrue("EALC 120 should be scheduled when East Asian Cultures interest is set",
                scheduled.contains("EALC 120"));
    }

    @Test
    public void noSemesterExceedsCreditLimitWithGenEds() {
        Major major = buildFinanceLikeMajor();
        AcademicPlan plan = PlanGenerator.generate(major);

        for (Semester semester : plan.getSemesters()) {
            assertTrue(
                    "Semester " + semester.getDisplayName() + " exceeds 18 credits: "
                            + semester.getTotalCredits(),
                    semester.getTotalCredits() <= 18);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Set<String> scheduledIds(AcademicPlan plan) {
        Set<String> ids = new HashSet<>();
        for (Semester semester : plan.getSemesters()) {
            for (Course course : semester.getCourses()) {
                ids.add(course.getId());
            }
        }
        return ids;
    }

    private static Set<String> poolIdsFor(String groupId) {
        Set<String> ids = new HashSet<>();
        for (Course c : GenEdCoursePool.optionsFor(groupId)) {
            ids.add(c.getId());
        }
        return ids;
    }

    private static Course course(String id, String name, int credits) {
        return new Course(id, name, credits, Collections.emptyList(),
                CourseType.MAJOR_REQUIREMENT, true, true, 0, false, 0, null);
    }

    private static RequirementGroup group(String id, String description,
            int requiredCredits, int requiredCount) {
        return new RequirementGroup(id, description, requiredCredits, requiredCount, 0,
                Collections.emptyList());
    }
}