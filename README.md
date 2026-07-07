# Declare

**An Android app that generates personalized four-year academic plans for University of Illinois Urbana-Champaign students.**

Declare turns the messy, guesswork-heavy process of course planning into something a student can do in a few taps. You pick a major, answer a short questionnaire about your background and goals, and the app builds a semester-by-semester path to graduation that respects prerequisites, course offering terms, credit-hour limits, and university general-education requirements. You can save multiple plans, compare two side by side, and see how choices like graduating early or studying abroad change the cost and timeline.

Built as an independent project for CS 124 at UIUC.

---

## Why it exists

Existing student planning tools at UIUC are narrow - most cover only a handful of engineering majors. Advising appointments are short and infrequent, and the official degree audit system tells you *what* you still need without helping you *sequence* it. Declare covers **297 undergraduate programs across 21 colleges** from the 2024-2025 catalog, and it treats planning as a scheduling problem: given the requirements and the prerequisite graph, produce a valid ordering a real student could actually follow.

---

## What it does

**Guided onboarding and profile.** A first-run welcome screen leads into an onboarding flow and a questionnaire that captures the inputs that actually change a plan: intended major and college, starting term, AP and IB exam scores, dual-credit coursework, and goals such as graduating early, studying abroad, or following a CPA-eligibility track. AP/IB scores and dual credit are translated into their UIUC course equivalents and treated as already completed, so prerequisites downstream unlock correctly.

**Automatic plan generation.** From the profile and the selected major, the app produces a full eight-semester schedule. Under the hood it topologically sorts every required course by its prerequisite chain, then greedily places courses term by term - only scheduling a course when its prerequisites are met, it's offered that term, it fits under the credit cap, and the student has the class standing it requires. Free-choice general-education slots are filled from a curated university-wide pool.

**Interest-aware gen-eds.** Instead of dropping in arbitrary gen-eds, the questionnaire collects interest areas (from "Astronomy & Space Science" to "Gender & Sexuality Studies" to specific world languages). The generator scores candidate gen-ed courses against those interests and places the best matches first, so two students in the same major get genuinely different, personalized schedules.

**Saved plans and comparison.** Plans are named, stored locally, and listed for later. Any two can be opened in a side-by-side comparison view that lines up their semesters, making it easy to weigh a single major against a double major, or a standard timeline against an accelerated one.

**Financial view.** A dedicated financial screen models tuition by college using in-state and out-of-state presets, so a student can see the cost implications of their plan - including the savings from finishing a semester early.

**Live course metadata.** On top of the bundled catalog, the app enriches courses with fresh data pulled in the background from the UIUC Course Information System API, cached on-device and refreshed when stale, with graceful fallback to the local catalog when the network isn't available.

---

## Screens

| Screen | Purpose |
|--------|---------|
| Welcome | Compose-based entry point; routes to onboarding or straight to the home screen. |
| Onboarding & Questionnaire | Collects major, starting term, AP/IB/dual credit, interests, and goals. |
| Home | Bottom-navigation shell hosting the Plans, Profile, and Financial tabs. |
| New Plan | Major/college selection to kick off generation. |
| Plan View | The generated eight-semester grid, with per-course detail and prerequisite chains. |
| Compare | Two saved plans lined up semester by semester. |
| Financial | Tuition modeling by college with early-graduation savings. |

---

## Architecture

Declare is a native Android app, primarily Java (~6,000 lines of app code) with a Kotlin + Jetpack Compose welcome screen and theme layer. It follows a straightforward layered structure:

- **`model/`** - plain data types: `Course`, `Major`, `Semester`, `AcademicPlan`, `RequirementGroup`, `UserProfile`, `CourseType`, `Goal`, and the `ApExam` / `IbExam` credit-equivalency tables.
- **`data/`** - the data layer: `MajorRepository` (parses the bundled catalog JSON), `GenEdCoursePool` (the curated gen-ed course lists per category), `CourseDataManager` / `CourseApiClient` / `CourseMetadataCache` (live CIS API enrichment with on-disk caching), `CollegeTuition`, and a Room database (`AppDatabase`, DAOs, and entities) for saved plans and the user profile.
- **`PlanGenerator.java`** - the core engine: Kahn's-algorithm topological sort, greedy prerequisite-aware placement, interest scoring, requirement-group filling, and major-merging for double majors.
- **Activities & fragments** - `HomeActivity` hosts `PlansFragment`, `ProfileFragment`, and `FinancialFragment` behind a bottom nav; standalone activities handle onboarding, questionnaire, plan viewing, and comparison.
- **`adapter/`** - RecyclerView adapters for semesters, saved plans, and the major dropdown.

### The plan-generation algorithm

1. **Topological sort.** Build the prerequisite graph for all of a major's required courses and sort them (Kahn's algorithm) so nothing is scheduled before its prerequisites. Ties are broken by how rarely a course is offered - Fall-only courses are placed before every-term courses to avoid getting boxed in.
2. **Pre-complete incoming credit.** Convert AP/IB scores and dual credit into UIUC course equivalents and mark them complete up front, and translate every 30 incoming credits into a class-standing bonus.
3. **Greedy term-by-term placement.** Walk the eight semesters in order; in each, place major requirements, then interest-ranked gen-eds, then electives - subject to prerequisite completion, offering term, credit cap, per-type caps, and class-standing gates.
4. **Fill requirement groups.** After individually-named courses are placed, satisfy elective and gen-ed *groups* (e.g. "three technical electives," "one Western-culture course") from their option lists or the university-wide gen-ed pool, again respecting prerequisites and caps.
5. **Handle special goals.** Study-abroad reserves a term with a reduced cap; accelerated tracks raise the per-term major-requirement limit; the CPA track pulls in additional required courses.

---

## Data pipeline

The course catalog is compiled from UIUC's public course data rather than hand-typed. `scripts/fetch_courses.py` pulls per-course details from the UIUC Course Explorer XML API - course name, credit hours, Fall/Spring availability, and a best-effort prerequisite parse from the course description - and writes them out as structured JSON. That data is assembled into the catalog file the app reads at runtime (`app/src/main/assets/courses_2024.json`), which is checked in so the app builds and runs without any network fetch or API key.

---

## Tech stack

- **Language:** Java (app logic), Kotlin (Compose welcome screen + theme)
- **UI:** Android Views + Material Components for the main app; Jetpack Compose (Material 3) for the welcome flow; ConstraintLayout, RecyclerView, ViewPager2
- **Persistence:** Room (saved plans, user profile), SharedPreferences, on-disk JSON cache
- **Networking:** UIUC CIS / Course Explorer APIs over HTTPS, fetched on a background thread pool
- **Serialization:** Gson
- **Build:** Gradle (Kotlin DSL), `compileSdk` 35, `minSdk` 24, `targetSdk` 35
- **Data tooling:** Python (requests, ElementTree) for pulling Course Explorer data

---

## Testing

Unit tests (JUnit) and instrumented tests (AndroidX Test) cover the parts most likely to break silently:

- **`PlanGeneratorTest`** - plans generate and respect scheduling constraints.
- **`GenEdCoverageTest`** - majors with open gen-ed slots still get gen-eds placed, interests steer the choice, and no semester exceeds its credit cap.
- **`ProfileSnapshotTest`** - profile serialization round-trips correctly.
- **`ManifestSanityTest`** - the manifest wiring stays valid.
- **`PlanStorageTest`** (instrumented) - Room persistence for saving, loading, and deleting plans.

---

## Building and running

Requirements: Android Studio (recent stable), JDK 11, an emulator or device on API 24+.

```bash
git clone <your-repo-url>
cd declare
./gradlew assembleDebug        # build
./gradlew test                 # unit tests
./gradlew connectedAndroidTest # instrumented tests (needs a running device/emulator)
```

Or open the project in Android Studio and run the `app` configuration. The bundled catalog JSON means no API keys or pipeline runs are needed to build and launch.

---

## Project layout

```
app/
  src/main/java/edu/illinois/cs/cs124/ay2026/project/
    model/       data types (Course, Major, Semester, AcademicPlan, exams…)
    data/        catalog parsing, gen-ed pool, CIS API client, Room DB, tuition
    adapter/     RecyclerView adapters
    fragment/    Plans / Profile / Financial tabs
    ui/theme/    Compose theme (Declare palette + type)
    *.java/*.kt  activities + PlanGenerator
  src/main/assets/    bundled catalog JSON
  src/test/           JUnit unit tests
  src/androidTest/    instrumented Room tests
scripts/                 Course Explorer API fetcher (fetch_courses.py)
```

---

## Roadmap

Ideas beyond the current build: expand past the 2024 catalog year, add prerequisite-graph visualizations, support minors and certificates, "what-if" major-switch modeling, and PDF/calendar export of a finished plan.
