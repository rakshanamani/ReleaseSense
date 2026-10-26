package com.releasesense;

import com.releasesense.dao.ReleasePredictionDAO;
import com.releasesense.model.PredictionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ReleasePredictionDAOTest {

    private ReleasePredictionDAO dao;

    @BeforeEach
    void setUp() {
        dao = new ReleasePredictionDAO();
    }

    @Test
    @DisplayName("Persist a prediction result and retrieve generated primary key")
    void testSavePrediction() {
        PredictionResult item = new PredictionResult(
                55, 1, 0, 82.0, 8.5, "Low", 93.4,
                "LOW RISK: Release meets quality gates."
        );

        boolean saved = dao.savePrediction(item);
        assertTrue(saved, "Prediction should be successfully saved to DB");
        assertNotNull(item.getId(), "Generated ID must be assigned");
        assertTrue(item.getId() > 0, "ID should be a positive number");
    }

    @Test
    @DisplayName("Retrieve recent predictions returns non-empty list with valid fields")
    void testGetAllPredictions() {
        List<PredictionResult> list = dao.getAllPredictions(10);
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Should retrieve at least seed records");

        PredictionResult first = list.get(0);
        assertNotNull(first.getRiskLevel());
        assertTrue(first.getConfidencePercent() >= 0.0);
    }

    @Test
    @DisplayName("Retrieve dashboard statistics aggregates correct summary counts")
    void testGetDashboardStatistics() {
        Map<String, Object> stats = dao.getDashboardStatistics();
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalReleases"));
        assertTrue(stats.containsKey("avgCoverage"));
        assertTrue((int) stats.get("totalReleases") >= 1);
    }
}
