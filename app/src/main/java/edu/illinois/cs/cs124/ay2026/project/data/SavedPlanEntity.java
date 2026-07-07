package edu.illinois.cs.cs124.ay2026.project.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing one saved academic plan.
 *
 * The plan is regenerated on demand using the profileSnapshot - we store the profile
 * at the time of saving rather than the generated schedule itself. This means:
 * 1. The plan view always reflects the original inputs, even if the user changes their profile later.
 * 2. We don't need to serialize the full semester/course structure to the database.
 */
@Entity(tableName = "saved_plans")
public class SavedPlanEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;                  // User-given name, e.g. "CS + Math double major"
    public String majorName;             // Primary major name
    public String secondaryMajorName;    // nullable - set if user added a second major
    public String profileSnapshot;       // JSON of UserProfile at the time this plan was saved
    public long createdAt;               // System.currentTimeMillis() when saved
}
