package com.payrollx.dao;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Notification entity.
 */
public class NotificationDao {
    private static final Logger LOGGER = Logger.getLogger(NotificationDao.class.getName());
    private final DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();

    public boolean create(Notification notif) {
        String sql = "INSERT INTO notifications (employee_id, notification_type, message, is_read, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, notif.getEmployeeId());
            stmt.setString(2, notif.getNotificationType());
            stmt.setString(3, notif.getMessage());
            stmt.setBoolean(4, notif.isRead());
            stmt.setTimestamp(5, Timestamp.valueOf(notif.getCreatedAt()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        notif.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating notification for employee " + notif.getEmployeeId(), e);
        }
        return false;
    }

    public List<Notification> getByEmployeeId(int employeeId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE employee_id = ? ORDER BY created_at DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching notifications for employee " + employeeId, e);
        }
        return list;
    }

    public List<Notification> getUnreadByEmployeeId(int employeeId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE employee_id = ? AND is_read = FALSE ORDER BY created_at DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching unread notifications for employee " + employeeId, e);
        }
        return list;
    }

    public boolean markAsRead(int id) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error marking notification as read for ID " + id, e);
        }
        return false;
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notif = new Notification();
        notif.setId(rs.getInt("id"));
        notif.setEmployeeId(rs.getInt("employee_id"));
        notif.setNotificationType(rs.getString("notification_type"));
        notif.setMessage(rs.getString("message"));
        notif.setRead(rs.getBoolean("is_read"));
        notif.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return notif;
    }
}
