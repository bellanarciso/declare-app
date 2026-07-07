# Fetches course info from the UIUC Course Explorer XML API and writes a
# courses.json file for the app's assets/ folder.
#
# Run with: python3 fetch_courses.py
#
# Edit the MAJORS dictionary below to set which courses belong to each major
# and their type (MAJOR_REQUIREMENT, GEN_ED, ELECTIVE). The script fills in the
# name, credits, fall/spring availability, and parses prerequisites from the
# course description.

import json
import re
import sys
import time
import xml.etree.ElementTree as ET

import requests

# Configuration

CATALOG_URL = "https://courses.illinois.edu/cisapp/explorer/catalog/2026/fall/{subject}/{number}.xml"
SCHEDULE_URL = "https://courses.illinois.edu/cisapp/explorer/schedule/{year}/{term}/{subject}/{number}.xml"

# Years to check when determining Fall/Spring availability.
AVAILABILITY_YEARS = [2025, 2024, 2023]

# Delay between API requests to be polite to the server.
REQUEST_DELAY = 0.15  # seconds

# Major definitions
# Format: "DEPT NUM": "TYPE"
# TYPE is one of: MAJOR_REQUIREMENT, GEN_ED, ELECTIVE

MAJOR_COLLEGES = {
    "Computer Science":                       "Grainger College of Engineering",
    "Physics":                                "College of Liberal Arts & Sciences",
    "Finance":                                "Gies College of Business",
    "Strategy, Innovation, and Entrepreneurship": "Gies College of Business",
}

MAJORS = {
    "Computer Science": {
        "CS 124":   "MAJOR_REQUIREMENT",
        "CS 128":   "MAJOR_REQUIREMENT",
        "CS 173":   "MAJOR_REQUIREMENT",
        "CS 225":   "MAJOR_REQUIREMENT",
        "CS 233":   "MAJOR_REQUIREMENT",
        "CS 241":   "MAJOR_REQUIREMENT",
        "CS 374":   "MAJOR_REQUIREMENT",
        "CS 421":   "MAJOR_REQUIREMENT",
        "CS 426":   "MAJOR_REQUIREMENT",
        "MATH 221": "MAJOR_REQUIREMENT",
        "MATH 231": "MAJOR_REQUIREMENT",
        "MATH 257": "MAJOR_REQUIREMENT",
        "STAT 400": "MAJOR_REQUIREMENT",
        "CS 411":   "ELECTIVE",
        "CS 440":   "ELECTIVE",
        "CS 446":   "ELECTIVE",
        "CS 461":   "ELECTIVE",
        "RHET 105": "GEN_ED",
    },
    "Physics": {
        "PHYS 211": "MAJOR_REQUIREMENT",
        "PHYS 212": "MAJOR_REQUIREMENT",
        "PHYS 213": "MAJOR_REQUIREMENT",
        "PHYS 214": "MAJOR_REQUIREMENT",
        "PHYS 225": "MAJOR_REQUIREMENT",
        "PHYS 325": "MAJOR_REQUIREMENT",
        "PHYS 326": "MAJOR_REQUIREMENT",
        "PHYS 401": "MAJOR_REQUIREMENT",
        "PHYS 435": "MAJOR_REQUIREMENT",
        "PHYS 436": "MAJOR_REQUIREMENT",
        "PHYS 486": "MAJOR_REQUIREMENT",
        "PHYS 487": "MAJOR_REQUIREMENT",
        "MATH 221": "MAJOR_REQUIREMENT",
        "MATH 231": "MAJOR_REQUIREMENT",
        "MATH 241": "MAJOR_REQUIREMENT",
        "MATH 285": "MAJOR_REQUIREMENT",
        "PHYS 404": "ELECTIVE",
        "PHYS 427": "ELECTIVE",
        "PHYS 460": "ELECTIVE",
        "RHET 105": "GEN_ED",
    },
    "Finance": {
        # Business core (all Gies students)
        "ACCY 201": "MAJOR_REQUIREMENT",
        "ACCY 202": "MAJOR_REQUIREMENT",
        "BUS 101":  "MAJOR_REQUIREMENT",
        "BUS 201":  "MAJOR_REQUIREMENT",
        "BUS 301":  "MAJOR_REQUIREMENT",
        "BUS 401":  "MAJOR_REQUIREMENT",
        "BADM 210": "MAJOR_REQUIREMENT",
        "BADM 211": "MAJOR_REQUIREMENT",
        "BADM 275": "MAJOR_REQUIREMENT",
        "BADM 300": "MAJOR_REQUIREMENT",
        "BADM 310": "MAJOR_REQUIREMENT",
        "BADM 320": "MAJOR_REQUIREMENT",
        "BADM 449": "MAJOR_REQUIREMENT",
        "CMN 101":  "MAJOR_REQUIREMENT",
        "CS 105":   "MAJOR_REQUIREMENT",
        "ECON 102": "MAJOR_REQUIREMENT",
        "ECON 103": "MAJOR_REQUIREMENT",
        "FIN 221":  "MAJOR_REQUIREMENT",
        "MATH 220": "MAJOR_REQUIREMENT",
        # Finance major requirements
        "FIN 300":  "MAJOR_REQUIREMENT",
        "FIN 321":  "MAJOR_REQUIREMENT",
        "FIN 411":  "MAJOR_REQUIREMENT",
    },
    "Strategy, Innovation, and Entrepreneurship": {
        # Business core (all Gies students)
        "ACCY 201": "MAJOR_REQUIREMENT",
        "ACCY 202": "MAJOR_REQUIREMENT",
        "BUS 101":  "MAJOR_REQUIREMENT",
        "BUS 201":  "MAJOR_REQUIREMENT",
        "BUS 301":  "MAJOR_REQUIREMENT",
        "BUS 401":  "MAJOR_REQUIREMENT",
        "BADM 210": "MAJOR_REQUIREMENT",
        "BADM 211": "MAJOR_REQUIREMENT",
        "BADM 275": "MAJOR_REQUIREMENT",
        "BADM 300": "MAJOR_REQUIREMENT",
        "BADM 310": "MAJOR_REQUIREMENT",
        "BADM 320": "MAJOR_REQUIREMENT",
        "BADM 449": "MAJOR_REQUIREMENT",
        "CMN 101":  "MAJOR_REQUIREMENT",
        "CS 105":   "MAJOR_REQUIREMENT",
        "ECON 102": "MAJOR_REQUIREMENT",
        "ECON 103": "MAJOR_REQUIREMENT",
        "FIN 221":  "MAJOR_REQUIREMENT",
        "MATH 220": "MAJOR_REQUIREMENT",
        # SIE-specific requirements
        "BADM 374": "MAJOR_REQUIREMENT",
        "BADM 375": "MAJOR_REQUIREMENT",
        "BADM 477": "MAJOR_REQUIREMENT",
    },
}

# Gen ed placeholders added to every major automatically.
GEN_ED_PLACEHOLDERS = [
    {"id": "HUM 1xx",  "name": "Humanities Elective",       "credits": 3, "prereqs": [], "type": "GEN_ED", "offeredFall": True, "offeredSpring": True},
    {"id": "HUM 2xx",  "name": "Advanced Composition",      "credits": 3, "prereqs": [], "type": "GEN_ED", "offeredFall": True, "offeredSpring": True},
    {"id": "SOC 1xx",  "name": "Social Science Elective",   "credits": 3, "prereqs": [], "type": "GEN_ED", "offeredFall": True, "offeredSpring": True},
    {"id": "NAT 1xx",  "name": "Natural Science Elective",  "credits": 3, "prereqs": [], "type": "GEN_ED", "offeredFall": True, "offeredSpring": True},
    {"id": "CULT 1xx", "name": "Cultural Studies Elective", "credits": 3, "prereqs": [], "type": "GEN_ED", "offeredFall": True, "offeredSpring": True},
]

FREE_ELECTIVE_COUNT = 8


# Helpers

def split_id(course_id):
    """Split 'CS 124' into ('CS', '124')."""
    parts = course_id.strip().split()
    return parts[0], parts[1]


def fetch_xml(url):
    try:
        r = requests.get(url, timeout=10)
        if r.status_code == 200:
            return ET.fromstring(r.content)
    except Exception as e:
        print(f"  Warning: request failed for {url}: {e}", file=sys.stderr)
    return None


def get_availability(subject, number):
    """Check recent semesters to determine if a course runs Fall/Spring."""
    offered_fall = False
    offered_spring = False
    for year in AVAILABILITY_YEARS:
        for term in ("fall", "spring"):
            url = SCHEDULE_URL.format(year=year, term=term, subject=subject, number=number)
            tree = fetch_xml(url)
            if tree is not None:
                if term == "fall":
                    offered_fall = True
                else:
                    offered_spring = True
            time.sleep(REQUEST_DELAY)
        if offered_fall and offered_spring:
            break  # No need to check further back
    # If we found nothing (course may be new), default to both terms.
    if not offered_fall and not offered_spring:
        offered_fall = offered_spring = True
    return offered_fall, offered_spring


COURSE_ID_RE = re.compile(r'\b([A-Z]{2,5})\s+(\d{3}[A-Z]?)\b')


def parse_prereqs(description):
    """Extract course IDs from the prerequisite portion of a description."""
    if not description:
        return []
    # Isolate the prerequisite sentence(s).
    match = re.search(r'[Pp]rerequisite[s]?\s*:?\s*(.*?)(?:\.|;|$)', description)
    text = match.group(1) if match else description
    return [f"{s} {n}" for s, n in COURSE_ID_RE.findall(text)]


STANDING_RE = re.compile(
    r'\b(freshman|sophomore|junior|senior)\s+standing\b',
    re.IGNORECASE
)
STANDING_YEAR = {"freshman": 1, "sophomore": 2, "junior": 3, "senior": 4}


def parse_min_year(description):
    """Extract minimum year standing from a course description, or 0 if none found."""
    if not description:
        return 0
    match = STANDING_RE.search(description)
    if match:
        return STANDING_YEAR[match.group(1).lower()]
    return 0


def fetch_course(course_id, course_type):
    subject, number = split_id(course_id)
    print(f"  Fetching {course_id}...")

    url = CATALOG_URL.format(subject=subject, number=number)
    tree = fetch_xml(url)
    time.sleep(REQUEST_DELAY)

    name = course_id  # fallback
    credits = 3       # fallback
    prereqs = []

    if tree is not None:
        label = tree.findtext("label")
        if label:
            # Label is often "DEPT NNN: Full Name" - strip the prefix.
            name = re.sub(r'^[A-Z]+\s+\d+[A-Z]?\s*:\s*', '', label).strip()

        credit_text = tree.findtext("creditHours")
        if credit_text:
            # May be "3" or "3 to 4" - take the first number.
            m = re.search(r'\d+', credit_text)
            if m:
                credits = int(m.group())

        description = tree.findtext("description") or ""
        prereqs = parse_prereqs(description)
        min_year = parse_min_year(description)

    offered_fall, offered_spring = get_availability(subject, number)

    return {
        "id": course_id,
        "name": name,
        "credits": credits,
        "prereqs": prereqs,
        "type": course_type,
        "offeredFall": offered_fall,
        "offeredSpring": offered_spring,
        "minYear": min_year,
    }


# Main

def main():
    out_path = "courses.json"

    # Load existing data so we don't overwrite already-fixed majors.
    existing = {"majors": []}
    try:
        with open(out_path) as f:
            existing = json.load(f)
        print(f"Loaded existing {out_path}")
    except FileNotFoundError:
        pass

    existing_names = {m["name"] for m in existing["majors"]}

    # De-duplicate API calls across majors within this run.
    course_cache = {}

    for major_name, course_map in MAJORS.items():
        if major_name in existing_names:
            print(f"\nSkipping {major_name} (already in {out_path})")
            continue

        print(f"\nBuilding {major_name}...")
        courses = []

        for course_id, course_type in course_map.items():
            if course_id not in course_cache:
                course_cache[course_id] = fetch_course(course_id, course_type)
            entry = dict(course_cache[course_id])
            entry["type"] = course_type
            courses.append(entry)

        # Add shared gen ed placeholders.
        courses.extend(GEN_ED_PLACEHOLDERS)

        # Add free elective placeholders.
        for i in range(1, FREE_ELECTIVE_COUNT + 1):
            courses.append({
                "id": f"ELEC {i:03d}",
                "name": f"Free Elective {i}",
                "credits": 3,
                "prereqs": [],
                "type": "ELECTIVE",
                "offeredFall": True,
                "offeredSpring": True,
            })

        existing["majors"].append({
            "name": major_name,
            "college": MAJOR_COLLEGES.get(major_name, ""),
            "courses": courses,
        })

    with open(out_path, "w") as f:
        json.dump(existing, f, indent=2)

    print(f"\nDone! Wrote {out_path}")
    print("Next steps:")
    print("  1. Review courses.json and fix any prereqs the parser missed.")
    print("  2. Copy it to app/src/main/assets/courses.json")


if __name__ == "__main__":
    main()