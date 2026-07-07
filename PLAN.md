Plan · MD
Copy

# UIUC Four-Year Plan Builder

## App Overview

A mobile app that generates personalized 8-semester academic plans for UIUC students. Users input their desired major, and the app creates an optimized path to graduation while accounting for prerequisites, course availability, and gen ed requirements.

**Primary Goal:** Help students compare different academic paths (majors, double majors, minors) and make informed decisions about their course of study—including potential cost savings from early graduation.

**Differentiator:** Unlike existing tools (e.g., IlliniPlan, which covers only CS and Physics), this app will support 5-10 popular majors at launch, with architecture designed for easy expansion.

---

## Target Majors (MVP)

Initial launch will support 5-10 high-demand majors. Candidates include:
- Computer Science
- Computer Engineering
- Mechanical Engineering
- Electrical Engineering
- Business (Gies)
- Psychology
- Biology
- Economics
- Political Science
- Communications

*Final selection TBD based on data availability and student demand.*

---

## Key Screens & Views

### 1. Profile Setup
- Declared major (or exploring)
- Completed courses (for transfer students or upperclassmen)
- Class interests/preferences (used for gen ed suggestions)
- Expected graduation timeline

### 2. Plan Generator
- Semester-by-semester interactive grid
- Visual prerequisite chain indicators
- Drag-and-drop course rearrangement
- Color coding by course type (major req, gen ed, elective)

### 3. Plan Comparison View
- Side-by-side comparison of different academic paths
- Highlights overlapping courses between majors
- Shows credit hour totals and estimated graduation dates
- Displays tuition cost differential for early graduation scenarios

### 4. Saved Plans
- Store and name multiple plan variations
- Mark one as "active" plan
- Edit history/versioning

---

## Core Features

### Plan Generation Algorithm
1. **Load major requirements** — Pull required courses, electives, and gen ed needs
2. **Build prerequisite graph** — Map all prerequisite chains for required courses
3. **Sequence courses** — Place courses in semesters respecting:
   - Prerequisite ordering
   - Course availability (Fall/Spring/Both)
   - Credit hour limits per semester (default: 15-17)
4. **Fill gen eds** — Slot general education courses into remaining space, prioritizing user interests
5. **Validate plan** — Check for conflicts, missing requirements, overloaded semesters

### Conflict Handling
- If two required courses conflict: suggest alternatives (summer session, online equivalents, sequence adjustment)
- Flag semesters that exceed recommended credit hours
- Warn about courses with historically low availability

### Comparison Engine
- Compare any two plans side-by-side
- Calculate course overlap percentage (useful for double major feasibility)
- Show "additional semesters needed" for double major/minor additions
- Estimate tuition savings for condensed plans

### Cost Optimization
- Model tuition costs by semester
- Identify opportunities to graduate 1 semester early
- Factor in summer course costs vs. full semester savings

---

## Data Requirements

### Course Catalog Data
- Course ID, name, description
- Credit hours
- Prerequisites (as parseable list)
- Gen ed categories fulfilled
- Typical semesters offered (Fall/Spring/Both)

**Source: TBD** — Investigate UIUC Course Explorer API or alternative data sources

### Degree Requirements
- Required courses per major
- Required credit hours by category
- Elective options and restrictions
- Gen ed requirements (university-wide)

**Source: TBD** — May need to manually compile from curriculum guides; investigate structured data availability

### Historical Course Offerings
- Which courses offered which semesters (past 3-4 years)
- Used to predict future availability

**Source: TBD** — Investigate whether historical data is accessible via API or requires compilation

### Tuition Data
- Per-semester tuition rates
- Differential tuition by college (e.g., Engineering, Business)
- Summer session rates

**Source: TBD** — UIUC Office of the Registrar / Student Financial Services

---

## External Dependencies

| Dependency | Purpose | Status |
|------------|---------|--------|
| UIUC Course Explorer API (if exists) | Course catalog data | TBD - needs investigation |
| Degree requirements data source | Major requirements | TBD - may require manual compilation |
| Historical offerings data | Predict course availability | TBD |
| Tuition rate data | Cost calculations | TBD - likely manual entry |

---

## Technical Approach

### Architecture (High-Level)
- **Frontend:** Mobile app (React Native or Flutter — TBD)
- **Backend:** Lightweight API for plan generation logic
- **Database:** Store course data, requirements, user profiles, saved plans

### Data Storage Strategy
- Course catalog and requirements: stored locally in app (updated each semester)
- User data: local storage for MVP, potential cloud sync later
- Plan generation: can run client-side for simple cases

---

## Open Questions

1. **Data sources:** What APIs or datasets are available for course catalog, degree requirements, and historical offerings?

2. **Algorithm complexity:** How sophisticated does the scheduling algorithm need to be? Start simple (greedy prerequisite-first) or build constraint solver?

3. **Update cadence:** How will the app receive updated course data each semester? Manual update? Background fetch?

4. **Scope creep:** Should v1 include the cost optimization feature, or defer to v2?

---

## MVP Milestones (Rough)

| Phase | Focus | Deliverable |
|-------|-------|-------------|
| 1 | Data investigation | Confirmed data sources; sample data for 2-3 majors |
| 2 | Core algorithm | Working plan generator (single major) |
| 3 | UI implementation | Profile, plan view, basic editing |
| 4 | Comparison feature | Side-by-side view |
| 5 | Polish & expand | Add remaining majors, cost features, testing |

*Timeline estimates TBD based on data availability findings.*

---

## Future Enhancements (Post-MVP)

- Expand to all UIUC majors
- Support other universities
- Integration with official UIUC systems (if APIs become available)
- GPA/professor data integration (like IlliniPlan)
- Prerequisite visualization graphs
- "What-if" scenario modeling (what if I switch majors sophomore year?)
- Export plan to PDF or calendar