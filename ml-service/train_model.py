"""
ReleaseSense - Machine Learning Training Pipeline
Trains a Random Forest Classifier to predict software release risk
based on software release metrics:
- changed_files
- failed_tests
- critical_bugs
- coverage
- previous_failure_rate

Note on Data:
The dataset ml-service/data/release_metrics.csv contains synthetic/demonstration
historical records formulated to mirror empirical DevOps delivery metrics.
Model accuracy metrics reported reflect this synthetic evaluation benchmark.
"""

import os
import json
import joblib
import pandas as pd
import numpy as np
from datetime import datetime, timezone
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_PATH = os.path.join(BASE_DIR, "data", "release_metrics.csv")
MODEL_DIR = os.path.join(BASE_DIR, "models")
MODEL_PATH = os.path.join(MODEL_DIR, "release_risk_rf.joblib")
METADATA_PATH = os.path.join(MODEL_DIR, "model_metadata.json")

FEATURES = [
    "changed_files",
    "failed_tests",
    "critical_bugs",
    "coverage",
    "previous_failure_rate"
]
TARGET = "risk_level"

def train():
    print("=" * 60)
    print("ReleaseSense ML Training Pipeline - Random Forest Classifier")
    print("=" * 60)

    if not os.path.exists(DATA_PATH):
        raise FileNotFoundError(f"Training dataset not found at {DATA_PATH}. Run generate_dataset.py first.")

    # 1. Load Data
    df = pd.read_csv(DATA_PATH)
    print(f"Loaded {len(df)} records from {DATA_PATH}")
    print(f"Class distribution:\n{df[TARGET].value_counts()}\n")

    X = df[FEATURES]
    y = df[TARGET]

    # 2. Stratified Train / Test Split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42, stratify=y
    )
    print(f"Train set: {len(X_train)} samples | Test set: {len(X_test)} samples")

    # 3. Model Initialization & Training
    # Random Forest is ideal for tabular software release metrics due to non-linear thresholds
    # and feature interaction (e.g. coverage vs critical bugs).
    rf = RandomForestClassifier(
        n_estimators=100,
        max_depth=7,
        min_samples_split=4,
        min_samples_leaf=2,
        random_state=42,
        class_weight="balanced"
    )

    print("\nTraining Random Forest model...")
    rf.fit(X_train, y_train)

    # 4. Evaluation
    y_pred = rf.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    report = classification_report(y_test, y_pred, output_dict=True)
    report_text = classification_report(y_test, y_pred)
    cm = confusion_matrix(y_test, y_pred, labels=rf.classes_)

    print("\nModel Evaluation Results:")
    print("-" * 40)
    print(f"Overall Accuracy: {acc * 100:.2f}%")
    print("\nClassification Report:\n", report_text)
    print("Confusion Matrix (Labels:", list(rf.classes_), "):\n", cm)

    # Feature Importances
    importances = dict(zip(FEATURES, [round(float(v), 4) for v in rf.feature_importances_]))
    print("\nFeature Importances:")
    for feat, imp in sorted(importances.items(), key=lambda item: item[1], reverse=True):
        print(f"  - {feat:22s}: {imp:.4f}")

    # 5. Persist Model and Artifacts
    os.makedirs(MODEL_DIR, exist_ok=True)
    model_payload = {
        "model": rf,
        "features": FEATURES,
        "classes": list(rf.classes_),
        "version": "1.0.0",
        "trained_at": datetime.now(timezone.utc).isoformat()
    }
    joblib.dump(model_payload, MODEL_PATH)
    print(f"\nModel saved successfully to: {MODEL_PATH}")

    # Save metadata JSON for API introspection
    metadata = {
        "model_type": "RandomForestClassifier",
        "n_estimators": 100,
        "features": FEATURES,
        "classes": list(rf.classes_),
        "accuracy": round(float(acc), 4),
        "metrics_report": report,
        "feature_importances": importances,
        "dataset_info": {
            "source": "Synthetic demonstration dataset (data/release_metrics.csv)",
            "total_records": len(df),
            "train_size": len(X_train),
            "test_size": len(X_test)
        },
        "trained_at": datetime.now(timezone.utc).isoformat()
    }

    with open(METADATA_PATH, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    print(f"Metadata saved to: {METADATA_PATH}")
    print("=" * 60)

    return model_payload, metadata

if __name__ == "__main__":
    train()
