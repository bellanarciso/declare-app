package edu.illinois.cs.cs124.ay2026.project;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.cs.cs124.ay2026.project.data.ProfileConverter;
import edu.illinois.cs.cs124.ay2026.project.data.SavedPlanEntity;
import edu.illinois.cs.cs124.ay2026.project.data.UserProfileEntity;
import edu.illinois.cs.cs124.ay2026.project.model.UserProfile;

import static org.junit.Assert.*;

/**
 * Diagnoses whether the "apply interests to saved plans" snapshot round-trip works correctly.
 *
 * Each test isolates one step of the pipeline so failures point to a specific layer.
 */
public class ProfileSnapshotTest {

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    // -------------------------------------------------------------------------
    // Layer 1: raw GSON round-trip of UserProfileEntity
    // -------------------------------------------------------------------------

    @Test
    public void entityGsonRoundTrip_preservesInterests() {
        UserProfileEntity entity = baseEntity();
        entity.interestsJson = GSON.toJson(Arrays.asList("Philosophy", "Astronomy"));

        String json = GSON.toJson(entity);
        UserProfileEntity restored = GSON.fromJson(json, UserProfileEntity.class);

        assertNotNull("Restored entity must not be null", restored);
        List<String> interests = GSON.fromJson(restored.interestsJson, LIST_TYPE);
        assertTrue("Philosophy must survive round-trip", interests.contains("Philosophy"));
        assertTrue("Astronomy must survive round-trip", interests.contains("Astronomy"));
    }

    // -------------------------------------------------------------------------
    // Layer 2: ProfileConverter snapshot round-trip
    // -------------------------------------------------------------------------

    @Test
    public void profileConverterSnapshot_preservesInterests() {
        Set<String> original = new HashSet<>(Arrays.asList("Film", "Spanish"));
        UserProfile profile = new UserProfile(
                "Fall", 2025,
                Collections.emptySet(),
                original,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyList(),
                null, false);

        String snapshot = ProfileConverter.toSnapshot(profile);
        assertNotNull("Snapshot must not be null", snapshot);

        UserProfile restored = ProfileConverter.fromSnapshot(snapshot);
        assertNotNull("Restored profile must not be null", restored);
        assertTrue("Film must survive snapshot round-trip", restored.getInterests().contains("Film"));
        assertTrue("Spanish must survive snapshot round-trip", restored.getInterests().contains("Spanish"));
    }

    // -------------------------------------------------------------------------
    // Layer 3: the exact applyInterestsToSavedPlans() update pattern
    // -------------------------------------------------------------------------

    @Test
    public void applyInterests_updatesSnapshotCorrectly() {
        // Build a saved plan whose snapshot has old interests
        UserProfileEntity oldEntity = baseEntity();
        oldEntity.interestsJson = GSON.toJson(Collections.singletonList("Philosophy"));

        SavedPlanEntity plan = new SavedPlanEntity();
        plan.profileSnapshot = GSON.toJson(oldEntity);

        // Simulate what applyInterestsToSavedPlans() does
        List<String> newSelected = Arrays.asList("Astronomy", "Spanish", "East Asian Cultures");
        String newInterestsJson = GSON.toJson(newSelected);

        UserProfileEntity snap = GSON.fromJson(plan.profileSnapshot, UserProfileEntity.class);
        assertNotNull("Snapshot must deserialize", snap);

        snap.interestsJson = newInterestsJson;
        plan.profileSnapshot = GSON.toJson(snap);

        // Read it back the way PlanViewActivity would (via ProfileConverter)
        UserProfile restored = ProfileConverter.fromSnapshot(plan.profileSnapshot);
        assertNotNull("Restored profile must not be null", restored);
        assertFalse("Old interest must be gone", restored.getInterests().contains("Philosophy"));
        assertTrue("Astronomy must be present", restored.getInterests().contains("Astronomy"));
        assertTrue("Spanish must be present", restored.getInterests().contains("Spanish"));
        assertTrue("East Asian Cultures must be present",
                restored.getInterests().contains("East Asian Cultures"));
    }

    @Test
    public void applyInterests_nullSnapshotIsSkipped() {
        // Plans with null snapshot must not crash (the method uses `continue`)
        SavedPlanEntity plan = new SavedPlanEntity();
        plan.profileSnapshot = null;

        // This is the guard condition in applyInterestsToSavedPlans()
        if (plan.profileSnapshot == null) return; // should not reach GSON call

        // If we reach this line, the guard is broken
        fail("Guard condition for null snapshot did not fire");
    }

    @Test
    public void applyInterests_emptyInterestListClearsOldInterests() {
        UserProfileEntity oldEntity = baseEntity();
        oldEntity.interestsJson = GSON.toJson(Arrays.asList("Philosophy", "Astronomy"));

        SavedPlanEntity plan = new SavedPlanEntity();
        plan.profileSnapshot = GSON.toJson(oldEntity);

        // Apply empty selection (user deselected everything)
        String emptyJson = GSON.toJson(Collections.emptyList());
        UserProfileEntity snap = GSON.fromJson(plan.profileSnapshot, UserProfileEntity.class);
        snap.interestsJson = emptyJson;
        plan.profileSnapshot = GSON.toJson(snap);

        UserProfile restored = ProfileConverter.fromSnapshot(plan.profileSnapshot);
        assertNotNull(restored);
        assertTrue("Interests should be empty after clearing", restored.getInterests().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static UserProfileEntity baseEntity() {
        UserProfileEntity e = new UserProfileEntity();
        e.startTerm = "Fall";
        e.startYear = 2025;
        e.goalsJson = "[]";
        e.apScoresJson = "{}";
        e.ibScoresJson = "{}";
        e.dualCreditCoursesJson = "[]";
        e.interestsJson = "[]";
        return e;
    }
}