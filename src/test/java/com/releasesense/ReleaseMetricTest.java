package com.releasesense;

import com.releasesense.model.ReleaseMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReleaseMetricTest {

    @Test
    @DisplayName("Valid release metrics pass validation without exception")
    void testValidMetricsPassValidation() {
        ReleaseMetric metric = new ReleaseMetric(45, 0, 0, 85.5, 5.0);
        assertDoesNotThrow(metric::validate);
    }

    @Test
    @DisplayName("Negative changed files count throws IllegalArgumentException")
    void testNegativeChangedFilesThrows() {
        ReleaseMetric metric = new ReleaseMetric(-1, 0, 0, 85.0, 5.0);
        Exception ex = assertThrows(IllegalArgumentException.class, metric::validate);
        assertTrue(ex.getMessage().contains("Changed files"));
    }

    @Test
    @DisplayName("Negative failed tests count throws IllegalArgumentException")
    void testNegativeFailedTestsThrows() {
        ReleaseMetric metric = new ReleaseMetric(10, -3, 0, 85.0, 5.0);
        Exception ex = assertThrows(IllegalArgumentException.class, metric::validate);
        assertTrue(ex.getMessage().contains("Failed tests"));
    }

    @Test
    @DisplayName("Negative critical bugs count throws IllegalArgumentException")
    void testNegativeCriticalBugsThrows() {
        ReleaseMetric metric = new ReleaseMetric(10, 0, -2, 85.0, 5.0);
        Exception ex = assertThrows(IllegalArgumentException.class, metric::validate);
        assertTrue(ex.getMessage().contains("Critical bugs"));
    }

    @Test
    @DisplayName("Coverage above 100% throws IllegalArgumentException")
    void testCoverageAbove100Throws() {
        ReleaseMetric metric = new ReleaseMetric(10, 0, 0, 105.0, 5.0);
        Exception ex = assertThrows(IllegalArgumentException.class, metric::validate);
        assertTrue(ex.getMessage().contains("coverage"));
    }

    @Test
    @DisplayName("Previous failure rate above 100% throws IllegalArgumentException")
    void testFailureRateAbove100Throws() {
        ReleaseMetric metric = new ReleaseMetric(10, 0, 0, 80.0, 150.0);
        Exception ex = assertThrows(IllegalArgumentException.class, metric::validate);
        assertTrue(ex.getMessage().contains("Previous failure rate"));
    }
}
