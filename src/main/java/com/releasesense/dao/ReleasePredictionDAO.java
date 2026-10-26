package com.releasesense.dao;

import com.releasesense.model.PredictionResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for performing JDBC CRUD operations on RELEASE_PREDICTIONS table.
 */
public class ReleasePredictionDAO {

    private static final Logger LOGGER = Logger.getLogger(ReleasePredictionDAO.class.getName());

    /**
     * Persists a release prediction result into Oracle Database.
     *
     * @param result PredictionResult object to persist
     * @return true if record was saved successfully, false otherwise
     */
    public boolean savePrediction(PredictionResult result) {
        String insertSql = "INSERT INTO RELEASE_PREDICTIONS (" +
                "CHANGED_FILES, FAILED_TESTS, CRITICAL_BUGS, COVERAGE, " +
                "PREVIOUS_FAILURE_RATE, RISK_LEVEL, CONFIDENCE_PERCENT, RECOMMENDATION, CREATED_AT" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, new String[]{"ID"})) {

            stmt.setInt(1, result.getChangedFiles());
            stmt.setInt(2, result.getFailedTests());
            stmt.setInt(3, result.getCriticalBugs());
            stmt.setDouble(4, result.getCoverage());
            stmt.setDouble(5, result.getPreviousFailureRate());
            stmt.setString(6, result.getRiskLevel());
            stmt.setDouble(7, result.getConfidencePercent());
            stmt.setString(8, result.getRecommendation());

            Timestamp ts = result.getCreatedAt() != null ? result.getCreatedAt() : new Timestamp(System.currentTimeMillis());
            stmt.setTimestamp(9, ts);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys != null && generatedKeys.next()) {
                        result.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error saving prediction to database: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Retrieves recent release predictions ordered by creation timestamp descending.
     *
     * @param limit maximum number of records to return
     * @return List of PredictionResult objects
     */
    public List<PredictionResult> getAllPredictions(int limit) {
        List<PredictionResult> list = new ArrayList<>();
        int maxRows = limit > 0 ? limit : 50;

        String querySql = "SELECT ID, CHANGED_FILES, FAILED_TESTS, CRITICAL_BUGS, COVERAGE, " +
                "PREVIOUS_FAILURE_RATE, RISK_LEVEL, CONFIDENCE_PERCENT, RECOMMENDATION, CREATED_AT " +
                "FROM RELEASE_PREDICTIONS " +
                "ORDER BY CREATED_AT DESC " +
                "FETCH FIRST ? ROWS ONLY";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(querySql)) {

            stmt.setInt(1, maxRows);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PredictionResult item = new PredictionResult();
                    item.setId(rs.getLong("ID"));
                    item.setChangedFiles(rs.getInt("CHANGED_FILES"));
                    item.setFailedTests(rs.getInt("FAILED_TESTS"));
                    item.setCriticalBugs(rs.getInt("CRITICAL_BUGS"));
                    item.setCoverage(rs.getDouble("COVERAGE"));
                    item.setPreviousFailureRate(rs.getDouble("PREVIOUS_FAILURE_RATE"));
                    item.setRiskLevel(rs.getString("RISK_LEVEL"));
                    item.setConfidencePercent(rs.getDouble("CONFIDENCE_PERCENT"));
                    item.setRecommendation(rs.getString("RECOMMENDATION"));
                    item.setCreatedAt(rs.getTimestamp("CREATED_AT"));

                    list.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL error querying predictions from database: " + e.getMessage(), e);
        }

        return list;
    }

    /**
     * Retrieves aggregated statistics for dashboard KPIs.
     */
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        String countSql = "SELECT " +
                "COUNT(*) AS TOTAL, " +
                "SUM(CASE WHEN RISK_LEVEL = 'Low' THEN 1 ELSE 0 END) AS LOW_COUNT, " +
                "SUM(CASE WHEN RISK_LEVEL = 'Medium' THEN 1 ELSE 0 END) AS MED_COUNT, " +
                "SUM(CASE WHEN RISK_LEVEL = 'High' THEN 1 ELSE 0 END) AS HIGH_COUNT, " +
                "AVG(COVERAGE) AS AVG_COVERAGE, " +
                "AVG(CONFIDENCE_PERCENT) AS AVG_CONFIDENCE " +
                "FROM RELEASE_PREDICTIONS";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(countSql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.put("totalReleases", rs.getInt("TOTAL"));
                stats.put("lowRiskCount", rs.getInt("LOW_COUNT"));
                stats.put("mediumRiskCount", rs.getInt("MED_COUNT"));
                stats.put("highRiskCount", rs.getInt("HIGH_COUNT"));
                stats.put("avgCoverage", Math.round(rs.getDouble("AVG_COVERAGE") * 100.0) / 100.0);
                stats.put("avgConfidence", Math.round(rs.getDouble("AVG_CONFIDENCE") * 100.0) / 100.0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to compute dashboard stats: " + e.getMessage());
            stats.put("totalReleases", 0);
            stats.put("lowRiskCount", 0);
            stats.put("mediumRiskCount", 0);
            stats.put("highRiskCount", 0);
            stats.put("avgCoverage", 0.0);
            stats.put("avgConfidence", 0.0);
        }

        return stats;
    }
}
