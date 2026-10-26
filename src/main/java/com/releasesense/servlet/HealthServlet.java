package com.releasesense.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.releasesense.dao.DatabaseConfig;
import com.releasesense.service.MLPredictionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;

/**
 * Health check endpoint (/api/health) for container orchestration,
 * load balancers, and frontend status indicators.
 */
@WebServlet(name = "HealthServlet", urlPatterns = {"/api/health"})
public class HealthServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json; charset=UTF-8");

        PrintWriter out = response.getWriter();
        JsonObject health = new JsonObject();
        health.addProperty("application", "ReleaseSense – Intelligent Release Prediction Platform");
        health.addProperty("status", "UP");

        // 1. Check Database
        JsonObject dbStatus = new JsonObject();
        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean valid = conn.isValid(2);
            dbStatus.addProperty("status", valid ? "CONNECTED" : "DISCONNECTED");
            dbStatus.addProperty("driver", DatabaseConfig.isOracleDatabaseActive() ? "Oracle Database JDBC" : "Embedded Local Engine");
            dbStatus.addProperty("product", conn.getMetaData().getDatabaseProductName());
            dbStatus.addProperty("version", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            dbStatus.addProperty("status", "ERROR");
            dbStatus.addProperty("message", e.getMessage());
        }
        health.add("database", dbStatus);

        // 2. Check ML Service
        JsonObject mlStatus = new JsonObject();
        String mlUrl = System.getenv(MLPredictionService.ENV_ML_SERVICE_URL);
        if (mlUrl == null || mlUrl.trim().isEmpty()) {
            mlUrl = "http://localhost:5001/health";
        } else {
            // Replace /predict with /health
            mlUrl = mlUrl.replace("/predict", "/health");
        }

        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(mlUrl)).timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            mlStatus.addProperty("status", resp.statusCode() == 200 ? "REACHABLE" : "DEGRADED");
            mlStatus.addProperty("endpoint", mlUrl);
        } catch (Exception e) {
            mlStatus.addProperty("status", "UNREACHABLE");
            mlStatus.addProperty("endpoint", mlUrl);
            mlStatus.addProperty("note", "Fallback heuristic prediction mode active");
        }
        health.add("ml_service", mlStatus);

        response.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(health));
        out.flush();
    }
}
