package edu.illinois.cs.cs124.ay2026.project.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.illinois.cs.cs124.ay2026.project.model.CourseMetadata;

/**
 * Persists course metadata to a JSON file in internal storage.
 * The cache is considered stale after CACHE_MAX_AGE_MS (7 days).
 */
public class CourseMetadataCache {

    private static final String CACHE_FILE = "course_metadata_cache.json";
    private static final long CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;

    public static boolean isStale(Context context) {
        File file = new File(context.getFilesDir(), CACHE_FILE);
        if (!file.exists()) return true;
        return System.currentTimeMillis() - file.lastModified() > CACHE_MAX_AGE_MS;
    }

    /** Loads cached course metadata. Returns null if the cache file does not exist. */
    public static Map<String, CourseMetadata> load(Context context) {
        File file = new File(context.getFilesDir(), CACHE_FILE);
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);

            JSONObject root = new JSONObject(sb.toString());
            Map<String, CourseMetadata> result = new HashMap<>();

            for (int i = 0; i < root.names().length(); i++) {
                String courseId = root.names().getString(i);
                JSONObject obj = root.getJSONObject(courseId);

                List<String> genEdCategories = new ArrayList<>();
                JSONArray cats = obj.optJSONArray("g");
                if (cats != null) {
                    for (int j = 0; j < cats.length(); j++) {
                        genEdCategories.add(cats.getString(j));
                    }
                }

                result.put(courseId, new CourseMetadata(
                        obj.getString("n"),
                        obj.getInt("c"),
                        obj.getBoolean("f"),
                        obj.getBoolean("s"),
                        obj.optString("d", ""),
                        genEdCategories));
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /** Saves course metadata to the cache file. */
    public static void save(Context context, Map<String, CourseMetadata> metadata) {
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, CourseMetadata> entry : metadata.entrySet()) {
                CourseMetadata m = entry.getValue();
                JSONObject obj = new JSONObject();
                obj.put("n", m.getName());
                obj.put("c", m.getCreditHours());
                obj.put("f", m.isOfferedFall());
                obj.put("s", m.isOfferedSpring());
                obj.put("d", m.getDescription());

                JSONArray cats = new JSONArray();
                for (String cat : m.getGenEdCategories()) cats.put(cat);
                obj.put("g", cats);

                root.put(entry.getKey(), obj);
            }

            File file = new File(context.getFilesDir(), CACHE_FILE);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(root.toString());
            }
        } catch (Exception ignored) {
        }
    }
}
