package edu.illinois.cs.cs124.ay2026.project.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import edu.illinois.cs.cs124.ay2026.project.model.CourseMetadata;

// Handles live course metadata from the UIUC CIS API. Call initialize() at
// startup to load the on-disk cache, refreshIfNeeded() to pull fresh data in
// the background, and getMetadata() to look a course up.
public class CourseDataManager {

    // Course IDs that are real UIUC courses (subject letters + numeric number only).
    // Placeholder IDs like "ELEC 001", "HUM 1xx", "FIN 4xx-1" are excluded.
    private static final Pattern REAL_COURSE_ID = Pattern.compile("^[A-Z]+ \\d+$");

    // In-memory cache of fetched metadata.
    private static volatile Map<String, CourseMetadata> metadataMap = null;

    // Prevents duplicate concurrent refreshes.
    private static final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    /**
     * Loads the on-disk cache into memory. Must be called before getMetadata().
     * This is fast (file read) and safe to call on the main thread.
     */
    public static void initialize(Context context) {
        Map<String, CourseMetadata> cached = CourseMetadataCache.load(context);
        if (cached != null) {
            metadataMap = cached;
        }
    }

    /**
     * Returns cached metadata for a course, or null if not yet available.
     * Falls back gracefully - callers should handle a null result by using
     * the local JSON data instead.
     */
    public static CourseMetadata getMetadata(String courseId) {
        Map<String, CourseMetadata> map = metadataMap;
        return map != null ? map.get(courseId) : null;
    }

    /**
     * Fetches fresh course data from the CIS API if the cache is stale.
     * Runs entirely on background threads; calls onComplete on the main thread when done.
     *
     * @param courseIds all course IDs in the app (placeholder IDs are filtered automatically)
     * @param onComplete called on the main thread when the refresh finishes (may be null)
     */
    public static void refreshIfNeeded(Context context, Set<String> courseIds, Runnable onComplete) {
        if (!CourseMetadataCache.isStale(context)) {
            if (onComplete != null) onComplete.run();
            return;
        }

        if (!refreshInProgress.compareAndSet(false, true)) {
            // A refresh is already running.
            return;
        }

        Handler mainHandler = new Handler(Looper.getMainLooper());
        ExecutorService executor = Executors.newFixedThreadPool(4);

        new Thread(() -> {
            try {
                Map<String, CourseMetadata> newMap = new ConcurrentHashMap<>();

                // Filter to real course IDs only.
                List<String> realIds = new ArrayList<>();
                for (String id : courseIds) {
                    if (REAL_COURSE_ID.matcher(id).matches()) {
                        realIds.add(id);
                    }
                }

                // Fetch each course in parallel.
                List<Future<?>> futures = new ArrayList<>();
                for (String id : realIds) {
                    futures.add(executor.submit(() -> {
                        CourseMetadata meta = CourseApiClient.fetchCourse(id);
                        if (meta != null) {
                            newMap.put(id, meta);
                        }
                    }));
                }

                for (Future<?> f : futures) {
                    try { f.get(); } catch (Exception ignored) { }
                }

                if (!newMap.isEmpty()) {
                    metadataMap = newMap;
                    CourseMetadataCache.save(context, newMap);
                }
            } finally {
                executor.shutdown();
                refreshInProgress.set(false);
                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
            }
        }).start();
    }
}
