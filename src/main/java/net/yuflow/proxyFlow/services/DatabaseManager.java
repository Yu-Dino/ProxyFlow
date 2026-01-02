package net.yuflow.proxyFlow.services;

import org.slf4j.Logger;
import java.nio.file.Path;
import java.sql.*;

public class DatabaseManager {
    private final Logger logger;
    private final String connectionString;
    private Connection connection;

    public DatabaseManager(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.connectionString = "jdbc:sqlite:" + dataDirectory.resolve("database.db").toString();
        this.initialize();
    }

    private void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(this.connectionString);
            try (Statement statement = this.connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS active_bans (" +
                        "ip_address TEXT PRIMARY KEY, " +
                        "expiry_time LONG NOT NULL)");
            }
        } catch (ClassNotFoundException | SQLException e) {
            this.logger.error("Failed to initialize SQLite database", e);
        }
    }

    public void addBan(String ipAddress, long expiryTime) {
        String sql = "INSERT OR REPLACE INTO active_bans (ip_address, expiry_time) VALUES (?, ?)";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setLong(2, expiryTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            this.logger.error("Failed to add ban to database", e);
        }
    }

    public boolean isBanned(String ipAddress) {
        String sql = "SELECT expiry_time FROM active_bans WHERE ip_address = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long expiry = rs.getLong("expiry_time");
                    if (System.currentTimeMillis() < expiry) {
                        return true;
                    } else {
                        removeBan(ipAddress);
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            this.logger.error("Failed to check ban status", e);
        }
        return false;
    }

    public void removeBan(String ipAddress) {
        String sql = "DELETE FROM active_bans WHERE ip_address = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            this.logger.error("Failed to remove ban from database", e);
        }
    }

    public void close() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (SQLException e) {
            this.logger.error("Failed to close database connection", e);
        }
    }
}