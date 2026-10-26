"""
ReleaseSense - Synthetic Dataset Generator
Generates realistic software release metrics with Low, Medium, and High risk classifications.
Note: This dataset represents synthetic historical release data engineered to mirror real-world
software deployment patterns (e.g., test failures, coverage dips, and bug densities).
"""

import csv
import random
import os

OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "data", "release_metrics.csv")

def determine_risk(changed_files, failed_tests, critical_bugs, coverage, previous_failure_rate):
    # Rule 1: High Risk conditions
    # Multiple critical bugs or heavy test failures or severe lack of test coverage with high failure rate
    if critical_bugs >= 2:
        return "High"
    if failed_tests >= 5:
        return "High"
    if critical_bugs == 1 and failed_tests >= 2:
        return "High"
    if coverage < 50.0 and (changed_files > 80 or previous_failure_rate > 25.0):
        return "High"
    if previous_failure_rate > 40.0 and failed_tests >= 1:
        return "High"
    if changed_files > 300 and (failed_tests > 0 or coverage < 60.0):
        return "High"

    # Rule 2: Medium Risk conditions
    if critical_bugs == 1:
        return "Medium"
    if 1 <= failed_tests <= 4:
        return "Medium"
    if 50.0 <= coverage < 70.0:
        return "Medium"
    if 20.0 <= previous_failure_rate <= 40.0:
        return "Medium"
    if changed_files > 150:
        return "Medium"

    # Rule 3: Low Risk
    # Low bugs/failures, high coverage, healthy history
    if coverage >= 70.0 and critical_bugs == 0 and failed_tests == 0 and previous_failure_rate <= 20.0:
        return "Low"

    # Default fallback based on composite score
    composite = (critical_bugs * 30) + (failed_tests * 12) + (previous_failure_rate * 0.8) + (changed_files * 0.05) - (coverage * 0.4)
    if composite > 35:
        return "High"
    elif composite > 10:
        return "Medium"
    else:
        return "Low"

def generate_records(num_records=600):
    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    random.seed(42)  # For reproducible dataset generation

    rows = []
    for _ in range(num_records):
        # Sample profiles: 40% clean releases, 35% moderate changes/issues, 25% problematic/hotfix
        profile = random.choices(["clean", "moderate", "problematic"], weights=[0.40, 0.35, 0.25])[0]

        if profile == "clean":
            changed_files = random.randint(3, 90)
            failed_tests = 0
            critical_bugs = 0
            coverage = round(random.uniform(75.0, 96.0), 2)
            previous_failure_rate = round(random.uniform(0.0, 14.0), 2)
        elif profile == "moderate":
            changed_files = random.randint(25, 220)
            failed_tests = random.choices([0, 1, 2, 3], weights=[0.4, 0.35, 0.15, 0.1])[0]
            critical_bugs = random.choices([0, 1], weights=[0.8, 0.2])[0]
            coverage = round(random.uniform(55.0, 82.0), 2)
            previous_failure_rate = round(random.uniform(10.0, 32.0), 2)
        else: # problematic
            changed_files = random.randint(80, 480)
            failed_tests = random.randint(2, 12)
            critical_bugs = random.choices([1, 2, 3, 4], weights=[0.45, 0.35, 0.15, 0.05])[0]
            coverage = round(random.uniform(30.0, 68.0), 2)
            previous_failure_rate = round(random.uniform(22.0, 58.0), 2)

        risk = determine_risk(changed_files, failed_tests, critical_bugs, coverage, previous_failure_rate)
        rows.append({
            "changed_files": changed_files,
            "failed_tests": failed_tests,
            "critical_bugs": critical_bugs,
            "coverage": coverage,
            "previous_failure_rate": previous_failure_rate,
            "risk_level": risk
        })

    with open(OUTPUT_PATH, mode="w", newline="", encoding="utf-8") as f:
        fieldnames = ["changed_files", "failed_tests", "critical_bugs", "coverage", "previous_failure_rate", "risk_level"]
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Successfully generated {len(rows)} release records into {OUTPUT_PATH}")

if __name__ == "__main__":
    generate_records()
