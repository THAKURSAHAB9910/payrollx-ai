package com.payrollx.dao;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.model.LeaveRequest;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for LeaveRequest entity.
 */
public class LeaveRequestDao {
    private static final Logger LOGGER = Logger.getLogger(LeaveRequestDao.class.getName());
    private final DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();

    public boolean create(LeaveRequest request) {
        String sql = "INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, status, manager_id, comment) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, request.getEmployeeId());
            stmt.setString(2, request.getLeaveType());
            stmt.setDate(3, Date.valueOf(request.getStartDate()));
            stmt.setDate(4, Date.valueOf(request.getEndDate()));
            stmt.setString(5, request.getStatus());
            stmt.setInt(6, request.getManagerId());
            stmt.setString(7, request.getComment());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        request.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating leave request for employee " + request.getEmployeeId(), e);
        }
        return false;
    }

    public LeaveRequest getById(int id) {
        String sql = "SELECT * FROM leave_requests WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLeaveRequest(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching leave request by ID " + id, e);
        }
        return null;
    }

    public List<LeaveRequest> getByEmployeeId(int employeeId) {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM leave_requests WHERE employee_id = ? ORDER BY start_date DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLeaveRequest(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching leave requests for employee " + employeeId, e);
        }
        return list;
    }

    public List<LeaveRequest> getByManagerId(int managerId) {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM leave_requests WHERE manager_id = ? ORDER BY start_date DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, managerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLeaveRequest(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching leave requests for manager " + managerId, e);
        }
        return list;
    }

    public List<LeaveRequest> getPendingRequestsByManager(int managerId) {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM leave_requests WHERE manager_id = ? AND status = 'PENDING' ORDER BY start_date ASC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, managerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLeaveRequest(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching pending leave requests for manager " + managerId, e);
        }
        return list;
    }

    public boolean update(LeaveRequest request) {
        String sql = "UPDATE leave_requests SET leave_type = ?, start_date = ?, end_date = ?, status = ?, manager_id = ?, comment = ? WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, request.getLeaveType());
            stmt.setDate(2, Date.valueOf(request.getStartDate()));
            stmt.setDate(3, Date.valueOf(request.getEndDate()));
            stmt.setString(4, request.getStatus());
            stmt.setInt(5, request.getManagerId());
            stmt.setString(6, request.getComment());
            stmt.setInt(7, request.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating leave request ID " + request.getId(), e);
        }
        return false;
    }

    private LeaveRequest mapResultSetToLeaveRequest(ResultSet rs) throws SQLException {
        LeaveRequest req = new LeaveRequest();
        req.setId(rs.getInt("id"));
        req.setEmployeeId(rs.getInt("employee_id"));
        req.setLeaveType(rs.getString("leave_type"));
        req.setStartDate(rs.getDate("start_date").toLocalDate());
        req.setEndDate(rs.getDate("end_date").toLocalDate());
        req.setStatus(rs.getString("status"));
        req.setManagerId(rs.getInt("manager_id"));
        req.setComment(rs.getString("comment"));
        return req;
    }
}
