package edu.illinois.cs.cs124.ay2026.project.model;

import java.util.Collections;
import java.util.List;

/**
 * A requirement group from the catalog - e.g. "choose 4 courses at the 400 level from this list"
 * or "choose 12 credit hours from the following technical electives."
 *
 * Either requiredCredits or requiredCount will be non-zero (not both).
 */
public class RequirementGroup {

    private final String id;
    private final String description;
    private final int requiredCredits; // 0 if using count
    private final int requiredCount;   // 0 if using credits
    private final int minLevel;        // minimum course number (e.g. 400), 0 if no restriction
    private final List<Course> options;

    public RequirementGroup(String id, String description, int requiredCredits,
            int requiredCount, int minLevel, List<Course> options) {
        this.id = id;
        this.description = description;
        this.requiredCredits = requiredCredits;
        this.requiredCount = requiredCount;
        this.minLevel = minLevel;
        this.options = Collections.unmodifiableList(options);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public int getRequiredCredits() {
        return requiredCredits;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public List<Course> getOptions() {
        return options;
    }
}
