package edu.illinois.cs.cs124.ay2026.project.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.model.Goal;
import edu.illinois.cs.cs124.ay2026.project.model.UserProfile;

/**
 * Converts between the UserProfile domain model and its Room entity representation,
 * and handles JSON serialization for plan snapshots.
 *
 * Collection fields (goals, scores, interests) are stored as JSON strings in the entity
 * so the Room schema stays flat and easy to migrate.
 */
public final class ProfileConverter {

    private static final Gson GSON = new Gson();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final Type STRING_INT_MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();

    private ProfileConverter() { }

    /** Converts a Room entity to the UserProfile domain model. Returns null if entity is null. */
    public static UserProfile fromEntity(UserProfileEntity entity) {
        if (entity == null) return null;

        Set<Goal> goals = EnumSet.noneOf(Goal.class);
        List<String> goalStrings = GSON.fromJson(entity.goalsJson, STRING_LIST_TYPE);
        if (goalStrings != null) {
            for (String s : goalStrings) {
                try { goals.add(Goal.valueOf(s)); } catch (IllegalArgumentException ignored) { }
            }
        }

        Map<String, Integer> apScores = parseMap(entity.apScoresJson);
        Map<String, Integer> ibScores = parseMap(entity.ibScoresJson);

        List<String> dualCredit = parseList(entity.dualCreditCoursesJson);
        List<String> interestList = parseList(entity.interestsJson);
        Set<String> interests = new HashSet<>(interestList);

        String term = entity.startTerm != null ? entity.startTerm : "Fall";
        int year = entity.startYear > 0
                ? entity.startYear
                : Calendar.getInstance().get(Calendar.YEAR);

        return new UserProfile(term, year, goals, interests,
                apScores, ibScores, dualCredit,
                entity.selectedConcentration, entity.preferOnlineGenEds);
    }

    /** Converts a UserProfile domain model to its Room entity form. */
    public static UserProfileEntity toEntity(UserProfile profile) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.startTerm = profile.getStartTerm();
        entity.startYear = profile.getStartYear();

        List<String> goalNames = new ArrayList<>();
        for (Goal g : profile.getGoals()) goalNames.add(g.name());
        entity.goalsJson = GSON.toJson(goalNames);

        entity.apScoresJson = GSON.toJson(profile.getApScores());
        entity.ibScoresJson = GSON.toJson(profile.getIbScores());
        entity.dualCreditCoursesJson = GSON.toJson(profile.getDualCreditCourseIds());
        entity.interestsJson = GSON.toJson(new ArrayList<>(profile.getInterests()));
        entity.preferOnlineGenEds = profile.prefersOnlineGenEds();
        entity.selectedConcentration = profile.getSelectedConcentration();
        return entity;
    }

    /** Serializes a UserProfile to a JSON string for embedding in a SavedPlanEntity snapshot. */
    public static String toSnapshot(UserProfile profile) {
        return GSON.toJson(toEntity(profile));
    }

    /** Deserializes a UserProfile from a plan snapshot JSON string. */
    public static UserProfile fromSnapshot(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isEmpty()) return null;
        UserProfileEntity entity = GSON.fromJson(snapshotJson, UserProfileEntity.class);
        return fromEntity(entity);
    }

    /** Serializes a list of goal name strings to a JSON array string. */
    public static String goalsToJson(List<String> goalNames) {
        return GSON.toJson(goalNames);
    }

    private static Map<String, Integer> parseMap(String json) {
        Map<String, Integer> map = GSON.fromJson(json, STRING_INT_MAP_TYPE);
        return map != null ? map : new HashMap<>();
    }

    private static List<String> parseList(String json) {
        List<String> list = GSON.fromJson(json, STRING_LIST_TYPE);
        return list != null ? list : new ArrayList<>();
    }
}
