package edu.illinois.cs.cs124.ay2026.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.data.GenEdCoursePool;
import edu.illinois.cs.cs124.ay2026.project.model.AcademicPlan;
import edu.illinois.cs.cs124.ay2026.project.model.ApExam;
import edu.illinois.cs.cs124.ay2026.project.model.Course;
import edu.illinois.cs.cs124.ay2026.project.model.CourseType;
import edu.illinois.cs.cs124.ay2026.project.model.Goal;
import edu.illinois.cs.cs124.ay2026.project.model.IbExam;
import edu.illinois.cs.cs124.ay2026.project.model.Major;
import edu.illinois.cs.cs124.ay2026.project.model.RequirementGroup;
import edu.illinois.cs.cs124.ay2026.project.model.Semester;
import edu.illinois.cs.cs124.ay2026.project.model.UserProfile;

// Builds an 8-semester plan for a major. First it sorts the required courses
// so prerequisites come before the courses that need them, then it walks the
// semesters in order and drops in each course once its prereqs are done, it's
// offered that term, and it still fits under the credit cap.
public class PlanGenerator {

    private static final int MAX_CREDITS_PER_SEMESTER = 18;
    private static final int MAX_CREDITS_STUDY_ABROAD = 12;
    private static final int MAX_MAJOR_REQS_PER_SEMESTER = 3;
    private static final int MAX_MAJOR_REQS_ACCELERATED = 4;
    private static final int MAX_GEN_EDS_PER_SEMESTER = 2;


    /** A single gen-ed course suggestion matched to an interest category. */
    public static class GenEdSuggestion {
        public final String categoryLabel;
        public final Course course;

        GenEdSuggestion(String categoryLabel, Course course) {
            this.categoryLabel = categoryLabel;
            this.course = course;
        }
    }

    /**
     * Returns the top interest-matched gen-ed suggestion per category.
     * Only categories with at least one score > 0 are included.
     * Call this after the user saves interests to show what their plan would pick.
     */
    public static List<GenEdSuggestion> suggestedGenEds(Set<String> interests) {
        String[][] categories = {
            {"humanities_and_arts",               "Humanities & Arts"},
            {"natural_sciences_and_technology",   "Natural Sciences & Technology"},
            {"social_and_behavioral_sciences",    "Social & Behavioral Sciences"},
            {"cultural_studies_non_western",      "Cultural Studies - Non-Western"},
            {"cultural_studies_us_minority",      "Cultural Studies - US Minority"},
            {"cultural_studies_western_comparative", "Cultural Studies - Western"},
            {"language_other_than_english",       "Language (LOTE)"},
            {"quantitative_reasoning",            "Quantitative Reasoning"},
        };

        List<GenEdSuggestion> result = new ArrayList<>();
        for (String[] cat : categories) {
            String groupId = cat[0];
            String label   = cat[1];
            Course best = null;
            int bestScore = 0;
            for (Course c : GenEdCoursePool.optionsFor(groupId)) {
                int score = interestScore(c, interests);
                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
            if (best != null && bestScore > 0) {
                result.add(new GenEdSuggestion(label, best));
            }
        }
        return result;
    }

    /** Controls scheduling behavior when generating a plan. */
    public static class PlanOptions {
        public final int majorReqsPerSemester;
        public final int creditCap;

        public PlanOptions(int majorReqsPerSemester, int creditCap) {
            this.majorReqsPerSemester = majorReqsPerSemester;
            this.creditCap = creditCap;
        }
    }

    /** Generates a plan with default settings (used by tests and legacy callers). */
    public static AcademicPlan generate(Major major) {
        return generate(major, null);
    }

    public static AcademicPlan generate(Major major, UserProfile profile) {
        boolean graduateEarly = profile != null && profile.hasGoal(Goal.GRADUATE_EARLY);
        return generate(major, profile, new PlanOptions(
                graduateEarly ? MAX_MAJOR_REQS_ACCELERATED : MAX_MAJOR_REQS_PER_SEMESTER,
                MAX_CREDITS_PER_SEMESTER));
    }

    public static AcademicPlan generate(Major major, UserProfile profile, PlanOptions options) {
        List<Course> ordered = topologicalSort(major.getCourses());

        String startTerm = (profile != null) ? profile.getStartTerm() : "Fall";
        List<Semester> semesters = buildSemesters(startTerm);

        boolean studyAbroad = profile != null && profile.hasGoal(Goal.STUDY_ABROAD);
        boolean cpaTrack    = profile != null && profile.hasGoal(Goal.CPA_TRACK);
        int majorReqCap = options.majorReqsPerSemester;

        // Incoming AP/transfer credits shift the effective academic year for standing holds.
        // Every 30 credits roughly equals one year of standing.
        int incomingCredits = (profile != null) ? profile.getIncomingCredits() : 0;
        int standingBonus = incomingCredits / 30;

        if (studyAbroad) {
            for (Semester s : semesters) {
                if (s.getYear() == 3 && !s.isFall()) {
                    s.setStudyAbroad(true);
                    break;
                }
            }
        }

        Set<String> completed = new HashSet<>();
        Set<String> interests = (profile != null) ? profile.getInterests() : Collections.emptySet();

        // Pre-populate with courses the student already has credit for via AP/IB/dual credit
        if (profile != null) {
            completed.addAll(computePreCompletedCourses(profile));
        }

        // When the student has stated interests, sort free-choice gen-eds by interest score
        // so the greedy placer picks preferred subjects first.
        List<Course> genEdOrdered = ordered;
        if (!interests.isEmpty()) {
            genEdOrdered = new ArrayList<>(ordered);
            genEdOrdered.sort((a, b) -> {
                if (a.getType() != CourseType.GEN_ED || b.getType() != CourseType.GEN_ED) return 0;
                return interestScore(b, interests) - interestScore(a, interests);
            });
        }

        for (Semester semester : semesters) {
            Set<String> prereqSnapshot = new HashSet<>(completed);
            int creditCap = semester.isStudyAbroad() ? MAX_CREDITS_STUDY_ABROAD : options.creditCap;

            if (!semester.isStudyAbroad()) {
                placeCourses(ordered, semester, completed, prereqSnapshot,
                        CourseType.MAJOR_REQUIREMENT, majorReqCap, creditCap, cpaTrack, standingBonus);
            }
            placeCourses(genEdOrdered, semester, completed, prereqSnapshot,
                    CourseType.GEN_ED, MAX_GEN_EDS_PER_SEMESTER, creditCap, cpaTrack, standingBonus);
            placeCourses(ordered, semester, completed, prereqSnapshot,
                    CourseType.ELECTIVE, Integer.MAX_VALUE, creditCap, cpaTrack, standingBonus);
        }

        // Place elective/gen-ed options from requirement groups into remaining slots
        placeGroupCourses(major.getRequirementGroups(), semesters, completed, standingBonus,
                interests, options.creditCap);

        return new AcademicPlan(major.getName(), semesters);
    }

    private static void placeCourses(
            List<Course> ordered, Semester semester,
            Set<String> allPlaced, Set<String> prereqSnapshot,
            CourseType type, int typeCap, int creditCap,
            boolean cpaTrack, int standingBonus) {
        for (Course course : ordered) {
            if (course.getType() != type) continue;
            if (course.isCpaTrack() && !cpaTrack) continue;
            if (allPlaced.contains(course.getId())) continue;
            if (!isOffered(course, semester)) continue;
            if (!prerequisitesMet(course, prereqSnapshot)) continue;
            if (semester.getTotalCredits() + course.getCreditHours() > creditCap) continue;
            if (course.getMinYear() > 0 && (semester.getYear() + standingBonus) < course.getMinYear()) continue;
            if (countByType(semester, type) >= typeCap) continue;
            semester.addCourse(course);
            allPlaced.add(course.getId());
        }
    }

    private static int countByType(Semester semester, CourseType type) {
        int count = 0;
        for (Course c : semester.getCourses()) {
            if (c.getType() == type) count++;
        }
        return count;
    }

    /** Returns true if a course with the same subject prefix is already in the semester. */
    private static boolean hasSubjectConflict(Semester semester, Course candidate) {
        String prefix = subjectPrefix(candidate);
        for (Course c : semester.getCourses()) {
            if (subjectPrefix(c).equals(prefix)) return true;
        }
        return false;
    }

    /** Extracts the subject prefix from a course ID, e.g. "CS" from "CS 225". */
    private static String subjectPrefix(Course course) {
        String id = course.getId();
        int space = id.indexOf(' ');
        return space >= 0 ? id.substring(0, space) : id;
    }

    /**
     * Places courses from requirement groups (technical electives, gen-ed options, etc.)
     * into remaining semester slots after all individually-specified courses are placed.
     *
     * For each group, courses already placed as required courses count toward the requirement.
     * Remaining slots are filled greedily from the options list.
     */
    private static void placeGroupCourses(
            List<RequirementGroup> groups,
            List<Semester> semesters,
            Set<String> allCompleted,
            int standingBonus,
            Set<String> interests,
            int creditCap) {
        if (groups == null || groups.isEmpty()) return;

        // Build cumulative completion snapshots so prereq checks are semester-aware.
        // snapshot[i] = all courses completed by the END of semester i.
        List<Set<String>> snapshots = new ArrayList<>();
        Set<String> cumulative = new HashSet<>();
        for (Semester s : semesters) {
            for (Course c : s.getCourses()) cumulative.add(c.getId());
            snapshots.add(new HashSet<>(cumulative));
        }

        for (RequirementGroup group : groups) {
            boolean creditBased = group.getRequiredCredits() > 0;
            int target = creditBased ? group.getRequiredCredits() : group.getRequiredCount();
            // A group with 0 credits and 0 count is a free-choice gen-ed placeholder.
            // Use pool size as a heuristic: large pools are catch-all gen-ed blocks
            // (target 15 cr), small pools are specific categories (target 3 cr).
            if (target == 0 && group.getOptions().isEmpty()) {
                List<Course> pool = GenEdCoursePool.optionsFor(group.getId());
                if (!pool.isEmpty()) {
                    creditBased = true;
                    target = pool.size() > 20 ? 15 : 3;
                }
            }
            int met = 0;

            // When the major leaves a gen-ed group open (no specific options listed),
            // fill from the university-wide pool for that category.
            List<Course> effectiveOptions = group.getOptions().isEmpty()
                    ? GenEdCoursePool.optionsFor(group.getId())
                    : group.getOptions();

            // Credit for options that are already placed as required courses
            for (Course opt : effectiveOptions) {
                if (allCompleted.contains(opt.getId())) {
                    met += creditBased ? opt.getCreditHours() : 1;
                }
            }

            // Sort options by interest match so preferred courses are placed first
            List<Course> rankedOptions = new ArrayList<>(effectiveOptions);
            if (!interests.isEmpty()) {
                rankedOptions.sort((a, b) ->
                        interestScore(b, interests) - interestScore(a, interests));
            }

            // Place remaining options until the group requirement is satisfied
            for (Course opt : rankedOptions) {
                if (met >= target) break;
                if (allCompleted.contains(opt.getId())) continue;
                if (group.getMinLevel() > 0 && courseNumber(opt) < group.getMinLevel()) continue;

                for (int i = 0; i < semesters.size(); i++) {
                    Semester sem = semesters.get(i);
                    if (sem.isStudyAbroad()) continue;
                    if (!isOffered(opt, sem)) continue;
                    Set<String> prereqSnap = i > 0 ? snapshots.get(i - 1) : Collections.emptySet();
                    if (!prerequisitesMet(opt, prereqSnap)) continue;
                    if (sem.getTotalCredits() + opt.getCreditHours() > creditCap) continue;
                    if (opt.getMinYear() > 0 && (sem.getYear() + standingBonus) < opt.getMinYear()) continue;

                    sem.addCourse(opt);
                    allCompleted.add(opt.getId());
                    for (int j = i; j < snapshots.size(); j++) snapshots.get(j).add(opt.getId());
                    met += creditBased ? opt.getCreditHours() : 1;
                    break;
                }
            }
        }
    }

    /**
     * Returns a score for how well a course matches the user's stated interests.
     * Higher score = better match. Score is additive across matched interests.
     */
    private static int interestScore(Course course, Set<String> interests) {
        String prefix = subjectPrefix(course).toUpperCase();
        String name = course.getName() != null ? course.getName().toLowerCase() : "";
        int score = 0;

        for (String interest : interests) {
            switch (interest) {
                // --- Humanities & Arts (LA / HP gen-eds) ---
                case "Philosophy & Ethics":
                    if (isIn(prefix, "PHIL", "REL", "CLCV")) score += 2;
                    if (name.contains("philosophy") || name.contains("ethics") || name.contains("moral")
                            || name.contains("logic") || name.contains("religion")) score += 1;
                    break;
                case "Ancient World & Classical Civilizations":
                    if (isIn(prefix, "CLCV", "HIST", "REL", "LING")) score += 2;
                    if (name.contains("ancient") || name.contains("classical") || name.contains("greek")
                            || name.contains("roman") || name.contains("medieval") || name.contains("myth")) score += 1;
                    break;
                case "Theater & Performance":
                    if (isIn(prefix, "THEA", "DANC", "FAA")) score += 2;
                    if (name.contains("theatre") || name.contains("theater") || name.contains("performance")
                            || name.contains("dance") || name.contains("acting") || name.contains("directing")) score += 1;
                    break;
                case "Visual Art & Design":
                    if (isIn(prefix, "ART", "ARTD", "ARTH", "ARTE", "LA", "ARCH")) score += 2;
                    if (name.contains("art") || name.contains("design") || name.contains("drawing")
                            || name.contains("painting") || name.contains("sculpture") || name.contains("visual")) score += 1;
                    break;
                case "Music & Sound":
                    if (isIn(prefix, "MUSI", "MUS")) score += 2;
                    if (name.contains("music") || name.contains("sound") || name.contains("composition")
                            || name.contains("theory") || name.contains("jazz") || name.contains("orchestra")) score += 1;
                    break;
                case "Film & Media Studies":
                    if (isIn(prefix, "CMN", "MDIA", "JOUR", "ENGL")) score += 2;
                    if (name.contains("film") || name.contains("cinema") || name.contains("media")
                            || name.contains("television") || name.contains("documentary") || name.contains("journalism")) score += 1;
                    break;
                case "Creative Writing & Literature":
                    if (isIn(prefix, "ENGL", "RHET", "CW", "SLCL")) score += 2;
                    if (name.contains("writing") || name.contains("literature") || name.contains("fiction")
                            || name.contains("poetry") || name.contains("narrative") || name.contains("rhetoric")) score += 1;
                    break;
                case "Religion & Spirituality":
                    if (isIn(prefix, "REL", "CLCV", "EALC", "HIST")) score += 2;
                    if (name.contains("religion") || name.contains("religious") || name.contains("spiritual")
                            || name.contains("theology") || name.contains("sacred") || name.contains("belief")) score += 1;
                    break;

                // --- Natural Sciences (PS / LS gen-eds) ---
                case "Astronomy & Space Science":
                    if (isIn(prefix, "ASTR", "PHYS")) score += 2;
                    if (name.contains("astronomy") || name.contains("astrophysics") || name.contains("cosmology")
                            || name.contains("space") || name.contains("planet") || name.contains("universe")) score += 1;
                    break;
                case "Climate & Earth Science":
                    if (isIn(prefix, "ATMS", "GEOL", "NRES", "ENVS", "ESE")) score += 2;
                    if (name.contains("climate") || name.contains("weather") || name.contains("geology")
                            || name.contains("atmosphere") || name.contains("earth") || name.contains("soil")) score += 1;
                    break;
                case "Ecology & Conservation":
                    if (isIn(prefix, "IB", "NRES", "ENVS", "BIOL", "EVO")) score += 2;
                    if (name.contains("ecology") || name.contains("environment") || name.contains("conservation")
                            || name.contains("wildlife") || name.contains("biodiversity") || name.contains("ecosystem")) score += 1;
                    break;
                case "Neuroscience & the Brain":
                    if (isIn(prefix, "MCB", "PSYC", "IB", "NEUR", "BCOG")) score += 2;
                    if (name.contains("neuroscience") || name.contains("brain") || name.contains("neural")
                            || name.contains("cognition") || name.contains("perception") || name.contains("nervous")) score += 1;
                    break;
                case "Genetics & Evolution":
                    if (isIn(prefix, "MCB", "IB", "BIOL", "EVO", "CPSC")) score += 2;
                    if (name.contains("genetics") || name.contains("evolution") || name.contains("genomics")
                            || name.contains("dna") || name.contains("heredity") || name.contains("molecular")) score += 1;
                    break;
                case "Human Biology & Health":
                    if (isIn(prefix, "MCB", "IB", "KIN", "KINES", "NRES", "BIOL")) score += 2;
                    if (name.contains("human") || name.contains("health") || name.contains("anatomy")
                            || name.contains("physiology") || name.contains("nutrition") || name.contains("disease")) score += 1;
                    break;

                // --- Social & Behavioral Sciences (SS / BSC gen-eds) ---
                case "Psychology & Mental Health":
                    if (isIn(prefix, "PSYC", "SHS", "HDFS")) score += 2;
                    if (name.contains("psychology") || name.contains("mental") || name.contains("behavior")
                            || name.contains("cognition") || name.contains("therapy") || name.contains("well-being")) score += 1;
                    break;
                case "Political Science & Law":
                    if (isIn(prefix, "POLS", "LAW", "GLBL", "PS")) score += 2;
                    if (name.contains("politics") || name.contains("political") || name.contains("law")
                            || name.contains("government") || name.contains("democracy") || name.contains("policy")) score += 1;
                    break;
                case "Sociology & Social Issues":
                    if (isIn(prefix, "SOC", "AFRO", "WGSS", "CW")) score += 2;
                    if (name.contains("society") || name.contains("social") || name.contains("inequality")
                            || name.contains("community") || name.contains("race") || name.contains("class")) score += 1;
                    break;
                case "Communication & Rhetoric":
                    if (isIn(prefix, "CMN", "RHET", "MDIA", "JOUR")) score += 2;
                    if (name.contains("communication") || name.contains("rhetoric") || name.contains("speech")
                            || name.contains("persuasion") || name.contains("public") || name.contains("argument")) score += 1;
                    break;
                case "Anthropology & Archaeology":
                    if (isIn(prefix, "ANTH", "CLCV")) score += 2;
                    if (name.contains("anthropology") || name.contains("archaeology") || name.contains("human evolution")
                            || name.contains("culture") || name.contains("fossil") || name.contains("ritual")) score += 1;
                    break;
                case "Gender & Sexuality Studies":
                    if (isIn(prefix, "WGSS", "SOC", "AFRO", "LLS")) score += 2;
                    if (name.contains("gender") || name.contains("sexuality") || name.contains("feminism")
                            || name.contains("women") || name.contains("queer") || name.contains("identity")) score += 1;
                    break;
                case "Geography & Urban Studies":
                    if (isIn(prefix, "GEOG", "UP", "NRES")) score += 2;
                    if (name.contains("geography") || name.contains("urban") || name.contains("city")
                            || name.contains("planning") || name.contains("landscape") || name.contains("spatial")) score += 1;
                    break;

                // --- Cultural Studies (NW / US / WCC gen-eds) ---
                case "East Asian Cultures":
                    if (isIn(prefix, "EALC", "JAPN", "CHIN", "KORE")) score += 2;
                    if (name.contains("japanese") || name.contains("chinese") || name.contains("korean")
                            || name.contains("asia") || name.contains("east asian") || name.contains("buddhism")) score += 1;
                    break;
                case "Latin American & Caribbean Studies":
                    if (isIn(prefix, "LLS", "SPAN", "PORT", "LAST")) score += 2;
                    if (name.contains("latin") || name.contains("hispanic") || name.contains("caribbean")
                            || name.contains("mexico") || name.contains("brazil") || name.contains("chicano")) score += 1;
                    break;
                case "African & African American Studies":
                    if (isIn(prefix, "AFRO", "AAS", "AFST")) score += 2;
                    if (name.contains("african") || name.contains("black") || name.contains("diaspora")
                            || name.contains("slavery") || name.contains("civil rights") || name.contains("colonial")) score += 1;
                    break;
                case "Middle Eastern & Islamic Studies":
                    if (isIn(prefix, "ARAB", "HEBR", "PERS", "TURK", "HIST")) score += 2;
                    if (name.contains("middle east") || name.contains("islamic") || name.contains("arab")
                            || name.contains("jewish") || name.contains("ottoman") || name.contains("persian")) score += 1;
                    break;
                case "Native American & Indigenous Studies":
                    if (isIn(prefix, "AIS", "ANTH", "HIST")) score += 2;
                    if (name.contains("native") || name.contains("indigenous") || name.contains("american indian")
                            || name.contains("tribal") || name.contains("sovereignty") || name.contains("decolonial")) score += 1;
                    break;
                case "European History & Culture":
                    if (isIn(prefix, "HIST", "SLAV", "FREN", "GERM", "ITAL", "CLCV")) score += 2;
                    if (name.contains("european") || name.contains("europe") || name.contains("french")
                            || name.contains("german") || name.contains("british") || name.contains("renaissance")) score += 1;
                    break;
                case "Global Issues & Justice":
                    if (isIn(prefix, "GLBL", "POLS", "AFRO", "LLS", "WGSS", "GEOG")) score += 2;
                    if (name.contains("global") || name.contains("international") || name.contains("justice")
                            || name.contains("human rights") || name.contains("development") || name.contains("migration")) score += 1;
                    break;

                // --- Languages (LOTE) ---
                case "Spanish":
                    if (isIn(prefix, "SPAN")) score += 3;
                    break;
                case "French":
                    if (isIn(prefix, "FREN")) score += 3;
                    break;
                case "German":
                    if (isIn(prefix, "GERM")) score += 3;
                    break;
                case "Japanese":
                    if (isIn(prefix, "JAPN")) score += 3;
                    break;
                case "Mandarin Chinese":
                    if (isIn(prefix, "CHIN")) score += 3;
                    break;
                case "Arabic":
                    if (isIn(prefix, "ARAB")) score += 3;
                    break;
                case "Korean":
                    if (isIn(prefix, "KORE")) score += 3;
                    break;
                case "Italian":
                    if (isIn(prefix, "ITAL")) score += 3;
                    break;
                case "Portuguese":
                    if (isIn(prefix, "PORT")) score += 3;
                    break;

                // --- Quantitative Reasoning (QR I / QR II) ---
                case "Statistics & Data":
                    if (isIn(prefix, "STAT", "ACE")) score += 2;
                    if (name.contains("statistics") || name.contains("data") || name.contains("probability")
                            || name.contains("regression") || name.contains("inference") || name.contains("sampling")) score += 1;
                    break;
                case "Logic & Formal Reasoning":
                    if (isIn(prefix, "PHIL", "MATH", "CS")) score += 2;
                    if (name.contains("logic") || name.contains("reasoning") || name.contains("proof")
                            || name.contains("deduction") || name.contains("formal") || name.contains("discrete")) score += 1;
                    break;

                default:
                    break;
            }
        }
        return score;
    }

    private static boolean isIn(String value, String... options) {
        for (String opt : options) {
            if (opt.equals(value)) return true;
        }
        return false;
    }

    private static int courseNumber(Course course) {
        String[] parts = course.getId().split(" ");
        if (parts.length >= 2) {
            try { return Integer.parseInt(parts[1]); }
            catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    /**
     * Merges two majors into one for combined plan generation.
     *
     * Courses shared by both majors are deduplicated (MAJOR_REQUIREMENT wins over
     * lower-priority types). Returns the merged Major; call secondaryOnlyIds() on
     * the two originals to find which course IDs are exclusive to the secondary major.
     */
    public static Major mergeMajors(Major primary, Major secondary) {
        Map<String, Course> merged = new LinkedHashMap<>();
        for (Course c : primary.getCourses()) {
            merged.put(c.getId(), c);
        }
        for (Course c : secondary.getCourses()) {
            if (!merged.containsKey(c.getId())) {
                merged.put(c.getId(), c);
            } else if (c.getType() == CourseType.MAJOR_REQUIREMENT
                    && merged.get(c.getId()).getType() != CourseType.MAJOR_REQUIREMENT) {
                merged.put(c.getId(), c);
            }
        }
        // Combine requirement groups from both majors. When placeGroupCourses runs,
        // any course already placed for a primary-major group is in allCompleted and
        // automatically counts toward the same-option group in the secondary major.
        List<RequirementGroup> combinedGroups = new ArrayList<>();
        combinedGroups.addAll(primary.getRequirementGroups());
        combinedGroups.addAll(secondary.getRequirementGroups());

        String combinedName = primary.getName() + " + " + secondary.getName();
        return new Major(combinedName, primary.getCollege(), new ArrayList<>(merged.values()),
                combinedGroups, Collections.emptyList());
    }

    /**
     * Returns course IDs that appear as elective options in BOTH majors' requirement groups.
     * These courses, if scheduled, satisfy elective requirements for both degrees simultaneously.
     */
    public static Set<String> sharedElectiveIds(Major primary, Major secondary) {
        Set<String> primaryOptions = new HashSet<>();
        for (RequirementGroup g : primary.getRequirementGroups()) {
            for (Course opt : g.getOptions()) primaryOptions.add(opt.getId());
        }
        Set<String> shared = new HashSet<>();
        for (RequirementGroup g : secondary.getRequirementGroups()) {
            for (Course opt : g.getOptions()) {
                if (primaryOptions.contains(opt.getId())) shared.add(opt.getId());
            }
        }
        return shared;
    }

    /**
     * Returns course IDs that appear in secondary but not in primary.
     * Used by the adapter to show the "2nd" badge on secondary-exclusive courses.
     */
    public static Set<String> secondaryOnlyIds(Major primary, Major secondary) {
        Set<String> primaryIds = new HashSet<>();
        for (Course c : primary.getCourses()) {
            primaryIds.add(c.getId());
        }
        Set<String> result = new HashSet<>();
        for (Course c : secondary.getCourses()) {
            if (!primaryIds.contains(c.getId())) {
                result.add(c.getId());
            }
        }
        return result;
    }

    /**
     * Returns the set of UIUC course IDs the student already has credit for based on
     * their AP scores, IB scores, and dual credit entries. These are added to the
     * completed set before scheduling begins so prerequisites are properly unlocked.
     */
    private static Set<String> computePreCompletedCourses(UserProfile profile) {
        Set<String> result = new HashSet<>();

        for (Map.Entry<String, Integer> entry : profile.getApScores().entrySet()) {
            try {
                ApExam exam = ApExam.valueOf(entry.getKey());
                if (entry.getValue() >= exam.getMinScoreForCredit()) {
                    result.addAll(exam.getUiucEquivalents());
                }
            } catch (IllegalArgumentException ignored) {
                // Unrecognized exam name - skip
            }
        }

        for (Map.Entry<String, Integer> entry : profile.getIbScores().entrySet()) {
            try {
                IbExam exam = IbExam.valueOf(entry.getKey());
                if (entry.getValue() >= exam.getMinScoreForCredit()) {
                    result.addAll(exam.getUiucEquivalents());
                }
            } catch (IllegalArgumentException ignored) {
                // Unrecognized exam name - skip
            }
        }

        result.addAll(profile.getDualCreditCourseIds());
        return result;
    }

    /**
     * Kahn's topological sort. Courses with no prerequisites come first.
     * Courses with prerequisites come after all their prerequisites.
     * Cycles (which shouldn't exist in a real curriculum) are placed at the end.
     */
    private static List<Course> topologicalSort(List<Course> courses) {
        Map<String, Course> courseById = new HashMap<>();
        for (Course course : courses) {
            courseById.put(course.getId(), course);
        }

        // Build in-degree map and adjacency list
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();

        for (Course course : courses) {
            inDegree.put(course.getId(), 0);
            dependents.put(course.getId(), new ArrayList<>());
        }

        for (Course course : courses) {
            for (String prereqId : course.getPrerequisites()) {
                if (dependents.containsKey(prereqId)) {
                    dependents.get(prereqId).add(course.getId());
                    inDegree.put(course.getId(), inDegree.get(course.getId()) + 1);
                }
            }
        }

        // Primary: courses offered in fewer terms are placed first (Fall-only before Fall+Spring).
        // Secondary: within the same offering count, courses with an earlier suggested year
        //   from the catalog's sample sequence are prioritized (0 = no suggestion, treated as year 5
        //   so unspecified courses are placed after courses with an explicit suggestion).
        Comparator<String> byOfferingCount = Comparator
                .comparingInt((String id) -> {
                    Course c = courseById.get(id);
                    int count = 0;
                    if (c.isOfferedFall()) count++;
                    if (c.isOfferedSpring()) count++;
                    return count;
                })
                .thenComparingInt(id -> {
                    int sy = courseById.get(id).getSuggestedYear();
                    return sy > 0 ? sy : 5;
                });
        Queue<String> queue = new PriorityQueue<>(byOfferingCount);
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Course> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String courseId = queue.poll();
            sorted.add(courseById.get(courseId));
            for (String dependentId : dependents.get(courseId)) {
                int newDegree = inDegree.get(dependentId) - 1;
                inDegree.put(dependentId, newDegree);
                if (newDegree == 0) {
                    queue.add(dependentId);
                }
            }
        }

        // Add any remaining courses (e.g., from unresolved prerequisites)
        for (Course course : courses) {
            if (!sorted.contains(course)) {
                sorted.add(course);
            }
        }

        return sorted;
    }

    private static List<Semester> buildSemesters(String startTerm) {
        List<Semester> semesters = new ArrayList<>();
        String first  = "Fall".equals(startTerm) ? "Fall" : "Spring";
        String second = "Fall".equals(startTerm) ? "Spring" : "Fall";
        for (int year = 1; year <= 4; year++) {
            semesters.add(new Semester(year, first));
            semesters.add(new Semester(year, second));
        }
        return semesters;
    }

    private static boolean isOffered(Course course, Semester semester) {
        return semester.isFall() ? course.isOfferedFall() : course.isOfferedSpring();
    }

    private static boolean prerequisitesMet(Course course, Set<String> completed) {
        return completed.containsAll(course.getPrerequisites());
    }
}