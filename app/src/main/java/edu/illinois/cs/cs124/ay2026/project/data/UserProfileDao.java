package edu.illinois.cs.cs124.ay2026.project.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserProfileDao {

    /** Returns the stored profile, or null if the user hasn't saved one yet. */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    UserProfileEntity getProfile();

    /**
     * Saves the profile. Because UserProfileEntity always has id = 1,
     * REPLACE effectively acts as an upsert - updating the existing row
     * rather than inserting a duplicate.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserProfileEntity profile);

    /** Returns the number of rows in the table. Should always be 0 or 1. */
    @Query("SELECT COUNT(*) FROM user_profile")
    int getCount();
}
