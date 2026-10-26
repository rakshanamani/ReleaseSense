package com.releasesense.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.releasesense.dao.ReleasePredictionDAO;
import com.releasesense.model.PredictionResult;
import com.releasesense.model.ReleaseMetric;
import com.releasesense.service.MLPredictionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jakarta Servlet endpoint (/api/predict) for handling release risk prediction requests.
 * Validates inputs, communicates with the ML engine, persists to Oracle DB, and returns results.
 */
@WebServlet(name = "PredictServlet", urlPatterns = {"/api/predict"})
public class PredictServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PredictServlet.class.getName());

    private MLPredictionService mlService;
    private ReleasePredictionDAO predictionDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        super.init();
        this.mlService = new MLPredictionService();
        this.predictionDAO = new ReleasePredictionDAO();
        this.gson = new Gson();
        LOGGER.info("[PredictServlet] Initialized successfully.");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCORSHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCORSHeaders(response);
        response.setContentType("application/json; charset=UTF-8");

        PrintWriter out = response.getWriter();
        JsonObject responseJson = new JsonObject();

        try {
            ReleaseMetric metric = parseRequestMetrics(request);
            metric.validate();

            LOGGER.info("[PredictServlet] Received prediction request: " + metric);

            // 1. Invoke Python ML prediction service
            PredictionResult result = mlService.predictRisk(metric);

            // 2. Persist to Oracle Database
            boolean saved = predictionDAO.savePrediction(result);
            if (!saved) {
                LOGGER.warning("[PredictServlet] Prediction computed, but failed to persist record to database.");
            }

            // 3. Return JSON response
            response.setStatus(HttpServletResponse.SC_OK);
            responseJson.addProperty("status", "success");
            responseJson.addProperty("savedToDatabase", saved);
            responseJson.add("prediction", gson.toJsonTree(result));
            out.print(gson.toJson(responseJson));

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseJson.addProperty("status", "error");
            responseJson.addProperty("message", "Validation Error: " + e.getMessage());
            out.print(gson.toJson(responseJson));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in PredictServlet: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJson.addProperty("status", "error");
            responseJson.addProperty("message", "Internal processing error: " + e.getMessage());
            out.print(gson.toJson(responseJson));
        } finally {
            out.flush();
        }
    }

    private ReleaseMetric parseRequestMetrics(HttpServletRequest request) throws IOException {
        // Check for JSON body first
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();

            int changedFiles = json.get("changedFiles") != null ? json.get("changedFiles").getAsInt() :
                    (json.get("changed_files") != null ? json.get("changed_files").getAsInt() : 0);
            int failedTests = json.get("failedTests") != null ? json.get("failedTests").getAsInt() :
                    (json.get("failed_tests") != null ? json.get("failed_tests").getAsInt() : 0);
            int criticalBugs = json.get("criticalBugs") != null ? json.get("criticalBugs").getAsInt() :
                    (json.get("critical_bugs") != null ? json.get("critical_bugs").getAsInt() : 0);
            double coverage = json.get("coverage") != null ? json.get("coverage").getAsDouble() : 0.0;
            double failureRate = json.get("previousFailureRate") != null ? json.get("previousFailureRate").getAsDouble() :
                    (json.get("previous_failure_rate") != null ? json.get("previous_failure_rate").getAsDouble() : 0.0);

            return new ReleaseMetric(changedFiles, failedTests, criticalBugs, coverage, failureRate);
        }

        // Fallback to Form Parameters
        int changedFiles = Integer.parseInt(getParam(request, "changedFiles", "changed_files"));
        int failedTests = Integer.parseInt(getParam(request, "failedTests", "failed_tests"));
        int criticalBugs = Integer.parseInt(getParam(request, "criticalBugs", "critical_bugs"));
        double coverage = Double.parseDouble(getParam(request, "coverage", "coverage"));
        double failureRate = Double.parseDouble(getParam(request, "previousFailureRate", "previous_failure_rate"));

        return new ReleaseMetric(changedFiles, failedTests, criticalBugs, coverage, failureRate);
    }

    private String getParam(HttpServletRequest request, String param1, String param2) {
        String val = request.getParameter(param1);
        if (val == null || val.trim().isEmpty()) {
            val = request.getParameter(param2);
        }
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter: " + param1);
        }
        return val.trim();
    }

    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
