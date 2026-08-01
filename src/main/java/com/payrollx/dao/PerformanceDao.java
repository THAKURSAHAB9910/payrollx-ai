package com.payrollx.dao;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.model.Performance;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Performance entity.
 */
public class PerformanceDao {
    private static final Logger LOGGER = Logger.getLogger(PerformanceDao.class.getName());
    private final DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();

    public boolean create(Performance perf) {
        String sql = "INSERT INTO performance (employee_id, rating, kpi_score, feedback, evaluation_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, perf.getEmployeeId());
            stmt.setDouble(2, perf.getRating());
            stmt.setDouble(3, perf.getKpiScore());
            stmt.setString(4, perf.getFeedback());
            stmt.setDate(5, Date.valueOf(perf.getEvaluationDate()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        perf.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating performance record for employee " + perf.getEmployeeId(), e);
        }
        return false;
    }

    public List<Performance> getByEmployeeId(int employeeId) {
        List<Performance> list = new ArrayList<>();
        String sql = "SELECT * FROM performance WHERE employee_id = ? ORDER BY evaluation_date DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPerformance(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching performance records for employee " + employeeId, e);
        }
        return list;
    }

    public Performance getRecentPerformance(int employeeId) {
        String sql = "SELECT * FROM performance WHERE employee_id = ? ORDER BY evaluation_date DESC LIMIT 1";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPerformance(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching recent performance record for employee " + employeeId, e);
        }
        return null;
    }

    public List<Performance> getAll() {
        List<Performance> list = new ArrayList<>();
        String sql = "SELECT * FROM performance ORDER BY evaluation_date DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToPerformance(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing all performance records", e);
        }
        return list;
    }

    public boolean update(Performance perf) {
        String sql = "UPDATE performance SET rating = ?, kpi_score = ?, feedback = ?, evaluation_date = ? WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, perf.getRating());
            stmt.setDouble(2, perf.getKpiScore());
            stmt.setString(3, perf.getFeedback());
            stmt.setDate(4, Date.valueOf(perf.getEvaluationDate()));
            stmt.setInt(5, perf.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating performance record ID " + perf.getId(), e);
        }
        return false;
    }

    private Performance mapResultSetToPerformance(ResultSet rs) throws SQLException {
        Performance perf = new Performance();
        perf.setId(rs.getInt("id"));
        perf.setEmployeeId(rs.getInt("employee_id"));
        perf.setRating(rs.getDouble("rating"));
        perf.setKpiScore(rs.getDouble("kpi_score"));
        perf.setFeedback(rs.getString("feedback"));
        perf.setEvaluationDate(rs.getDate("evaluation_date").toLocalDate());
        return perf;
    }
}
