package com.releasesense.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.releasesense.model.PredictionResult;
import com.releasesense.model.ReleaseMetric;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that communicates with the Python Machine Learning Prediction API
 * over HTTP REST. Includes fallback heuristic scoring for resilience.
 */
public class MLPredictionService {

    private static final Logger LOGGER = Logger.getLogger(MLPredictionService.class.getName());
    public static final String ENV_ML_SERVICE_URL = "ML_SERVICE_URL";
    private static final String DEFAULT_ML_URL = "http://localhost:5001/predict";

    private final HttpClient httpClient;
    private final Gson gson;
    private final String mlServiceUrl;

    public MLPredictionService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        this.gson = new Gson();

        String envUrl = System.getenv(ENV_ML_SERVICE_URL);
        this.mlServiceUrl = (envUrl != null && !envUrl.trim().isEmpty()) ? envUrl.trim() : DEFAULT_ML_URL;
        LOGGER.info("[MLPredictionService] Target ML Service URL: " + this.mlServiceUrl);
    }

    /**
     * Sends release metrics to the Python ML microservice and transforms the response
     * into a PredictionResult.
     *
     * @param metric Validated release metrics
     * @return PredictionResult containing risk classification and confidence
     */
    public PredictionResult predictRisk(ReleaseMetric metric) {
        // Construct JSON payload
        JsonObject requestPayload = new JsonObject();
        requestPayload.addProperty("changed_files", metric.getChangedFiles());
        requestPayload.addProperty("failed_tests", metric.getFailedTests());
        requestPayload.addProperty("critical_bugs", metric.getCriticalBugs());
        requestPayload.addProperty("coverage", metric.getCoverage());
        requestPayload.addProperty("previous_failure_rate", metric.getPreviousFailureRate());

        String jsonString = gson.toJson(requestPayload);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mlServiceUrl))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("prediction")) {
                    JsonObject pred = root.getAsJsonObject("prediction");
                    String riskLevel = pred.get("risk_level").getAsString();
                    double confidence = pred.get("confidence_percent").getAsDouble();
                    String recommendation = pred.get("recommendation").getAsString();

                    return new PredictionResult(
                            metric.getChangedFiles(),
                            metric.getFailedTests(),
                            metric.getCriticalBugs(),
                            metric.getCoverage(),
                            metric.getPreviousFailureRate(),
                            riskLevel,
                            confidence,
                            recommendation
                    );
                }
            } else {
                LOGGER.warning("ML Service returned non-200 code: " + response.statusCode() + " Body: " + response.body());
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Connection to ML Service at " + mlServiceUrl + " failed: " + e.getMessage() + ". Activating fallback heuristic engine.");
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        // Resilient Fallback Heuristic
        return evaluateFallbackHeuristic(metric);
    }

    /**
     * Fallback heuristic scoring to ensure the platform remains functional
     * even during ML microservice restarts or network blips.
     */
    private PredictionResult evaluateFallbackHeuristic(ReleaseMetric m) {
        String risk;
        double confidence;
        String recommendation;

        if (m.getCriticalBugs() >= 2 || m.getFailedTests() >= 5 || (m.getCoverage() < 50.0 && m.getPreviousFailureRate() > 25.0)) {
            risk = "High";
            confidence = 94.5;
            recommendation = "HIGH RISK RELEASE (Fallback Engine): Critical bugs or test failures detected. Halt deployment until resolved.";
        } else if (m.getCriticalBugs() == 1 || m.getFailedTests() > 0 || m.getCoverage() < 70.0 || m.getPreviousFailureRate() > 20.0) {
            risk = "Medium";
            confidence = 86.0;
            recommendation = "MODERATE RISK (Fallback Engine): Potential regression points detected. Execute canary deployment and targeted test runs.";
        } else {
            risk = "Low";
            confidence = 92.0;
            recommendation = "LOW RISK (Fallback Engine): Quality metrics pass all deployment thresholds. Proceed with automated deployment.";
        }

        return new PredictionResult(
                m.getChangedFiles(),
                m.getFailedTests(),
                m.getCriticalBugs(),
                m.getCoverage(),
                m.getPreviousFailureRate(),
                risk,
                confidence,
                recommendation
        );
    }
}
