"""
ReleaseSense - Machine Learning Prediction REST API
Microservice providing real-time software release risk classification
using a trained Random Forest model.
"""

import os
import joblib
import json
from flask import Flask, request, jsonify

app = Flask(__name__)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "models", "release_risk_rf.joblib")
METADATA_PATH = os.path.join(BASE_DIR, "models", "model_metadata.json")

# Global reference to loaded model payload
model_bundle = None

def load_prediction_model():
    global model_bundle
    if os.path.exists(MODEL_PATH):
        try:
            model_bundle = joblib.load(MODEL_PATH)
            print(f"[ML Service] Successfully loaded model from {MODEL_PATH}")
        except Exception as e:
            print(f"[ML Service] Error loading model: {e}")
            model_bundle = None
    else:
        print(f"[ML Service] Warning: Model file not found at {MODEL_PATH}. Run train_model.py first.")
        model_bundle = None

# Attempt to load model on module load
load_prediction_model()

def generate_recommendations(risk_level, confidence, metrics):
    """
    Produces actionable engineering recommendations based on the predicted risk level
    and specific metrics values.
    """
    changed_files = metrics["changed_files"]
    failed_tests = metrics["failed_tests"]
    critical_bugs = metrics["critical_bugs"]
    coverage = metrics["coverage"]
    prev_failure_rate = metrics["previous_failure_rate"]

    action_items = []

    if risk_level == "High":
        primary_msg = "HIGH RISK RELEASE: Deployment should be blocked immediately until critical blockers are resolved."
        if critical_bugs > 0:
            action_items.append(f"Resolve {critical_bugs} critical bug(s) before release approval.")
        if failed_tests > 0:
            action_items.append(f"Fix {failed_tests} failing automated test(s); zero test failure tolerance.")
        if coverage < 50.0:
            action_items.append(f"Code coverage ({coverage:.1f}%) is below acceptable baseline. Increase to >= 75%.")
        if changed_files > 150:
            action_items.append(f"Large changeset detected ({changed_files} files). Consider breaking down into smaller incremental pull requests.")
        if prev_failure_rate > 30.0:
            action_items.append(f"Historical failure rate ({prev_failure_rate:.1f}%) is elevated. Mandate peer QA sign-off.")
        if not action_items:
            action_items.append("Conduct an emergency architectural review before proceeding.")

    elif risk_level == "Medium":
        primary_msg = "MODERATE RISK: Release requires targeted manual verification and canary deployment."
        if critical_bugs > 0:
            action_items.append(f"Review {critical_bugs} open critical bug(s) with product owner.")
        if failed_tests > 0:
            action_items.append(f"Investigate {failed_tests} flaky or failing test(s).")
        if coverage < 70.0:
            action_items.append(f"Increase unit and integration test coverage from {coverage:.1f}% to >= 75%.")
        action_items.append("Deploy to a 5% Canary deployment ring and observe error budget for 30 minutes.")
        action_items.append("Ensure rollback scripts and database migration undo plans are tested.")

    else:  # Low Risk
        primary_msg = "LOW RISK: Release meets automated quality gates and is recommended for deployment."
        action_items.append("Proceed with automated Blue/Green or Rolling deployment pipeline.")
        action_items.append("Standard APM and distributed tracing alerts enabled for post-release monitoring.")
        if coverage < 80.0:
            action_items.append(f"Note: Current coverage is {coverage:.1f}%. Continue maintaining good testing standards.")

    recommendation_summary = f"{primary_msg} {' '.join(action_items[:2])}"

    return {
        "summary": recommendation_summary,
        "action_items": action_items
    }

@app.route("/", methods=["GET"])
@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint to inspect service and model status."""
    is_loaded = model_bundle is not None
    return jsonify({
        "service": "ReleaseSense ML Prediction Microservice",
        "status": "healthy",
        "model_loaded": is_loaded,
        "model_version": model_bundle.get("version", "unknown") if is_loaded else None
    }), 200

@app.route("/metadata", methods=["GET"])
def metadata():
    """Returns metadata regarding training accuracy, features, and evaluation."""
    if os.path.exists(METADATA_PATH):
        try:
            with open(METADATA_PATH, "r", encoding="utf-8") as f:
                data = json.load(f)
            return jsonify(data), 200
        except Exception as e:
            return jsonify({"error": f"Failed to read metadata: {str(e)}"}), 500
    return jsonify({"error": "Metadata not available. Model has not been trained."}), 404

@app.route("/predict", methods=["POST"])
def predict():
    """
    Accepts JSON with release metrics and returns risk classification,
    confidence score, and recommendations.
    """
    global model_bundle
    if model_bundle is None:
        load_prediction_model()
        if model_bundle is None:
            return jsonify({
                "status": "error",
                "message": "Model not loaded. Please ensure the model has been trained."
            }), 503

    data = request.get_json(silent=True)
    if not data or not isinstance(data, dict):
        return jsonify({
            "status": "error",
            "message": "Invalid request body. Expected JSON object with release metrics."
        }), 400

    required_fields = ["changed_files", "failed_tests", "critical_bugs", "coverage", "previous_failure_rate"]
    missing = [f for f in required_fields if f not in data]
    if missing:
        return jsonify({
            "status": "error",
            "message": f"Missing required metrics: {', '.join(missing)}"
        }), 400

    # Validation
    try:
        changed_files = int(data["changed_files"])
        failed_tests = int(data["failed_tests"])
        critical_bugs = int(data["critical_bugs"])
        coverage = float(data["coverage"])
        previous_failure_rate = float(data["previous_failure_rate"])

        if changed_files < 0:
            raise ValueError("changed_files must be greater than or equal to 0.")
        if failed_tests < 0:
            raise ValueError("failed_tests must be greater than or equal to 0.")
        if critical_bugs < 0:
            raise ValueError("critical_bugs must be greater than or equal to 0.")
        if not (0.0 <= coverage <= 100.0):
            raise ValueError("coverage must be between 0.0 and 100.0 percent.")
        if not (0.0 <= previous_failure_rate <= 100.0):
            raise ValueError("previous_failure_rate must be between 0.0 and 100.0 percent.")

    except (ValueError, TypeError) as ve:
        return jsonify({
            "status": "error",
            "message": f"Input validation failed: {str(ve)}"
        }), 400

    input_metrics = {
        "changed_files": changed_files,
        "failed_tests": failed_tests,
        "critical_bugs": critical_bugs,
        "coverage": coverage,
        "previous_failure_rate": previous_failure_rate
    }

    try:
        model = model_bundle["model"]
        features = model_bundle.get("features", required_fields)
        import pandas as pd
        feature_df = pd.DataFrame([input_metrics])[features]

        # Model inference
        predicted_class = model.predict(feature_df)[0]
        classes = list(model.classes_)
        probabilities = model.predict_proba(feature_df)[0]

        prob_dict = {
            classes[i]: round(float(probabilities[i]), 4)
            for i in range(len(classes))
        }

        # Confidence percentage is probability of predicted class
        pred_idx = classes.index(predicted_class)
        confidence_percent = round(float(probabilities[pred_idx]) * 100, 2)

        recs = generate_recommendations(predicted_class, confidence_percent, input_metrics)

        return jsonify({
            "status": "success",
            "prediction": {
                "risk_level": predicted_class,
                "confidence_percent": confidence_percent,
                "probabilities": prob_dict,
                "recommendation": recs["summary"],
                "action_items": recs["action_items"],
                "metrics": input_metrics
            }
        }), 200

    except Exception as e:
        return jsonify({
            "status": "error",
            "message": f"Inference engine failure: {str(e)}"
        }), 500

if __name__ == "__main__":
    # Host on 0.0.0.0:5001 so it's accessible within Docker and to the Java backend
    port = int(os.environ.get("PORT", 5001))
    print(f"Starting ReleaseSense ML prediction service on port {port}...")
    app.run(host="0.0.0.0", port=port, debug=False)
