package com.releasesense.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.releasesense.dao.ReleasePredictionDAO;
import com.releasesense.model.PredictionResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jakarta Servlet endpoint (/api/history) for retrieving past release predictions
 * and aggregated quality KPI metrics for the dashboard.
 */
@WebServlet(name = "HistoryServlet", urlPatterns = {"/api/history"})
public class HistoryServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(HistoryServlet.class.getName());

    private ReleasePredictionDAO predictionDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        super.init();
        this.predictionDAO = new ReleasePredictionDAO();
        this.gson = new Gson();
        LOGGER.info("[HistoryServlet] Initialized successfully.");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCORSHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCORSHeaders(response);
        response.setContentType("application/json; charset=UTF-8");

        PrintWriter out = response.getWriter();
        JsonObject responseJson = new JsonObject();

        try {
            int limit = 50;
            String limitParam = request.getParameter("limit");
            if (limitParam != null && !limitParam.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            // Retrieve history records and dashboard statistics
            List<PredictionResult> history = predictionDAO.getAllPredictions(limit);
            Map<String, Object> stats = predictionDAO.getDashboardStatistics();

            response.setStatus(HttpServletResponse.SC_OK);
            responseJson.addProperty("status", "success");
            responseJson.addProperty("count", history.size());
            responseJson.add("statistics", gson.toJsonTree(stats));
            responseJson.add("history", gson.toJsonTree(history));
            out.print(gson.toJson(responseJson));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving release history: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJson.addProperty("status", "error");
            responseJson.addProperty("message", "Unable to retrieve release history: " + e.getMessage());
            out.print(gson.toJson(responseJson));
        } finally {
            out.flush();
        }
    }

    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
