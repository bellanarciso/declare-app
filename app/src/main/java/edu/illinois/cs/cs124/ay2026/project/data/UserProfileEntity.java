package edu.illinois.cs.cs124.ay2026.project.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity for the user's saved profile data.
 *
 * This is a singleton table - there is always at most one row (id = 1).
 * All collection fields (goals, scores, interests) are stored as JSON strings
 * to keep the schema simple. The repository layer handles serialization.
 *
 * Use UserProfileDao.upsert() to save; it replaces the existing row if present.
 */
@Entity(tableName = "user_profile")
public class UserProfileEntity {

    @PrimaryKey
    public int id = 1; // Fixed ID - only one profile per device

    public String startTerm;            // "Fall" or "Spring"
    public int startYear;

    public String goalsJson;            // JSON array of Goal enum names, e.g. ["GRADUATE_EARLY"]
    public String apScoresJson;         // JSON object: ApExam.name() -> score (1-5)
    public String ibScoresJson;         // JSON object: IbExam.name() -> score (1-7)
    public String dualCreditCoursesJson; // JSON array of UIUC course IDs, e.g. ["CS 101"]
    public String interestsJson;        // JSON array of interest strings

    public boolean preferOnlineGenEds;
    public String selectedConcentration; // nullable
}
