package edu.illinois.cs.cs124.ay2026.project;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.model.AcademicPlan;
import edu.illinois.cs.cs124.ay2026.project.model.Course;
import edu.illinois.cs.cs124.ay2026.project.model.CourseType;
import edu.illinois.cs.cs124.ay2026.project.model.Major;
import edu.illinois.cs.cs124.ay2026.project.model.Semester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlanGeneratorTest {

    /**
     * Minimal CS-like major: a small prerequisite chain plus a gen-ed and an elective,
     * enough to exercise the core scheduling logic without needing Android context.
     */
    private static Major buildTestMajor() {
        List<Course> courses = Arrays.asList(
                req("CS 101",  "Intro Computing",         3, Collections.emptyList(),          true, true),
                req("MATH 220","Calculus I",               4, Collections.emptyList(),          true, true),
                req("CS 225",  "Data Structures",          3, Arrays.asList("CS 101","MATH 220"), true, true),
                req("CS 374",  "Algorithms",               3, Collections.singletonList("CS 225"), false, true),
                req("CS 473",  "Algorithms II",            3, Collections.singletonList("CS 374"), true, false),
                genEd("HIST 100","World History",          3),
                elective("CS 499","Special Topics",        3)
        );
        return new Major("Computer Science", "Grainger College of Engineering", courses);
    }

    @Test
    public void generatedPlanHasEightSemesters() {
        AcademicPlan plan = PlanGenerator.generate(buildTestMajor());
        assertEquals(8, plan.getSemesters().size());
    }

    @Test
    public void allCoursesAreScheduled() {
        Major major = buildTestMajor();
        AcademicPlan plan = PlanGenerator.generate(major);

        Set<String> scheduled = new HashSet<>();
        for (Semester semester : plan.getSemesters()) {
            for (Course course : semester.getCourses()) {
                scheduled.add(course.getId());
            }
        }

        for (Course course : major.getCourses()) {
            assertTrue("Course not scheduled: " + course.getId(),
                    scheduled.contains(course.getId()));
        }
    }

    @Test
    public void noSemesterExceedsCreditLimit() {
        AcademicPlan plan = PlanGenerator.generate(buildTestMajor());

        for (Semester semester : plan.getSemesters()) {
            assertTrue("Semester " + semester.getDisplayName() + " exceeds 18 credits",
                    semester.getTotalCredits() <= 18);
        }
    }

    @Test
    public void prerequisitesAreAlwaysBeforeTheirDependents() {
        Major major = buildTestMajor();
        AcademicPlan plan = PlanGenerator.generate(major);

        Map<String, Integer> semesterIndex = new HashMap<>();
        List<Semester> semesters = plan.getSemesters();
        for (int i = 0; i < semesters.size(); i++) {
            for (Course course : semesters.get(i).getCourses()) {
                semesterIndex.put(course.getId(), i);
            }
        }

        for (Semester semester : semesters) {
            for (Course course : semester.getCourses()) {
                int courseIdx = semesterIndex.get(course.getId());
                for (String prereqId : course.getPrerequisites()) {
                    assertNotNull("Prerequisite not scheduled: " + prereqId,
                            semesterIndex.get(prereqId));
                    assertTrue(
                            prereqId + " must come before " + course.getId(),
                            semesterIndex.get(prereqId) < courseIdx);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Course req(String id, String name, int credits,
            List<String> prereqs, boolean fall, boolean spring) {
        return new Course(id, name, credits, prereqs, CourseType.MAJOR_REQUIREMENT,
                fall, spring, 0, false, 0, null);
    }

    private static Course genEd(String id, String name, int credits) {
        return new Course(id, name, credits, Collections.emptyList(), CourseType.GEN_ED,
                true, true, 0, false, 0, null);
    }

    private static Course elective(String id, String name, int credits) {
        return new Course(id, name, credits, Collections.emptyList(), CourseType.ELECTIVE,
                true, true, 0, false, 0, null);
    }
}