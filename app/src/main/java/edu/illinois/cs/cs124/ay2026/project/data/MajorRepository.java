package edu.illinois.cs.cs124.ay2026.project.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.model.Course;
import edu.illinois.cs.cs124.ay2026.project.model.CourseMetadata;
import edu.illinois.cs.cs124.ay2026.project.model.CourseType;
import edu.illinois.cs.cs124.ay2026.project.model.Major;
import edu.illinois.cs.cs124.ay2026.project.model.RequirementGroup;

/**
 * Loads major and course data from assets/courses.json.
 * After parsing, concentrations are grouped under their parent major by name prefix.
 */
public class MajorRepository {

    private static List<Major> cache = null;

    /** Returns all top-level majors (concentrations are nested inside each major). */
    public static List<Major> loadAll(Context context) {
        if (cache == null) {
            cache = parseJson(readAsset(context, "courses_2024.json"));
        }
        return cache;
    }

    /** Clears the in-memory cache so the next loadAll() re-parses with fresh API data. */
    public static void invalidateCache() {
        cache = null;
    }

    /**
     * Finds a major or concentration by name, matching both the full catalog name
     * and the short display name (e.g. "Computer Science, BS").
     */
    public static Major findByName(Context context, String name) {
        for (Major major : loadAll(context)) {
            if (major.getName().equals(name) || major.getDisplayName().equals(name)) return major;
            for (Major conc : major.getConcentrations()) {
                if (conc.getName().equals(name) || conc.getDisplayName().equals(name)) return conc;
            }
        }
        return null;
    }

    /** Returns the short display names of all top-level majors. */
    public static List<String> majorNames(Context context) {
        List<String> names = new ArrayList<>();
        for (Major major : loadAll(context)) {
            names.add(major.getDisplayName());
        }
        return names;
    }

    /** Returns a sorted list of unique canonical college names across all majors. */
    public static List<String> colleges(Context context) {
        Set<String> seen = new java.util.LinkedHashSet<>();
        for (Major major : loadAll(context)) {
            seen.addAll(effectiveColleges(major));
        }
        List<String> result = new ArrayList<>(seen);
        java.util.Collections.sort(result);
        return result;
    }

    /** Returns majors belonging to the given college, as short display names. */
    public static List<String> majorNamesByCollege(Context context, String college) {
        List<String> names = new ArrayList<>();
        for (Major major : loadAll(context)) {
            if (effectiveColleges(major).contains(college)) {
                names.add(major.getDisplayName());
            }
        }
        return names;
    }

    /**
     * Returns the canonical college names a major should appear under.
     * Handles slash-separated joint programs (e.g. "Grainger / ACES") by splitting them,
     * normalizes known name variants, and ensures CS + X programs also appear under Grainger.
     */
    private static List<String> effectiveColleges(Major major) {
        String raw = major.getCollege();
        if (raw == null || raw.trim().isEmpty()) return Collections.singletonList("Other");

        List<String> result = new ArrayList<>();
        for (String part : raw.split(" / ")) {
            String normalized = normalizeCollege(part.trim());
            if (!result.contains(normalized)) result.add(normalized);
        }

        // CS + X programs are jointly administered with Grainger; show them there too.
        if (major.getName().contains("Computer Science +")
                && !result.contains("Grainger College of Engineering")) {
            result.add("Grainger College of Engineering");
        }

        return result;
    }

    /**
     * Maps raw college name strings from the JSON to canonical display names.
     * The catalog data has several spelling variants of the same college.
     */
    private static String normalizeCollege(String raw) {
        if (raw == null || raw.isEmpty()) return "Other";
        switch (raw) {
            case "The Grainger College of Engineering":
            case "Siebel School of Computing and Data Science and Charles H. Sandage Department of Advertising":
                return "Grainger College of Engineering";
            case "College of Liberal Arts and Sciences":
            case "Liberal Arts and Sciences":
                return "College of Liberal Arts & Sciences";
            case "College of Fine and Applied Arts":
                return "College of Fine & Applied Arts";
            case "College of Agricultural, Consumer and Environmental Sciences":
            case "College of ACES":
                return "College of Agricultural, Consumer & Environmental Sciences";
            default:
                return raw;
        }
    }

    private static List<Major> parseJson(String json) {
        if (json == null) return Collections.emptyList();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray majorsArray = root.getJSONArray("majors");
            List<Major> flat = new ArrayList<>();

            for (int i = 0; i < majorsArray.length(); i++) {
                JSONObject majorObj = majorsArray.getJSONObject(i);
                String name = majorObj.getString("name");
                String college = majorObj.optString("college", "");
                JSONArray coursesArray = majorObj.getJSONArray("courses");

                List<Course> courses = new ArrayList<>();
                for (int j = 0; j < coursesArray.length(); j++) {
                    JSONObject c = coursesArray.getJSONObject(j);

                    String id = c.getString("id");
                    String courseName = c.getString("name");
                    int credits = c.getInt("credits");

                    JSONArray prereqsArray = c.getJSONArray("prereqs");
                    List<String> prereqs = new ArrayList<>();
                    for (int k = 0; k < prereqsArray.length(); k++) {
                        prereqs.add(prereqsArray.getString(k));
                    }

                    CourseType type = CourseType.valueOf(c.getString("type"));
                    boolean offeredFall = c.optBoolean("offeredFall", true);
                    boolean offeredSpring = c.optBoolean("offeredSpring", true);
                    int minYear = c.optInt("minYear", 0);
                    boolean cpaTrack = c.optBoolean("cpaTrack", false);
                    int suggestedYear = c.optInt("suggestedYear", 0);
                    String suggestedSemester = c.optString("suggestedSemester", null);
                    if (suggestedSemester != null && suggestedSemester.isEmpty()) {
                        suggestedSemester = null;
                    }

                    // Enrich with live API data when available; fall back to JSON values.
                    CourseMetadata meta = CourseDataManager.getMetadata(id);
                    String resolvedName = (meta != null) ? meta.getName() : courseName;
                    int resolvedCredits = (meta != null) ? meta.getCreditHours() : credits;
                    boolean resolvedFall = (meta != null) ? meta.isOfferedFall() : offeredFall;
                    boolean resolvedSpring = (meta != null) ? meta.isOfferedSpring() : offeredSpring;

                    courses.add(new Course(id, resolvedName, resolvedCredits, prereqs, type,
                            resolvedFall, resolvedSpring, minYear, cpaTrack, suggestedYear, suggestedSemester));
                }

                // Build a lookup map so option courses can reuse already-parsed Course objects
                Map<String, Course> courseById = new HashMap<>();
                for (Course c : courses) courseById.put(c.getId(), c);

                // Parse requirementGroups
                JSONArray groupsArray = majorObj.optJSONArray("requirementGroups");
                List<RequirementGroup> groups = new ArrayList<>();
                if (groupsArray != null) {
                    for (int g = 0; g < groupsArray.length(); g++) {
                        JSONObject grp = groupsArray.getJSONObject(g);
                        String grpId = grp.optString("id", "group_" + g);
                        String grpDesc = grp.optString("description", "");
                        int reqCredits = grp.optInt("requiredCredits", 0);
                        int reqCount = grp.optInt("requiredCount", 0);
                        int minLvl = grp.optInt("minLevel", 0);

                        JSONArray optArr = grp.optJSONArray("options");
                        List<Course> optionCourses = new ArrayList<>();
                        if (optArr != null) {
                            for (int o = 0; o < optArr.length(); o++) {
                                String optId = optArr.getString(o).trim().toUpperCase()
                                        .replaceAll("\\s+", " ");
                                if (courseById.containsKey(optId)) {
                                    optionCourses.add(courseById.get(optId));
                                } else {
                                    // Create a lightweight Course for this elective/gen-ed option
                                    CourseMetadata meta = CourseDataManager.getMetadata(optId);
                                    String optName = (meta != null) ? meta.getName() : optId;
                                    int optCr = (meta != null) ? meta.getCreditHours() : 3;
                                    boolean optFall = (meta == null) || meta.isOfferedFall();
                                    boolean optSpr = (meta == null) || meta.isOfferedSpring();
                                    CourseType optType = isGenEdGroup(grpDesc)
                                            ? CourseType.GEN_ED : CourseType.ELECTIVE;
                                    optionCourses.add(new Course(optId, optName, optCr,
                                            new ArrayList<>(), optType,
                                            optFall, optSpr, 0, false, 0, null, grpDesc));
                                }
                            }
                        }

                        // For gen-ed groups with no specific options, add standard UIUC courses
                        if (optionCourses.isEmpty() && isGenEdGroup(grpDesc)) {
                            optionCourses = genEdFallback(grpDesc);
                        }

                        if (!optionCourses.isEmpty() && (reqCredits > 0 || reqCount > 0)) {
                            groups.add(new RequirementGroup(grpId, grpDesc,
                                    reqCredits, reqCount, minLvl, optionCourses));
                        } else if (optionCourses.isEmpty()
                                && !GenEdCoursePool.optionsFor(grpId).isEmpty()) {
                            // Gen-ed placeholder group: no explicit options and no credit/count
                            // requirement, but maps to the university pool. Add with empty options
                            // so PlanGenerator can fill it from GenEdCoursePool.
                            groups.add(new RequirementGroup(grpId, grpDesc,
                                    reqCredits, reqCount, minLvl, Collections.emptyList()));
                        }
                    }
                }

                flat.add(new Major(name, college, courses, groups, Collections.emptyList()));
            }

            return groupConcentrations(flat);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Groups concentration programs under their parent major.
     * A concentration is identified by its name ending with " Concentration" and
     * starting with a parent major's name followed by ", ".
     * Returns only the top-level (non-concentration) majors.
     */
    private static List<Major> groupConcentrations(List<Major> flat) {
        // Map name -> index for fast lookup
        Map<String, Integer> indexByName = new HashMap<>();
        for (int i = 0; i < flat.size(); i++) {
            indexByName.put(flat.get(i).getName(), i);
        }

        // For each concentration, find its parent
        Map<Integer, List<Major>> concentrationsByParent = new HashMap<>();
        Set<Integer> concentrationIndices = new HashSet<>();

        for (int i = 0; i < flat.size(); i++) {
            Major m = flat.get(i);
            if (!m.getName().endsWith(" Concentration")) continue;

            // Walk through possible prefixes to find the longest matching parent
            String fullName = m.getName();
            int bestParentIdx = -1;
            int bestPrefixLen = 0;
            for (Map.Entry<String, Integer> entry : indexByName.entrySet()) {
                String parentName = entry.getKey();
                String prefix = parentName + ", ";
                if (fullName.startsWith(prefix) && prefix.length() > bestPrefixLen) {
                    bestPrefixLen = prefix.length();
                    bestParentIdx = entry.getValue();
                }
            }

            if (bestParentIdx >= 0 && bestParentIdx != i) {
                concentrationIndices.add(i);
                concentrationsByParent
                        .computeIfAbsent(bestParentIdx, k -> new ArrayList<>())
                        .add(m);
            }
        }

        // Rebuild top-level list, attaching concentrations to their parents
        List<Major> topLevel = new ArrayList<>();
        for (int i = 0; i < flat.size(); i++) {
            if (concentrationIndices.contains(i)) continue;
            Major m = flat.get(i);
            List<Major> concs = concentrationsByParent.getOrDefault(i, Collections.emptyList());
            topLevel.add(new Major(m.getName(), m.getCollege(), m.getCourses(),
                    m.getRequirementGroups(), concs));
        }
        return topLevel;
    }

    private static boolean isGenEdGroup(String description) {
        String d = description.toLowerCase();
        return d.contains("gen ed") || d.contains("general education")
                || d.contains("composition") || d.contains("quantitative reasoning")
                || d.contains("natural science") || d.contains("social science")
                || d.contains("humanities") || d.contains("cultural studies")
                || d.contains("western civilization") || d.contains("non-western");
    }

    /**
     * Returns a small set of commonly-taken UIUC courses for a gen-ed category
     * when the catalog didn't specify specific options.
     */
    private static List<Course> genEdFallback(String description) {
        String d = description.toLowerCase();
        List<String[]> entries = new ArrayList<>(); // {id, label}

        if (d.contains("composition")) {
            entries.add(new String[]{"RHET 105", description});
        } else if (d.contains("quantitative")) {
            entries.add(new String[]{"MATH 115", description});
            entries.add(new String[]{"MATH 220", description});
            entries.add(new String[]{"STAT 100", description});
        } else if (d.contains("natural science")) {
            entries.add(new String[]{"CHEM 102", description});
            entries.add(new String[]{"PHYS 100", description});
            entries.add(new String[]{"BIOL 100", description});
            entries.add(new String[]{"EPS 101",  description});
        } else if (d.contains("social") || d.contains("behavioral")) {
            entries.add(new String[]{"PSYC 100", description});
            entries.add(new String[]{"SOC 100",  description});
            entries.add(new String[]{"ECON 102", description});
            entries.add(new String[]{"ANTH 101", description});
        } else if (d.contains("humanit") || d.contains("arts")) {
            entries.add(new String[]{"PHIL 101", description});
            entries.add(new String[]{"ENGL 110", description});
            entries.add(new String[]{"ART 100",  description});
            entries.add(new String[]{"MUS 133",  description});
        } else if (d.contains("non-western") || d.contains("nonwestern")) {
            entries.add(new String[]{"ANTH 101", description});
            entries.add(new String[]{"GEOG 101", description});
        } else if (d.contains("cultural") || d.contains("western")) {
            entries.add(new String[]{"HIST 171", description});
            entries.add(new String[]{"HIST 172", description});
            entries.add(new String[]{"PHIL 101", description});
        }

        List<Course> result = new ArrayList<>();
        for (String[] entry : entries) {
            String id = entry[0];
            String label = entry[1];
            CourseMetadata meta = CourseDataManager.getMetadata(id);
            String optName = (meta != null) ? meta.getName() : id;
            int optCr   = (meta != null) ? meta.getCreditHours() : 3;
            boolean optFall = (meta == null) || meta.isOfferedFall();
            boolean optSpr  = (meta == null) || meta.isOfferedSpring();
            result.add(new Course(id, optName, optCr, new ArrayList<>(), CourseType.GEN_ED,
                    optFall, optSpr, 0, false, 0, null, label));
        }
        return result;
    }

    private static String readAsset(Context context, String filename) {
        try (InputStream is = context.getAssets().open(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
