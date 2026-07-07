package edu.illinois.cs.cs124.ay2026.project.model;

import java.util.Arrays;
import java.util.List;

/**
 * AP exams recognized by UIUC for transfer credit.
 * Each entry includes the minimum score required, total credit hours awarded,
 * and the specific UIUC course IDs the exam satisfies (if any).
 */
public enum ApExam {

    // Math & Sciences
    CALC_AB("AP Calculus AB", 3, 5, "MATH 220"),
    CALC_BC("AP Calculus BC", 3, 10, "MATH 221", "MATH 231"),
    PRECALCULUS("AP Precalculus", 3, 3),
    STATISTICS("AP Statistics", 3, 3, "STAT 100"),
    CHEMISTRY("AP Chemistry", 4, 3, "CHEM 102"),
    BIOLOGY("AP Biology", 4, 4, "IB 150"),
    ENVIRONMENTAL_SCI("AP Environmental Science", 3, 3),
    PHYSICS_1("AP Physics 1", 4, 4),
    PHYSICS_2("AP Physics 2", 4, 4),
    PHYSICS_C_MECH("AP Physics C: Mechanics", 4, 4, "PHYS 211"),
    PHYSICS_C_EM("AP Physics C: E&M", 4, 4, "PHYS 212"),

    // Computer Science
    CS_A("AP Computer Science A", 3, 3, "CS 101"),
    CS_PRINCIPLES("AP Computer Science Principles", 3, 3),

    // English
    ENGLISH_LANG("AP English Language & Composition", 3, 4, "RHET 105"),
    ENGLISH_LIT("AP English Literature & Composition", 3, 4, "RHET 105"),

    // Social Sciences & History
    MACROECONOMICS("AP Macroeconomics", 3, 3, "ECON 102"),
    MICROECONOMICS("AP Microeconomics", 3, 3, "ECON 103"),
    PSYCHOLOGY("AP Psychology", 3, 4, "PSYC 100"),
    US_HISTORY("AP US History", 3, 6),
    WORLD_HISTORY("AP World History: Modern", 3, 3),
    US_GOVERNMENT("AP US Gov & Politics", 3, 3),
    COMPARATIVE_GOV("AP Comparative Gov & Politics", 3, 3),
    HUMAN_GEOGRAPHY("AP Human Geography", 3, 3),

    // Arts
    ART_HISTORY("AP Art History", 3, 3),
    MUSIC_THEORY("AP Music Theory", 3, 3),

    // Languages
    CHINESE_LANG("AP Chinese Language & Culture", 3, 3),
    FRENCH_LANG("AP French Language & Culture", 3, 3),
    GERMAN_LANG("AP German Language & Culture", 3, 3),
    ITALIAN_LANG("AP Italian Language & Culture", 3, 3),
    JAPANESE_LANG("AP Japanese Language & Culture", 3, 3),
    LATIN("AP Latin", 3, 3),
    SPANISH_LANG("AP Spanish Language & Culture", 3, 3),
    SPANISH_LIT("AP Spanish Literature & Culture", 3, 3),

    // Capstone
    SEMINAR("AP Seminar", 3, 3),
    RESEARCH("AP Research", 3, 3);

    private final String displayName;
    private final int minScoreForCredit;
    private final int creditHours;
    private final String[] uiucEquivalents;

    ApExam(String displayName, int minScoreForCredit, int creditHours, String... uiucEquivalents) {
        this.displayName = displayName;
        this.minScoreForCredit = minScoreForCredit;
        this.creditHours = creditHours;
        this.uiucEquivalents = uiucEquivalents;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Minimum AP score (1-5) needed to receive UIUC credit. */
    public int getMinScoreForCredit() {
        return minScoreForCredit;
    }

    /** Total credit hours awarded at UIUC for a qualifying score. */
    public int getCreditHours() {
        return creditHours;
    }

    /**
     * Specific UIUC course IDs this exam satisfies (may be empty if only gen-credit is awarded).
     * These IDs are used to mark courses as pre-completed in the plan generator.
     */
    public List<String> getUiucEquivalents() {
        return Arrays.asList(uiucEquivalents);
    }
}