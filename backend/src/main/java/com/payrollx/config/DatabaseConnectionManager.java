package com.payrollx.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton database connection manager supporting H2 (embedded/in-memory), MySQL, and PostgreSQL.
 */
public class DatabaseConnectionManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionManager.class.getName());
    private static DatabaseConnectionManager instance;
    
    private String dbType;
    private String url;
    private String username;
    private String password;
    
    // In-memory H2 connection keep-alive reference to prevent database cleanups between sessions
    private Connection keepAliveConnection;

    private DatabaseConnectionManager() {
        loadConfig();
        initDriver();
        if ("h2".equalsIgnoreCase(dbType)) {
            // Establish a keep-alive connection to keep the H2 DB in memory
            try {
                this.keepAliveConnection = DriverManager.getConnection(url, username, password);
                LOGGER.info("H2 In-Memory Keep-Alive connection established.");
                runSchemaScript(this.keepAliveConnection);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to establish H2 keep-alive connection", e);
            }
        }
    }

    private void runSchemaScript(Connection conn) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (input == null) {
                LOGGER.warning("schema.sql not found in classpath. Seeding skipped.");
                return;
            }
            try (java.util.Scanner scanner = new java.util.Scanner(input, "UTF-8").useDelimiter(";");
                 Statement stmt = conn.createStatement()) {
                while (scanner.hasNext()) {
                    String sql = scanner.next().trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            }
            LOGGER.info("schema.sql executed programmatically successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to execute schema.sql programmatically", e);
        }
    }

    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                LOGGER.warning("database.properties not found in classpath. Using default H2 Configuration.");
                this.dbType = "h2";
                this.url = "jdbc:h2:mem:payrollxdb;DB_CLOSE_DELAY=-1;MODE=MySQL";
                this.username = "sa";
                this.password = "";
                return;
            }
            props.load(input);
            this.dbType = props.getProperty("db.type", "h2");
            this.url = props.getProperty("db.url");
            this.username = props.getProperty("db.username");
            this.password = props.getProperty("db.password");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading database configuration. Fallback to H2.", ex);
            this.dbType = "h2";
            this.url = "jdbc:h2:mem:payrollxdb;DB_CLOSE_DELAY=-1;MODE=MySQL";
            this.username = "sa";
            this.password = "";
        }
    }

    private void initDriver() {
        try {
            switch (dbType.toLowerCase()) {
                case "mysql":
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    LOGGER.info("MySQL JDBC Driver registered.");
                    break;
                case "postgresql":
                    Class.forName("org.postgresql.Driver");
                    LOGGER.info("PostgreSQL JDBC Driver registered.");
                    break;
                case "h2":
                default:
                    Class.forName("org.h2.Driver");
                    LOGGER.info("H2 JDBC Driver registered.");
                    break;
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Database driver not found", e);
        }
    }

    /**
     * Obtains a new database connection.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Helper to close connections and statements safely.
     */
    public static void close(Connection conn, Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing statement", e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    /**
     * Shutdown the H2 keep-alive connection when application terminates.
     */
    public synchronized void shutdown() {
        if (keepAliveConnection != null) {
            try {
                keepAliveConnection.close();
                LOGGER.info("H2 keep-alive connection closed.");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing H2 keep-alive connection", e);
            }
        }
    }
}
