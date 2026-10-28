package com.releasesense.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connectivity for ReleaseSense using JDBC.
 * Reads connection settings from environment variables:
 * - ORACLE_DB_URL: (e.g. jdbc:oracle:thin:@//localhost:1521/FREE or XEPDB1)
 * - ORACLE_DB_USER: Oracle user
 * - ORACLE_DB_PASSWORD: User password
 *
 * If Oracle environment variables are not supplied or the Oracle database is
 * unreachable during local development, it gracefully initializes an embedded
 * Oracle-compatibility mode database so developers can test and develop without blockers.
 */
public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    // Environment variable names
    public static final String ENV_ORACLE_URL = "ORACLE_DB_URL";
    public static final String ENV_ORACLE_USER = "ORACLE_DB_USER";
    public static final String ENV_ORACLE_PASSWORD = "ORACLE_DB_PASSWORD";

    private static final String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
    private static final String FALLBACK_DRIVER = "org.h2.Driver";
    private static final String FALLBACK_URL = "jdbc:h2:mem:releasesense;MODE=Oracle;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

    private static volatile boolean initialized = false;
    private static boolean isOracleActive = false;

    private DatabaseConfig() {
        // Utility class
    }

    /**
     * Obtains a JDBC Connection according to configured environment variables.
     */
    public static Connection getConnection() throws SQLException {
        ensureInitialized();

        String oracleUrl = System.getenv(ENV_ORACLE_URL);
        String oracleUser = System.getenv(ENV_ORACLE_USER);
        String oraclePassword = System.getenv(ENV_ORACLE_PASSWORD);

        if (oracleUrl != null && !oracleUrl.trim().isEmpty()) {
            try {
                Class.forName(ORACLE_DRIVER);
                Connection conn = DriverManager.getConnection(oracleUrl, oracleUser, oraclePassword);
                isOracleActive = true;
                return conn;
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "Oracle JDBC driver class not found on classpath: " + e.getMessage());
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Unable to connect to Oracle Database at " + oracleUrl + ": " + e.getMessage() + ". Using local fallback.");
            }
        }

        // Fallback to embedded in-memory database with Oracle mode
        try {
            Class.forName(FALLBACK_DRIVER);
            isOracleActive = false;
            return DriverManager.getConnection(FALLBACK_URL, "sa", "");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Neither Oracle nor fallback JDBC driver is available.", e);
        }
    }

    /**
     * Initializes the database schema if not present.
     */
    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }

        String oracleUrl = System.getenv(ENV_ORACLE_URL);
        if (oracleUrl != null && !oracleUrl.trim().isEmpty()) {
            LOGGER.info("[ReleaseSense DB] Configured to connect to Oracle Database via " + oracleUrl);
            isOracleActive = true;
        } else {
            LOGGER.info("[ReleaseSense DB] " + ENV_ORACLE_URL + " not configured. Utilizing local Oracle-compatible database engine for development.");
            isOracleActive = false;
            setupFallbackSchema();
        }

        initialized = true;
    }

    private static void setupFallbackSchema() {
    try {
        Class.forName(FALLBACK_DRIVER);

        try (Connection conn = DriverManager.getConnection(FALLBACK_URL, "sa", "");
             Statement stmt = conn.createStatement()) {

            String createTableSql = "CREATE TABLE IF NOT EXISTS RELEASE_PREDICTIONS (" +
                    "ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "CHANGED_FILES NUMBER(6) NOT NULL," +
                    "FAILED_TESTS NUMBER(6) NOT NULL," +
                    "CRITICAL_BUGS NUMBER(6) NOT NULL," +
                    "COVERAGE NUMBER(5, 2) NOT NULL," +
                    "PREVIOUS_FAILURE_RATE NUMBER(5, 2) NOT NULL," +
                    "RISK_LEVEL VARCHAR2(20) NOT NULL," +
                    "CONFIDENCE_PERCENT NUMBER(5, 2) NOT NULL," +
                    "RECOMMENDATION VARCHAR2(1000) NOT NULL," +
                    "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL" +
                    ")";

            stmt.execute(createTableSql);

            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM RELEASE_PREDICTIONS")) {
                if (rs.next() && rs.getInt(1) == 0) {

                    stmt.executeUpdate("INSERT INTO RELEASE_PREDICTIONS (CHANGED_FILES, FAILED_TESTS, CRITICAL_BUGS, COVERAGE, PREVIOUS_FAILURE_RATE, RISK_LEVEL, CONFIDENCE_PERCENT, RECOMMENDATION) " +
                            "VALUES (18, 0, 0, 88.50, 2.50, 'Low', 96.40, 'LOW RISK: Release meets automated quality gates. Recommended for deployment.')");

                    stmt.executeUpdate("INSERT INTO RELEASE_PREDICTIONS (CHANGED_FILES, FAILED_TESTS, CRITICAL_BUGS, COVERAGE, PREVIOUS_FAILURE_RATE, RISK_LEVEL, CONFIDENCE_PERCENT, RECOMMENDATION) " +
                            "VALUES (142, 2, 1, 64.20, 24.00, 'Medium', 88.10, 'MODERATE RISK: Release requires targeted manual verification and canary deployment.')");

                    stmt.executeUpdate("INSERT INTO RELEASE_PREDICTIONS (CHANGED_FILES, FAILED_TESTS, CRITICAL_BUGS, COVERAGE, PREVIOUS_FAILURE_RATE, RISK_LEVEL, CONFIDENCE_PERCENT, RECOMMENDATION) " +
                            "VALUES (310, 8, 3, 42.10, 45.00, 'High', 98.70, 'HIGH RISK RELEASE: Deployment blocked immediately until critical bugs and test failures are resolved.')");
                }
            }

            LOGGER.info("[ReleaseSense DB] Local fallback schema initialized with seed data.");
        }

    } catch (ClassNotFoundException | SQLException e) {
        LOGGER.log(Level.SEVERE,
                "Failed to initialize fallback database schema: " + e.getMessage(), e);
    }
}

    public static boolean isOracleDatabaseActive() {
        return isOracleActive;
    }
}
