"""
ReleaseSense ML Service Unit & Integration Tests
Validates the Flask API endpoints, input validation, and model inference.
"""

import json
from app import app

def test_endpoints():
    client = app.test_client()

    print("[TEST 1] Testing /health endpoint...")
    res = client.get("/health")
    assert res.status_code == 200, f"Health check failed: {res.status_code}"
    health_data = res.get_json()
    assert health_data["status"] == "healthy"
    assert health_data["model_loaded"] is True
    print("  -> Passed. Health:", health_data)

    print("\n[TEST 2] Testing /metadata endpoint...")
    res = client.get("/metadata")
    assert res.status_code == 200
    meta = res.get_json()
    assert "accuracy" in meta
    print(f"  -> Passed. Model Accuracy: {meta['accuracy'] * 100:.2f}%")

    print("\n[TEST 3] Testing /predict with Low Risk metrics...")
    low_payload = {
        "changed_files": 12,
        "failed_tests": 0,
        "critical_bugs": 0,
        "coverage": 89.5,
        "previous_failure_rate": 3.0
    }
    res = client.post("/predict", json=low_payload)
    assert res.status_code == 200
    pred = res.get_json()["prediction"]
    assert pred["risk_level"] == "Low", f"Expected Low, got {pred['risk_level']}"
    assert pred["confidence_percent"] > 70.0
    print(f"  -> Passed: {pred['risk_level']} (Confidence: {pred['confidence_percent']}%)")

    print("\n[TEST 4] Testing /predict with High Risk metrics (multiple bugs, low coverage)...")
    high_payload = {
        "changed_files": 320,
        "failed_tests": 6,
        "critical_bugs": 3,
        "coverage": 38.0,
        "previous_failure_rate": 42.0
    }
    res = client.post("/predict", json=high_payload)
    assert res.status_code == 200
    pred = res.get_json()["prediction"]
    assert pred["risk_level"] == "High", f"Expected High, got {pred['risk_level']}"
    print(f"  -> Passed: {pred['risk_level']} (Confidence: {pred['confidence_percent']}%)")
    print(f"     Recommendation: {pred['recommendation']}")

    print("\n[TEST 5] Testing /predict with Medium Risk metrics...")
    med_payload = {
        "changed_files": 85,
        "failed_tests": 2,
        "critical_bugs": 0,
        "coverage": 68.0,
        "previous_failure_rate": 15.0
    }
    res = client.post("/predict", json=med_payload)
    assert res.status_code == 200
    pred = res.get_json()["prediction"]
    assert pred["risk_level"] == "Medium", f"Expected Medium, got {pred['risk_level']}"
    print(f"  -> Passed: {pred['risk_level']} (Confidence: {pred['confidence_percent']}%)")

    print("\n[TEST 6] Testing validation for invalid inputs...")
    bad_payload = {
        "changed_files": -5,  # Invalid negative
        "failed_tests": 0,
        "critical_bugs": 0,
        "coverage": 150.0,  # Invalid > 100
        "previous_failure_rate": 5.0
    }
    res = client.post("/predict", json=bad_payload)
    assert res.status_code == 400, f"Expected 400, got {res.status_code}"
    print("  -> Passed. Received 400 Bad Request as expected:", res.get_json()["message"])

    print("\nAll ML Service tests passed successfully!")

if __name__ == "__main__":
    test_endpoints()
