package edu.illinois.cs.cs124.ay2026.project.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.illinois.cs.cs124.ay2026.project.model.Course;
import edu.illinois.cs.cs124.ay2026.project.model.CourseType;

/**
 * University-wide pool of UIUC courses that satisfy each gen-ed category.
 * Used by PlanGenerator when a major's requirement group has no options specified
 * (i.e., the slot is truly free-choice within the category).
 *
 * Courses are real UIUC offerings. Credit hours and offering terms reflect
 * typical catalog values. No prerequisites within the pool - all are entry-level.
 *
 * The alias block at the bottom maps all variant group IDs found in courses_2024.json
 * to the canonical pool lists so that every major gets interest-guided gen-eds.
 */
public class GenEdCoursePool {

    private static final Map<String, List<Course>> POOL = new HashMap<>();

    static {
        // -------------------------------------------------------------------------
        // Primary pools (canonical IDs)
        // -------------------------------------------------------------------------

        // --- Composition I (FC1) ---
        POOL.put("composition_i", list(
                c("RHET 105", "Principles of Composition", 4)
        ));

        // --- Advanced Composition (ACP / CLL) ---
        POOL.put("advanced_composition", list(
                c("ENGL 201", "Advanced Composition", 3),
                c("CMN 209", "Communication Report Writing", 3),
                c("JOUR 201", "Reporting and Writing I", 3),
                c("RHET 233", "Writing for the Web", 3),
                c("AGST 201", "Writing in Agriculture", 3)
        ));

        // --- Humanities & Arts: Literature & Arts (LA) + Historical & Philosophical (HP) ---
        POOL.put("humanities_and_arts", list(
                c("PHIL 101", "Introduction to Philosophy", 3),
                c("MUSI 100", "Introduction to Music", 3),
                c("THEA 101", "Introduction to Theatre", 3),
                c("ART 100",  "Introduction to Art History", 3),
                c("ENGL 109", "Introduction to Fiction", 3),
                c("CLCV 115", "Classical Mythology", 3),
                c("REL 110",  "Introduction to World Religions", 3),
                c("HIST 141", "Western Civilization I", 3),
                c("HIST 142", "Western Civilization II", 3),
                c("DANC 100", "Introduction to Dance", 3),
                c("ARTH 110", "Introduction to Art", 3),
                c("MUSI 120", "Music in World Cultures", 3),
                c("PHIL 201", "Ethics", 3)
        ));

        // --- Natural Sciences & Technology: Physical Sciences (PS) + Life Sciences (LS) ---
        POOL.put("natural_sciences_and_technology", list(
                c("ASTR 100", "Introductory Astronomy", 3),
                c("ATMS 100", "Introduction to Meteorology", 3),
                c("GEOL 107", "Physical Geology", 3),
                c("PHYS 100", "Conceptual Physics", 3),
                c("CHEM 101", "Chemistry and Society", 3),
                c("IB 150",   "Organismal & Evolutionary Biology", 3),
                c("MCB 150",  "Molecular & Cellular Biology", 3),
                c("BIOL 100", "Introduction to Biology", 3),
                c("IB 104",   "Environmental Challenges", 3),
                c("NRES 102", "Environment and Society", 3),
                c("ATMS 120", "Climate and Global Change", 3),
                c("GEOL 143", "Earth, Energy, and Environment", 3),
                c("ASTR 210", "Stars and Galaxies", 3)
        ));

        // Physical sciences sub-pool (PS only - ASTR, ATMS, GEOL, PHYS, CHEM)
        POOL.put("physical_sciences", list(
                c("ASTR 100", "Introductory Astronomy", 3),
                c("ATMS 100", "Introduction to Meteorology", 3),
                c("GEOL 107", "Physical Geology", 3),
                c("PHYS 100", "Conceptual Physics", 3),
                c("CHEM 101", "Chemistry and Society", 3),
                c("ATMS 120", "Climate and Global Change", 3),
                c("GEOL 143", "Earth, Energy, and Environment", 3),
                c("ASTR 210", "Stars and Galaxies", 3)
        ));

        // Life sciences sub-pool (LS only - IB, MCB, BIOL, NRES)
        POOL.put("life_sciences", list(
                c("IB 150",   "Organismal & Evolutionary Biology", 3),
                c("MCB 150",  "Molecular & Cellular Biology", 3),
                c("BIOL 100", "Introduction to Biology", 3),
                c("IB 104",   "Environmental Challenges", 3),
                c("NRES 102", "Environment and Society", 3),
                c("ATMS 120", "Climate and Global Change", 3)
        ));

        // --- Social & Behavioral Sciences: Social Sciences (SS) + Behavioral Sciences (BSC) ---
        POOL.put("social_and_behavioral_sciences", list(
                c("PSYC 100", "Introduction to Psychology", 3),
                c("SOC 100",  "Introduction to Sociology", 3),
                c("POLS 100", "Introduction to Political Science", 3),
                c("ANTH 101", "Introduction to Anthropology", 3),
                c("GEOG 101", "Introduction to Geography", 3),
                c("CMN 101",  "Public Speaking", 3),
                c("HDFS 105", "Individual and Family Development", 3),
                c("EPSY 210", "Educational Psychology", 3)
        ));
        POOL.put("social_sciences",    POOL.get("social_and_behavioral_sciences"));
        POOL.put("behavioral_sciences", POOL.get("social_and_behavioral_sciences"));

        // --- Cultural Studies: Non-Western (NW) ---
        POOL.put("cultural_studies_non_western", list(
                c("EALC 120", "Introduction to East Asian Cultures", 3),
                c("HIST 151", "History of East Asia", 3),
                c("REL 205",  "Introduction to Buddhism", 3),
                c("ANTH 210", "Cultural Diversity", 3),
                c("SOC 280",  "Globalization and Society", 3),
                c("HIST 160", "Introduction to the Modern Middle East", 3),
                c("EALC 242", "Masterworks of Japanese Literature", 3),
                c("HIST 352", "Modern China", 3),
                c("REL 220",  "Hinduism", 3)
        ));

        // --- Cultural Studies: US Minority (US) ---
        POOL.put("cultural_studies_us_minority", list(
                c("AFRO 100", "Introduction to Africana Studies", 3),
                c("LLS 100",  "Introduction to Latina/Latino Studies", 3),
                c("WGSS 101", "Introduction to Women's Studies", 3),
                c("SOC 201",  "Race, Ethnicity, and Immigration", 3),
                c("HIST 170", "African American History", 3),
                c("AIS 101",  "Introduction to American Indian Studies", 3),
                c("AFRO 201", "Black Women in America", 3),
                c("CMN 332",  "Communication and Race", 3)
        ));

        // --- Cultural Studies: Western/Comparative (WCC) ---
        POOL.put("cultural_studies_western_comparative", list(
                c("HIST 141", "Western Civilization I", 3),
                c("HIST 142", "Western Civilization II", 3),
                c("PHIL 201", "Ethics", 3),
                c("CLCV 222", "Greek and Roman Civilization", 3),
                c("CMN 225",  "Intercultural Communication", 3),
                c("HIST 200", "The Modern World", 3),
                c("PHIL 104", "Introduction to Ethics", 3),
                c("SLAV 241", "Russian Literature in Translation", 3)
        ));

        // --- Quantitative Reasoning (QR I and QR II) ---
        POOL.put("quantitative_reasoning", list(
                c("STAT 100", "Statistics", 3),
                c("MATH 112", "Algebra", 3),
                c("MATH 115", "Precalculus Trigonometry", 3),
                c("PHIL 102", "Logic and Reasoning", 3),
                c("STAT 200", "Statistical Analysis", 3),
                c("ACE 261",  "Statistical Methods", 3)
        ));
        POOL.put("quantitative_reasoning_i",  POOL.get("quantitative_reasoning"));
        POOL.put("quantitative_reasoning_ii", POOL.get("quantitative_reasoning"));

        // --- Language Other Than English (LOTE) ---
        POOL.put("language_other_than_english", list(
                c("SPAN 101", "Elementary Spanish I",       4),
                c("FREN 101", "Elementary French I",        4),
                c("GERM 101", "Elementary German I",        4),
                c("JAPN 101", "Elementary Japanese I",      4),
                c("CHIN 101", "Elementary Chinese I",       4),
                c("ARAB 101", "Elementary Arabic I",        4),
                c("KORE 101", "Elementary Korean I",        4),
                c("ITAL 101", "Elementary Italian I",       4),
                c("PORT 101", "Elementary Portuguese I",    4),
                c("RUSS 101", "Elementary Russian I",       4),
                c("SPAN 102", "Elementary Spanish II",      4,
                        Collections.singletonList("SPAN 101")),
                c("FREN 102", "Elementary French II",       4,
                        Collections.singletonList("FREN 101")),
                c("JAPN 102", "Elementary Japanese II",     4,
                        Collections.singletonList("JAPN 101")),
                c("CHIN 102", "Elementary Chinese II",      4,
                        Collections.singletonList("CHIN 101")),
                c("SPAN 201", "Intermediate Spanish I",     3,
                        Collections.singletonList("SPAN 102")),
                c("FREN 201", "Intermediate French I",      3,
                        Collections.singletonList("FREN 102")),
                c("JAPN 201", "Intermediate Japanese I",    3,
                        Collections.singletonList("JAPN 102")),
                c("SPAN 202", "Intermediate Spanish II",    3,
                        Collections.singletonList("SPAN 201"))
        ));

        // -------------------------------------------------------------------------
        // Aliases: all variant group IDs found in courses_2024.json
        // -------------------------------------------------------------------------

        // -- Composition I aliases --
        alias("composition_i",
                "gen_ed_composition_i", "gen_ed_composition", "composition_requirement",
                "composition_i_or_gen_ed", "gen_ed_or_comp_i_spring",
                "composition_and_gen_ed", "composition_and_speaking", "composition_and_speech",
                "composition_sequence", "composition_i_and_ii", "composition_i_or_gen_ed",
                "gen_ed_composition_speech", "greek_latin_track_composition");

        // -- Advanced Composition aliases --
        alias("advanced_composition",
                "gen_ed_advanced_composition", "advanced_composition_gen_ed",
                "advanced_composition_humanities", "gen_ed_composition_and_humanities_sbs",
                "gen_ed_composition_humanities_sbs");

        // -- Humanities & Arts aliases --
        alias("humanities_and_arts",
                "gen_ed_humanities_arts", "gen_ed_humanities_and_arts",
                "humanities_arts", "humanities_and_the_arts", "humanities_fine_arts",
                "humanities", "humanities_gen_ed", "gen_ed_humanities",
                "literature_and_arts", "historical_philosophical_perspective",
                "humanities_and_social_sciences", "humanities_social_sciences",
                "humanities_social_sciences_gen_ed", "gen_ed_humanities_social_sciences",
                "gen_ed_humanities_sbs_cultural_studies",
                "humanities_natural_sciences_gen_ed",
                "humanities_social_behavioral_elective",
                "humanities_social_science_3hrs", "humanities_social_science_6hrs",
                "humanities_social_science_elective", "supplemental_humanities",
                "storytelling_gen_ed");

        // -- Natural Sciences & Technology aliases (full combined pool) --
        alias("natural_sciences_and_technology",
                "gen_ed_natural_sciences", "natural_sciences_technology",
                "gen_ed_natural_sciences_technology", "gen_ed_natural_science_tech",
                "gen_ed_nat_sci_tech", "gen_ed_natural_science_and_technology",
                "gen_ed_natural_sciences_and_technology", "natural_sciences",
                "natural_sciences_and_tech", "natural_sciences_gen_ed",
                "natural_sciences_technology", "gen_ed_life_sciences",
                "earth_and_space_science");

        // -- Physical sciences aliases --
        alias("physical_sciences",
                "gen_ed_physical_science", "gen_ed_physical_sciences",
                "physical_science", "natural_science_physical",
                "natural_sciences_physical", "physical_science_gen_ed",
                "physical_sciences_requirement");

        // -- Life sciences aliases --
        alias("life_sciences",
                "gen_ed_life_science", "life_science", "natural_science_life",
                "natural_sciences_life", "life_sciences_requirement",
                "gen_ed_life_science");

        // -- Social & Behavioral Sciences aliases --
        alias("social_and_behavioral_sciences",
                "social_behavioral_sciences", "gen_ed_social_behavioral",
                "gen_ed_social_behavioral_sciences", "gen_ed_social_and_behavioral_sciences",
                "social_behavioral_science", "social_behavioral_science_gen_ed",
                "social_behavioral_sciences_gen_ed", "social_behavioral_sciences_i",
                "additional_social_behavioral", "additional_social_behavioral_science",
                "additional_social_behavioral_sciences", "additional_social_science",
                "additional_social_science_gen_ed", "social_sciences_gen_ed",
                "foundation_social_behavioral", "additional_social_behavioral_science",
                "cognate_introductory_social_science", "core_social_course",
                "gen_ed_social_behavioral", "psyc_gen_ed",
                "social_science_core", "human_social_core",
                "foundation_list_2_social_individual_diff",
                "ese_introductory_core_social_science");

        // -- Cultural Studies: Non-Western aliases --
        alias("cultural_studies_non_western",
                "gen_ed_non_western", "gen_ed_non_western_cultures",
                "gen_ed_nonwestern", "cultural_studies_nonwestern",
                "non_western_cultures", "non_western", "non_western_culture",
                "non_western_culture_gen_ed", "non_western_cultures_gen_ed",
                "gen_ed_cultural_studies_nonwestern", "gen_ed_cultural_studies_non_western",
                "gen_ed_non_western_cultures", "non_western_culture");

        // -- Cultural Studies: US Minority aliases --
        alias("cultural_studies_us_minority",
                "gen_ed_us_minority", "gen_ed_us_minority_cultures",
                "cultural_studies_us_minorities", "us_minority_cultures",
                "us_minority_culture", "us_minority", "us_minority_culture_gen_ed",
                "gen_ed_cultural_studies_us_minority", "gen_ed_us_minority",
                "us_minorities_history", "difference_and_diaspora");

        // -- Cultural Studies: Western/Comparative aliases --
        alias("cultural_studies_western_comparative",
                "cultural_studies_western", "gen_ed_western_comparative",
                "gen_ed_western_comparative_cultures", "gen_ed_western_cultures",
                "gen_ed_western", "western_cultures", "western_comparative",
                "western_comparative_cultures", "western_culture", "western_culture_gen_ed",
                "gen_ed_cultural_studies_western", "gen_ed_cultural_studies_western_comparative",
                "gen_ed_western_comparative", "european_history");

        // -- Quantitative Reasoning aliases --
        alias("quantitative_reasoning",
                "gen_ed_quantitative_reasoning", "quantitative_reasoning_gen_ed",
                "gen_ed_qr1", "gen_ed_qr2", "gen_ed_quant_reasoning",
                "gen_ed_quantitative_reasoning_i", "gen_ed_quantitative_reasoning_ii",
                "quantitative_reasoning_i_or_ii", "general_education_quantitative_reasoning",
                "math_foundations_quantitative_reasoning_i",
                "math_foundations_quantitative_reasoning_ii",
                "general_education_qr1_statistics", "gen_ed_qri",
                "ese_introductory_core_quantitative");

        // -- Language aliases --
        alias("language_other_than_english",
                "language_requirement", "gen_ed_language",
                "foreign_language", "gen_ed_language_other_than_english",
                "gen_ed_language_requirement", "foreign_language_requirement",
                "gen_ed_language", "second_foreign_language",
                "anth_foundation_language", "component_1_language",
                "target_language_beyond_second_year");

        // -------------------------------------------------------------------------
        // Combined cultural-studies pool (for groups that don't specify NW/US/WCC)
        // -------------------------------------------------------------------------
        List<Course> combinedCultural = combined(
                POOL.get("cultural_studies_non_western"),
                POOL.get("cultural_studies_us_minority"),
                POOL.get("cultural_studies_western_comparative"));
        POOL.put("cultural_studies", combinedCultural);
        alias("cultural_studies",
                "cultural_studies_gen_ed", "gen_ed_cultural_studies",
                "gen_ed_cultural_studies_non_western",
                "concentration_cultural_understanding",
                "cwl_area_cultural_studies");

        // -------------------------------------------------------------------------
        // Combined gen-ed pool (catch-all groups that represent the whole gen-ed block)
        // -------------------------------------------------------------------------
        List<Course> allGenEds = combined(
                POOL.get("humanities_and_arts"),
                POOL.get("natural_sciences_and_technology"),
                POOL.get("social_and_behavioral_sciences"),
                POOL.get("cultural_studies_non_western"),
                POOL.get("cultural_studies_us_minority"),
                POOL.get("cultural_studies_western_comparative"),
                POOL.get("quantitative_reasoning"));
        POOL.put("general_education", allGenEds);
        alias("general_education",
                "campus_general_education", "gen_ed_requirements",
                "gen_ed_courses", "general_education_courses",
                "general_education_remaining", "campus_gen_ed",
                "gen_ed_elective", "gen_ed_humanities_sbs_cultural_studies",
                "general_education_electives");
    }

    /**
     * Returns the pool of courses for a given gen-ed requirement group ID.
     * Returns an empty list if the group ID is not a recognized gen-ed category.
     */
    public static List<Course> optionsFor(String groupId) {
        List<Course> options = POOL.get(groupId);
        return options != null ? options : Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Points multiple alias IDs at the same pool list as the canonical ID. */
    private static void alias(String canonicalId, String... aliases) {
        List<Course> pool = POOL.get(canonicalId);
        if (pool == null) return;
        for (String alias : aliases) {
            POOL.put(alias, pool);
        }
    }

    /** Merges multiple pool lists into a single deduplicated list. */
    @SafeVarargs
    private static List<Course> combined(List<Course>... pools) {
        List<Course> result = new ArrayList<>();
        for (List<Course> pool : pools) {
            if (pool == null) continue;
            for (Course c : pool) {
                boolean found = false;
                for (Course existing : result) {
                    if (existing.getId().equals(c.getId())) { found = true; break; }
                }
                if (!found) result.add(c);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Course c(String id, String name, int credits) {
        return new Course(id, name, credits, Collections.emptyList(), CourseType.GEN_ED,
                true, true, 0, false, 0, null);
    }

    private static Course c(String id, String name, int credits, List<String> prereqs) {
        return new Course(id, name, credits, prereqs, CourseType.GEN_ED,
                true, true, 0, false, 0, null);
    }

    private static List<Course> list(Course... courses) {
        return Collections.unmodifiableList(Arrays.asList(courses));
    }
}