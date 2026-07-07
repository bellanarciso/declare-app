package edu.illinois.cs.cs124.ay2026.project.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database for the UIUC Plan Builder app.
 *
 * Contains two tables:
 *  - user_profile: Singleton row with the user's profile data (goals, credits, interests).
 *  - saved_plans:  One row per saved academic plan.
 *
 * Access via AppDatabase.getInstance(context) - never create directly.
 */
@Database(
        entities = {UserProfileEntity.class, SavedPlanEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract UserProfileDao userProfileDao();
    public abstract SavedPlanDao savedPlanDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "plan_builder.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
