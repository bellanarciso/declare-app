package edu.illinois.cs.cs124.ay2026.project.model;

import java.util.Arrays;
import java.util.List;

/**
 * IB Higher Level (HL) exams recognized by UIUC for transfer credit.
 * IB scores range from 1-7; most UIUC credit requires a minimum of 5.
 */
public enum IbExam {

    // Math & Sciences
    MATH_AA_HL("IB Mathematics: Analysis & Approaches HL", 5, 4, "MATH 221"),
    MATH_AI_HL("IB Mathematics: Applications & Interpretation HL", 5, 3),
    PHYSICS_HL("IB Physics HL", 5, 4),
    CHEMISTRY_HL("IB Chemistry HL", 5, 3, "CHEM 102"),
    BIOLOGY_HL("IB Biology HL", 5, 4, "IB 150"),
    ENVIRONMENTAL_SYS_HL("IB Environmental Systems & Societies HL", 5, 3),
    COMPUTER_SCI_HL("IB Computer Science HL", 5, 3),

    // Humanities & Social Sciences
    ECONOMICS_HL("IB Economics HL", 5, 3, "ECON 102"),
    PSYCHOLOGY_HL("IB Psychology HL", 5, 4, "PSYC 100"),
    HISTORY_HL("IB History HL", 5, 3),
    GEOGRAPHY_HL("IB Geography HL", 5, 3),
    PHILOSOPHY_HL("IB Philosophy HL", 5, 3),
    GLOBAL_POL_HL("IB Global Politics HL", 5, 3),
    BUSINESS_HL("IB Business Management HL", 5, 3),

    // Languages
    ENGLISH_A_LANG_LIT_HL("IB English A: Language & Literature HL", 5, 4, "RHET 105"),
    ENGLISH_A_LIT_HL("IB English A: Literature HL", 5, 4, "RHET 105"),
    SPANISH_B_HL("IB Spanish B HL", 5, 3),
    FRENCH_B_HL("IB French B HL", 5, 3),
    GERMAN_B_HL("IB German B HL", 5, 3),
    CHINESE_B_HL("IB Chinese B HL", 5, 3),
    JAPANESE_B_HL("IB Japanese B HL", 5, 3),

    // Arts
    VISUAL_ARTS_HL("IB Visual Arts HL", 5, 3),
    MUSIC_HL("IB Music HL", 5, 3),
    THEATRE_HL("IB Theatre HL", 5, 3),
    FILM_HL("IB Film HL", 5, 3);

    private final String displayName;
    private final int minScoreForCredit;
    private final int creditHours;
    private final String[] uiucEquivalents;

    IbExam(String displayName, int minScoreForCredit, int creditHours, String... uiucEquivalents) {
        this.displayName = displayName;
        this.minScoreForCredit = minScoreForCredit;
        this.creditHours = creditHours;
        this.uiucEquivalents = uiucEquivalents;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Minimum IB score (1-7) needed to receive UIUC credit. */
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