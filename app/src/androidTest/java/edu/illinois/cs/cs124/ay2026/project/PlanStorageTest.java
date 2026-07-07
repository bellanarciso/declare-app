package edu.illinois.cs.cs124.ay2026.project;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import edu.illinois.cs.cs124.ay2026.project.data.AppDatabase;
import edu.illinois.cs.cs124.ay2026.project.data.SavedPlanDao;
import edu.illinois.cs.cs124.ay2026.project.data.SavedPlanEntity;
import edu.illinois.cs.cs124.ay2026.project.data.UserProfileDao;
import edu.illinois.cs.cs124.ay2026.project.data.UserProfileEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented tests for the Room database layer.
 *
 * Each test runs against a fresh in-memory database so tests are fully isolated.
 * To run: right-click this file in Android Studio -> Run 'PlanStorageTest'.
 * Requires a running emulator or connected device.
 */
@RunWith(AndroidJUnit4.class)
public class PlanStorageTest {

    private AppDatabase db;
    private UserProfileDao profileDao;
    private SavedPlanDao planDao;

    @Before
    public void createInMemoryDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        profileDao = db.userProfileDao();
        planDao = db.savedPlanDao();
    }

    @After
    public void closeDatabase() {
        db.close();
    }

    // -------------------------------------------------------------------------
    // UserProfile tests
    // -------------------------------------------------------------------------

    @Test
    public void profileIsNullBeforeFirstSave() {
        assertNull("Fresh database should return no profile", profileDao.getProfile());
    }

    @Test
    public void canSaveAndLoadProfile() {
        UserProfileEntity entity = buildProfile("Fall", 2024, false);
        profileDao.upsert(entity);

        UserProfileEntity loaded = profileDao.getProfile();

        assertNotNull(loaded);
        assertEquals("Fall", loaded.startTerm);
        assertEquals(2024, loaded.startYear);
        assertEquals("[]", loaded.goalsJson);
    }

    @Test
    public void updatingProfileOverwritesPreviousRow() {
        profileDao.upsert(buildProfile("Fall", 2024, false));
        profileDao.upsert(buildProfile("Spring", 2025, true));

        UserProfileEntity loaded = profileDao.getProfile();

        assertEquals("Spring", loaded.startTerm);
        assertEquals(2025, loaded.startYear);
        assertTrue(loaded.preferOnlineGenEds);
        // Upsert must not create a second row
        assertEquals(1, profileDao.getCount());
    }

    @Test
    public void profileAllFieldsRoundTrip() {
        UserProfileEntity entity = new UserProfileEntity();
        entity.startTerm = "Fall";
        entity.startYear = 2026;
        entity.goalsJson = "[\"GRADUATE_EARLY\",\"ADD_MINOR\"]";
        entity.apScoresJson = "{\"CALC_BC\":4,\"CHEMISTRY\":5}";
        entity.ibScoresJson = "{}";
        entity.dualCreditCoursesJson = "[\"CS 101\"]";
        entity.interestsJson = "[\"Technology & Computing\"]";
        entity.preferOnlineGenEds = true;
        entity.selectedConcentration = "Software Engineering";
        profileDao.upsert(entity);

        UserProfileEntity loaded = profileDao.getProfile();

        assertEquals("[\"GRADUATE_EARLY\",\"ADD_MINOR\"]", loaded.goalsJson);
        assertEquals("{\"CALC_BC\":4,\"CHEMISTRY\":5}", loaded.apScoresJson);
        assertEquals("[\"CS 101\"]", loaded.dualCreditCoursesJson);
        assertEquals("Software Engineering", loaded.selectedConcentration);
    }

    // -------------------------------------------------------------------------
    // SavedPlan tests
    // -------------------------------------------------------------------------

    @Test
    public void canSaveAndLoadAPlan() {
        long id = planDao.insert(buildPlan("My CS Plan", "Computer Science", null));

        SavedPlanEntity loaded = planDao.getById(id);

        assertNotNull(loaded);
        assertEquals("My CS Plan", loaded.name);
        assertEquals("Computer Science", loaded.majorName);
        assertNull(loaded.secondaryMajorName);
    }

    @Test
    public void planWithSecondaryMajorRoundTrips() {
        long id = planDao.insert(buildPlan("Dual Degree", "Computer Science", "Mathematics"));

        SavedPlanEntity loaded = planDao.getById(id);

        assertEquals("Computer Science", loaded.majorName);
        assertEquals("Mathematics", loaded.secondaryMajorName);
    }

    @Test
    public void getAllPlansReturnsEveryInsertedPlan() {
        planDao.insert(buildPlan("Plan A", "Computer Science", null));
        planDao.insert(buildPlan("Plan B", "Mathematics", null));
        planDao.insert(buildPlan("Plan C", "Physics", "Math"));

        List<SavedPlanEntity> all = planDao.getAll();

        assertEquals(3, all.size());
    }

    @Test
    public void getAllPlansIsOrderedNewestFirst() {
        planDao.insert(buildPlanAt("Oldest", "CS", 1_000L));
        planDao.insert(buildPlanAt("Middle", "Math", 2_000L));
        planDao.insert(buildPlanAt("Newest", "Physics", 3_000L));

        List<SavedPlanEntity> all = planDao.getAll();

        assertEquals("Newest", all.get(0).name);
        assertEquals("Oldest", all.get(2).name);
    }

    @Test
    public void deletePlanRemovesItFromDatabase() {
        long id = planDao.insert(buildPlan("To Delete", "CS", null));
        assertNotNull("Plan should exist before delete", planDao.getById(id));

        planDao.deleteById(id);

        assertNull("Plan should be gone after delete", planDao.getById(id));
        assertEquals(0, planDao.getAll().size());
    }

    @Test
    public void deletingOnePlanLeavesOthersIntact() {
        long idToDelete = planDao.insert(buildPlan("Delete Me", "CS", null));
        planDao.insert(buildPlan("Keep Me", "Math", null));

        planDao.deleteById(idToDelete);

        assertEquals(1, planDao.getAll().size());
        assertEquals("Keep Me", planDao.getAll().get(0).name);
    }

    @Test
    public void planRetainsSnapshotWhenProfileIsUpdated() {
        String originalSnapshot = "{\"startTerm\":\"Fall\",\"startYear\":2024}";
        long id = planDao.insert(buildPlanWithSnapshot("Snapshot Plan", "CS", originalSnapshot));

        // Simulate the user updating their profile later
        profileDao.upsert(buildProfile("Spring", 2025, false));

        // The plan's snapshot must reflect the profile at the time it was saved
        SavedPlanEntity loaded = planDao.getById(id);
        assertEquals(originalSnapshot, loaded.profileSnapshot);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserProfileEntity buildProfile(String term, int year, boolean preferOnline) {
        UserProfileEntity e = new UserProfileEntity();
        e.startTerm = term;
        e.startYear = year;
        e.goalsJson = "[]";
        e.apScoresJson = "{}";
        e.ibScoresJson = "{}";
        e.dualCreditCoursesJson = "[]";
        e.interestsJson = "[]";
        e.preferOnlineGenEds = preferOnline;
        e.selectedConcentration = null;
        return e;
    }

    private SavedPlanEntity buildPlan(String name, String major, String secondaryMajor) {
        return buildPlanAt(name, major, secondaryMajor, System.currentTimeMillis());
    }

    private SavedPlanEntity buildPlanAt(String name, String major, long createdAt) {
        return buildPlanAt(name, major, null, createdAt);
    }

    private SavedPlanEntity buildPlanAt(String name, String major, String secondaryMajor, long createdAt) {
        SavedPlanEntity e = new SavedPlanEntity();
        e.name = name;
        e.majorName = major;
        e.secondaryMajorName = secondaryMajor;
        e.profileSnapshot = "{}";
        e.createdAt = createdAt;
        return e;
    }

    private SavedPlanEntity buildPlanWithSnapshot(String name, String major, String snapshot) {
        SavedPlanEntity e = new SavedPlanEntity();
        e.name = name;
        e.majorName = major;
        e.secondaryMajorName = null;
        e.profileSnapshot = snapshot;
        e.createdAt = System.currentTimeMillis();
        return e;
    }
}
