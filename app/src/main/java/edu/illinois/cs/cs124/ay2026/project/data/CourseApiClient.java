package edu.illinois.cs.cs124.ay2026.project.data;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import edu.illinois.cs.cs124.ay2026.project.model.CourseMetadata;

/**
 * Fetches individual course metadata from the UIUC Course Information Suite (CIS) API.
 * API docs: https://courses.illinois.edu/cisdocs/api
 *
 * All methods are blocking and must be called from a background thread.
 */
public class CourseApiClient {

    private static final String BASE_URL = buildBaseUrl();

    private static String buildBaseUrl() {
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        // Clamp to minimum 2024 - earlier catalog years may be missing courses.
        year = Math.max(year, 2024);
        return "https://courses.illinois.edu/cisapp/explorer/catalog/" + year + "/spring/";
    }
    private static final int TIMEOUT_MS = 10_000;

    /**
     * Fetches metadata for a single course.
     *
     * @param courseId e.g. "CS 124"
     * @return CourseMetadata, or null if the course is not found or a network error occurs
     */
    public static CourseMetadata fetchCourse(String courseId) {
        String[] parts = courseId.trim().split(" ");
        if (parts.length != 2) return null;
        String subject = parts[0];
        String number = parts[1];

        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + subject + "/" + number + ".xml");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) return null;

            try (InputStream is = conn.getInputStream()) {
                return parseCourseXml(is);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static CourseMetadata parseCourseXml(InputStream is) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(is, "UTF-8");

        String name = null;
        int creditHours = 3;
        String description = null;
        boolean offeredFall = false;
        boolean offeredSpring = false;
        List<String> genEdCategories = new ArrayList<>();

        boolean inTermsOffered = false;
        boolean inGenEdCategories = false;
        String currentTag = null;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    currentTag = parser.getName();
                    if ("termsOffered".equals(currentTag)) {
                        inTermsOffered = true;
                    } else if ("genEdCategories".equals(currentTag)) {
                        inGenEdCategories = true;
                    } else if (inGenEdCategories && "category".equals(currentTag)) {
                        String catId = parser.getAttributeValue(null, "id");
                        if (catId != null && !catId.isEmpty()) {
                            genEdCategories.add(catId);
                        }
                    }
                    break;

                case XmlPullParser.END_TAG:
                    String endTag = parser.getName();
                    if ("termsOffered".equals(endTag)) inTermsOffered = false;
                    if ("genEdCategories".equals(endTag)) inGenEdCategories = false;
                    currentTag = null;
                    break;

                case XmlPullParser.TEXT:
                    String text = parser.getText().trim();
                    if (!text.isEmpty() && currentTag != null) {
                        if ("label".equals(currentTag) && name == null) {
                            name = text;
                        } else if ("creditHours".equals(currentTag)) {
                            creditHours = parseCreditHours(text);
                        } else if ("description".equals(currentTag) && description == null) {
                            description = text;
                        } else if (inTermsOffered && "course".equals(currentTag)) {
                            if (text.startsWith("Fall")) offeredFall = true;
                            if (text.startsWith("Spring")) offeredSpring = true;
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }

        if (name == null) return null;
        return new CourseMetadata(
                name,
                creditHours,
                offeredFall,
                offeredSpring,
                description != null ? description : "",
                genEdCategories);
    }

    /**
     * Parses credit hour strings from the API, e.g.:
     *   "3 hours."        -> 3
     *   "1 to 3 hours."   -> 3  (takes the maximum)
     *   "4 hours."        -> 4
     */
    static int parseCreditHours(String raw) {
        int last = 3;
        for (String token : raw.split("\\s+")) {
            try {
                last = Integer.parseInt(token);
            } catch (NumberFormatException ignored) {
            }
        }
        return last;
    }
}
