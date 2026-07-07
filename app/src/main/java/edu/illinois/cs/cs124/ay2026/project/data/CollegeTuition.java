package edu.illinois.cs.cs124.ay2026.project.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 2025-2026 UIUC undergraduate tuition rates (annual) by college.
 * Source: registrar.illinois.edu / cost.illinois.edu
 */
public class CollegeTuition {

    public final double inState;
    public final double outOfState;

    private CollegeTuition(double inState, double outOfState) {
        this.inState = inState;
        this.outOfState = outOfState;
    }

    // Annual tuition by college display name (as stored in courses_2024.json)
    private static final Map<String, CollegeTuition> RATES = new HashMap<>();

    static {
        CollegeTuition base       = new CollegeTuition(12_992, 33_344);
        CollegeTuition engineering = new CollegeTuition(18_372, 41_444);
        CollegeTuition business   = new CollegeTuition(18_372, 38_724);
        CollegeTuition faa        = new CollegeTuition(14_620, 34_972);
        CollegeTuition economics  = new CollegeTuition(15_586, 35_938);

        RATES.put("The Grainger College of Engineering", engineering);
        RATES.put("Grainger College of Engineering", engineering);
        RATES.put("Gies College of Business", business);
        RATES.put("College of Fine and Applied Arts", faa);
        // Economics and Econometrics majors within LAS carry a differential
        RATES.put("College of Liberal Arts & Sciences (Economics)", economics);

        // All others use the base rate
        RATES.put("College of Liberal Arts & Sciences", base);
        RATES.put("College of Agricultural, Consumer & Environmental Sciences", base);
        RATES.put("College of Education", base);
        RATES.put("College of Applied Health Sciences", base);
        RATES.put("College of Media", base);
        RATES.put("School of Information Sciences", base);
    }

    private static final CollegeTuition BASE = new CollegeTuition(12_992, 33_344);

    public static CollegeTuition forCollege(String collegeName) {
        if (collegeName == null) return BASE;
        CollegeTuition rate = RATES.get(collegeName);
        return rate != null ? rate : BASE;
    }
}