package edu.illinois.cs.cs124.ay2026.project.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SavedPlanDao {

    /** Inserts a new plan and returns its auto-generated ID. */
    @Insert
    long insert(SavedPlanEntity plan);

    /** Returns all saved plans, newest first. */
    @Query("SELECT * FROM saved_plans ORDER BY createdAt DESC")
    List<SavedPlanEntity> getAll();

    /** Returns the plan with the given ID, or null if it doesn't exist. */
    @Query("SELECT * FROM saved_plans WHERE id = :id")
    SavedPlanEntity getById(long id);

    /** Updates an existing plan (matched by primary key). */
    @Update
    void update(SavedPlanEntity plan);

    /** Permanently deletes a plan by ID. */
    @Query("DELETE FROM saved_plans WHERE id = :id")
    void deleteById(long id);
}
