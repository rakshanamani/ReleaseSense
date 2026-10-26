package com.releasesense.model;

import java.io.Serializable;

/**
 * Encapsulates the core metrics of a software release used for risk prediction.
 */
public class ReleaseMetric implements Serializable {
    private static final long serialVersionUID = 1L;

    private int changedFiles;
    private int failedTests;
    private int criticalBugs;
    private double coverage;
    private double previousFailureRate;

    public ReleaseMetric() {
    }

    public ReleaseMetric(int changedFiles, int failedTests, int criticalBugs, double coverage, double previousFailureRate) {
        this.changedFiles = changedFiles;
        this.failedTests = failedTests;
        this.criticalBugs = criticalBugs;
        this.coverage = coverage;
        this.previousFailureRate = previousFailureRate;
    }

    /**
     * Validates that all release metrics conform to business and mathematical boundaries.
     * @throws IllegalArgumentException if any metric violates constraints
     */
    public void validate() throws IllegalArgumentException {
        if (changedFiles < 0) {
            throw new IllegalArgumentException("Changed files count cannot be negative: " + changedFiles);
        }
        if (failedTests < 0) {
            throw new IllegalArgumentException("Failed tests count cannot be negative: " + failedTests);
        }
        if (criticalBugs < 0) {
            throw new IllegalArgumentException("Critical bugs count cannot be negative: " + criticalBugs);
        }
        if (coverage < 0.0 || coverage > 100.0) {
            throw new IllegalArgumentException("Test coverage must be between 0.0% and 100.0%: " + coverage);
        }
        if (previousFailureRate < 0.0 || previousFailureRate > 100.0) {
            throw new IllegalArgumentException("Previous failure rate must be between 0.0% and 100.0%: " + previousFailureRate);
        }
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

    @Override
    public String toString() {
        return "ReleaseMetric{" +
                "changedFiles=" + changedFiles +
                ", failedTests=" + failedTests +
                ", criticalBugs=" + criticalBugs +
                ", coverage=" + coverage + "%" +
                ", previousFailureRate=" + previousFailureRate + "%" +
                '}';
    }
}
