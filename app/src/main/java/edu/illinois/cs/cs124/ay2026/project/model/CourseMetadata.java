package edu.illinois.cs.cs124.ay2026.project.model;

import java.util.Collections;
import java.util.List;

/** Live course data fetched from the UIUC Course Information Suite API. */
public class CourseMetadata {

    private final String name;
    private final int creditHours;
    private final boolean offeredFall;
    private final boolean offeredSpring;
    private final String description;
    private final List<String> genEdCategories;

    public CourseMetadata(
            String name,
            int creditHours,
            boolean offeredFall,
            boolean offeredSpring,
            String description,
            List<String> genEdCategories) {
        this.name = name;
        this.creditHours = creditHours;
        this.offeredFall = offeredFall;
        this.offeredSpring = offeredSpring;
        this.description = description;
        this.genEdCategories = Collections.unmodifiableList(genEdCategories);
    }

    public String getName() { return name; }
    public int getCreditHours() { return creditHours; }
    public boolean isOfferedFall() { return offeredFall; }
    public boolean isOfferedSpring() { return offeredSpring; }
    public String getDescription() { return description; }
    public List<String> getGenEdCategories() { return genEdCategories; }
}
