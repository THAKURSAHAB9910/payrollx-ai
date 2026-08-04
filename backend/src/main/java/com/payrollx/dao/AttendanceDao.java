package com.payrollx.dao;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.model.Attendance;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Attendance entity.
 */
public class AttendanceDao {
    private static final Logger LOGGER = Logger.getLogger(AttendanceDao.class.getName());
    private final DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();

    public boolean create(Attendance att) {
        String sql = "INSERT INTO attendance (employee_id, attendance_date, check_in, check_out, status, overtime_hours) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, att.getEmployeeId());
            stmt.setDate(2, Date.valueOf(att.getDate()));
            stmt.setTimestamp(3, att.getCheckIn() != null ? Timestamp.valueOf(att.getCheckIn()) : null);
            stmt.setTimestamp(4, att.getCheckOut() != null ? Timestamp.valueOf(att.getCheckOut()) : null);
            stmt.setString(5, att.getStatus());
            stmt.setDouble(6, att.getOvertimeHours());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        att.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating attendance for employee " + att.getEmployeeId(), e);
        }
        return false;
    }

    public Attendance getById(int id) {
        String sql = "SELECT * FROM attendance WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAttendance(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching attendance by ID " + id, e);
        }
        return null;
    }

    public Attendance getByEmployeeIdAndDate(int employeeId, LocalDate date) {
        String sql = "SELECT * FROM attendance WHERE employee_id = ? AND attendance_date = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAttendance(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching attendance for emp " + employeeId + " on " + date, e);
        }
        return null;
    }

    public List<Attendance> getByEmployeeId(int employeeId) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance WHERE employee_id = ? ORDER BY attendance_date DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing attendance for employee " + employeeId, e);
        }
        return list;
    }

    public List<Attendance> getByMonth(int employeeId, String yearMonth) {
        List<Attendance> list = new ArrayList<>();
        // Format: 'YYYY-MM' -> match dates that start with that string, or range query
        String sql = "SELECT * FROM attendance WHERE employee_id = ? AND TO_CHAR(attendance_date, 'YYYY-MM') = ? ORDER BY attendance_date ASC";
        // To make it cross-db compatible (H2 supports TO_CHAR in MySQL/Postgres modes, but let's do a robust range query or LIKE query)
        // Since we store date as DATE, we can query: attendance_date LIKE 'YYYY-MM%' or use functions.
        // Let's use: YEAR(attendance_date) = ? AND MONTH(attendance_date) = ? which is ANSI SQL standard and supported by H2, MySQL, and Postgres
        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        String ansiSql = "SELECT * FROM attendance WHERE employee_id = ? AND EXTRACT(YEAR FROM attendance_date) = ? AND EXTRACT(MONTH FROM attendance_date) = ? ORDER BY attendance_date ASC";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ansiSql)) {
            stmt.setInt(1, employeeId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing monthly attendance for employee " + employeeId, e);
        }
        return list;
    }

    public boolean update(Attendance att) {
        String sql = "UPDATE attendance SET check_in = ?, check_out = ?, status = ?, overtime_hours = ? WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, att.getCheckIn() != null ? Timestamp.valueOf(att.getCheckIn()) : null);
            stmt.setTimestamp(2, att.getCheckOut() != null ? Timestamp.valueOf(att.getCheckOut()) : null);
            stmt.setString(3, att.getStatus());
            stmt.setDouble(4, att.getOvertimeHours());
            stmt.setInt(5, att.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating attendance ID " + att.getId(), e);
        }
        return false;
    }

    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance att = new Attendance();
        att.setId(rs.getInt("id"));
        att.setEmployeeId(rs.getInt("employee_id"));
        att.setDate(rs.getDate("attendance_date").toLocalDate());
        Timestamp checkInTs = rs.getTimestamp("check_in");
        att.setCheckIn(checkInTs != null ? checkInTs.toLocalDateTime() : null);
        Timestamp checkOutTs = rs.getTimestamp("check_out");
        att.setCheckOut(checkOutTs != null ? checkOutTs.toLocalDateTime() : null);
        att.setStatus(rs.getString("status"));
        att.setOvertimeHours(rs.getDouble("overtime_hours"));
        return att;
    }
}
