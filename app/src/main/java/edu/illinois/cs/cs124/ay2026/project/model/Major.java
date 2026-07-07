package edu.illinois.cs.cs124.ay2026.project.model;

import java.util.Collections;
import java.util.List;

public class Major {

    private final String name;
    private final String college;
    private final List<Course> courses;
    private final List<RequirementGroup> requirementGroups;
    private final List<Major> concentrations;

    public Major(String name, String college, List<Course> courses) {
        this(name, college, courses, Collections.emptyList(), Collections.emptyList());
    }

    public Major(String name, String college, List<Course> courses, List<Major> concentrations) {
        this(name, college, courses, Collections.emptyList(), concentrations);
    }

    public Major(String name, String college, List<Course> courses,
            List<RequirementGroup> requirementGroups, List<Major> concentrations) {
        this.name = name;
        this.college = college;
        this.courses = Collections.unmodifiableList(courses);
        this.requirementGroups = Collections.unmodifiableList(requirementGroups);
        this.concentrations = Collections.unmodifiableList(concentrations);
    }

    public String getName() {
        return name;
    }

    public String getCollege() {
        return college;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public List<RequirementGroup> getRequirementGroups() {
        return requirementGroups;
    }

    /** Concentrations nested under this major, e.g. "Acting Concentration" under Theatre BFA. */
    public List<Major> getConcentrations() {
        return concentrations;
    }

    public boolean hasConcentrations() {
        return !concentrations.isEmpty();
    }

    /**
     * Short display name derived from the full catalog name.
     * e.g. "Bachelor of Science in Engineering Major in Computer Science" -> "Computer Science, BS"
     * Falls back to the full name if no known pattern matches.
     */
    public String getDisplayName() {
        return shortName(name);
    }

    public static String shortName(String fullName) {
        // Some Gies program names spell out "plus" instead of using the symbol
        String name = fullName.replace(" plus ", " + ");

        String abbrev = degreeAbbrev(name);

        // Most common pattern: "... Major in <Program Name>"
        int majorInIdx = name.indexOf(" Major in ");
        if (majorInIdx >= 0 && !abbrev.isEmpty()) {
            return name.substring(majorInIdx + " Major in ".length()) + ", " + abbrev;
        }

        // Fallback: "Bachelor of X in <Program Name>" (e.g. "Bachelor of Science in Civil Engineering")
        int inIdx = name.indexOf(" in ");
        if (inIdx >= 0 && !abbrev.isEmpty()) {
            String after = name.substring(inIdx + " in ".length());
            // Guard against matching the "in" from "Bachelor of Science in Liberal Arts ... Major in ..."
            if (!after.contains(" Major in ")) {
                return after + ", " + abbrev;
            }
        }

        return name;
    }

    private static String degreeAbbrev(String name) {
        if (name.startsWith("Bachelor of Fine Arts"))              return "BFA";
        if (name.startsWith("Bachelor of Music"))                  return "BM";
        if (name.startsWith("Bachelor of Architecture"))           return "BArch";
        if (name.startsWith("Bachelor of Landscape Architecture")) return "BLA";
        if (name.startsWith("Bachelor of Social Work"))            return "BSW";
        // Catalog uses both "Science" and "Sciences" (plural) for LAS degrees
        if (name.startsWith("Bachelor of Science in Liberal Arts")
                || name.startsWith("Bachelor of Sciences in Liberal Arts")) return "BSLAS";
        if (name.startsWith("Bachelor of Arts in Liberal Arts"))   return "BALAS";
        if (name.startsWith("Bachelor of Science"))                return "BS";
        if (name.startsWith("Bachelor of Arts"))                   return "BA";
        return "";
    }

    /**
     * Short display label for a concentration, stripping the parent prefix.
     * e.g. "Bachelor of Arts ... Anthropology, Human Evolutionary Biology Concentration"
     *   -> "Human Evolutionary Biology"
     */
    public static String concentrationLabel(Major parent, Major concentration) {
        String suffix = concentration.getName().substring(parent.getName().length());
        // suffix is like ", Human Evolutionary Biology Concentration"
        if (suffix.startsWith(", ")) suffix = suffix.substring(2);
        if (suffix.endsWith(" Concentration")) suffix = suffix.substring(0, suffix.length() - " Concentration".length());
        return suffix;
    }
}