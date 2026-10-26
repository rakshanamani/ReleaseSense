package com.releasesense.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Represents the risk prediction outcome, confidence level, recommendations,
 * and persistence metadata for a software release.
 */
public class PredictionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private int changedFiles;
    private int failedTests;
    private int criticalBugs;
    private double coverage;
    private double previousFailureRate;
    private String riskLevel;            // Low, Medium, High
    private double confidencePercent;    // 0.0 - 100.0
    private String recommendation;
    private Timestamp createdAt;

    public PredictionResult() {
        this.createdAt = Timestamp.from(Instant.now());
    }

    public PredictionResult(int changedFiles, int failedTests, int criticalBugs, double coverage,
                            double previousFailureRate, String riskLevel, double confidencePercent,
                            String recommendation) {
        this.changedFiles = changedFiles;
        this.failedTests = failedTests;
        this.criticalBugs = criticalBugs;
        this.coverage = coverage;
        this.previousFailureRate = previousFailureRate;
        this.riskLevel = riskLevel;
        this.confidencePercent = confidencePercent;
        this.recommendation = recommendation;
        this.createdAt = Timestamp.from(Instant.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(int changedFiles) {
        this.changedFiles = changedFiles;
    }

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public int getCriticalBugs() {
        return criticalBugs;
    }

    public void setCriticalBugs(int criticalBugs) {
        this.criticalBugs = criticalBugs;
    }

    public double getCoverage() {
        return coverage;
    }

    public void setCoverage(double coverage) {
        this.coverage = coverage;
    }

    public double getPreviousFailureRate() {
        return previousFailureRate;
    }

    public void setPreviousFailureRate(double previousFailureRate) {
        this.previousFailureRate = previousFailureRate;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getConfidencePercent() {
        return confidencePercent;
    }

    public void setConfidencePercent(double confidencePercent) {
        this.confidencePercent = confidencePercent;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PredictionResult{" +
                "id=" + id +
                ", riskLevel='" + riskLevel + '\'' +
                ", confidencePercent=" + confidencePercent + "%" +
                ", recommendation='" + recommendation + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
