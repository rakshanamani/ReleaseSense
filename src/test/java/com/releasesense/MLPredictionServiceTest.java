package com.releasesense;

import com.releasesense.model.PredictionResult;
import com.releasesense.model.ReleaseMetric;
import com.releasesense.service.MLPredictionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MLPredictionServiceTest {

    @Test
    @DisplayName("Evaluate fallback heuristic for low risk metrics")
    void testLowRiskEvaluation() {
        MLPredictionService service = new MLPredictionService();
        ReleaseMetric low = new ReleaseMetric(20, 0, 0, 92.0, 3.0);

        PredictionResult result = service.predictRisk(low);
        assertNotNull(result);
        assertNotNull(result.getRiskLevel());
        assertTrue(result.getConfidencePercent() > 50.0);
        assertNotNull(result.getRecommendation());
    }

    @Test
    @DisplayName("Evaluate fallback heuristic for high risk metrics (critical bugs)")
    void testHighRiskEvaluation() {
        MLPredictionService service = new MLPredictionService();
        ReleaseMetric high = new ReleaseMetric(250, 7, 3, 35.0, 48.0);

        PredictionResult result = service.predictRisk(high);
        assertNotNull(result);
        assertEquals("High", result.getRiskLevel());
        assertTrue(result.getRecommendation().contains("HIGH RISK"));
    }
}
