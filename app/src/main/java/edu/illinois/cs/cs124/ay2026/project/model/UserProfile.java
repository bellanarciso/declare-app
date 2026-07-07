package edu.illinois.cs.cs124.ay2026.project.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserProfile implements Serializable {

    private final String startTerm;            // "Fall" or "Spring"
    private final int startYear;               // e.g. 2024
    private final Set<Goal> goals;
    private final Set<String> interests;
    private final Map<String, Integer> apScores;         // ApExam.name() -> score (1-5)
    private final Map<String, Integer> ibScores;         // IbExam.name() -> score (1-7)
    private final List<String> dualCreditCourseIds;      // UIUC course IDs from dual credit
    private final String selectedConcentration;
    private final boolean preferOnlineGenEds;

    public UserProfile(String startTerm, int startYear, Set<Goal> goals, Set<String> interests,
            Map<String, Integer> apScores, Map<String, Integer> ibScores,
            List<String> dualCreditCourseIds, String selectedConcentration,
            boolean preferOnlineGenEds) {
        this.startTerm = startTerm;
        this.startYear = startYear;
        this.goals = goals;
        this.interests = interests;
        this.apScores = apScores != null ? apScores : Collections.emptyMap();
        this.ibScores = ibScores != null ? ibScores : Collections.emptyMap();
        this.dualCreditCourseIds = dualCreditCourseIds != null
                ? dualCreditCourseIds : Collections.emptyList();
        this.selectedConcentration = selectedConcentration;
        this.preferOnlineGenEds = preferOnlineGenEds;
    }

    public String getStartTerm() {
        return startTerm;
    }

    public int getStartYear() {
        return startYear;
    }

    public Set<Goal> getGoals() {
        return goals;
    }

    public Set<String> getInterests() {
        return interests;
    }

    public boolean hasGoal(Goal goal) {
        return goals.contains(goal);
    }

    /** AP exam scores entered by the user. Key = ApExam.name(), value = score 1-5. */
    public Map<String, Integer> getApScores() {
        return apScores;
    }

    /** IB exam scores entered by the user. Key = IbExam.name(), value = score 1-7. */
    public Map<String, Integer> getIbScores() {
        return ibScores;
    }

    /** UIUC course IDs the student has credit for via dual enrollment. */
    public List<String> getDualCreditCourseIds() {
        return dualCreditCourseIds;
    }

    /** The full name of the selected concentration, or null if none chosen. */
    public String getSelectedConcentration() {
        return selectedConcentration;
    }

    /** True if the student prefers online sections for gen ed requirements. */
    public boolean prefersOnlineGenEds() {
        return preferOnlineGenEds;
    }

    /**
     * Total incoming credit hours from all qualifying AP, IB, and dual credit.
     * Used to compute standing bonus (every 30 credits ≈ one year of standing).
     */
    public int getIncomingCredits() {
        int total = 0;
        for (Map.Entry<String, Integer> entry : apScores.entrySet()) {
            try {
                ApExam exam = ApExam.valueOf(entry.getKey());
                if (entry.getValue() >= exam.getMinScoreForCredit()) {
                    total += exam.getCreditHours();
                }
            } catch (IllegalArgumentException ignored) {
                // Unrecognized exam key - skip
            }
        }
        for (Map.Entry<String, Integer> entry : ibScores.entrySet()) {
            try {
                IbExam exam = IbExam.valueOf(entry.getKey());
                if (entry.getValue() >= exam.getMinScoreForCredit()) {
                    total += exam.getCreditHours();
                }
            } catch (IllegalArgumentException ignored) {
                // Unrecognized exam key - skip
            }
        }
        // Assume 3 credit hours per dual credit course
        total += dualCreditCourseIds.size() * 3;
        return total;
    }
}