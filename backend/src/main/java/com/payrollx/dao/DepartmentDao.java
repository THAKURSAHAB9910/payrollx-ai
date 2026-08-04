package com.payrollx.dao;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Department entity.
 */
public class DepartmentDao {
    private static final Logger LOGGER = Logger.getLogger(DepartmentDao.class.getName());
    private final DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();

    public boolean create(Department dept) {
        String sql = "INSERT INTO departments (dept_name, manager_id, budget) VALUES (?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, dept.getDeptName());
            if (dept.getManagerId() != null) stmt.setInt(2, dept.getManagerId());
            else stmt.setNull(2, Types.INTEGER);
            stmt.setDouble(3, dept.getBudget());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        dept.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating department " + dept.getDeptName(), e);
        }
        return false;
    }

    public Department getById(int id) {
        String sql = "SELECT * FROM departments WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDepartment(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching department by ID " + id, e);
        }
        return null;
    }

    public Department getByName(String name) {
        String sql = "SELECT * FROM departments WHERE dept_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDepartment(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching department by name " + name, e);
        }
        return null;
    }

    public List<Department> getAll() {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT * FROM departments";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDepartment(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing all departments", e);
        }
        return list;
    }

    public boolean update(Department dept) {
        String sql = "UPDATE departments SET dept_name = ?, manager_id = ?, budget = ? WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dept.getDeptName());
            if (dept.getManagerId() != null) stmt.setInt(2, dept.getManagerId());
            else stmt.setNull(2, Types.INTEGER);
            stmt.setDouble(3, dept.getBudget());
            stmt.setInt(4, dept.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating department " + dept.getDeptName(), e);
        }
        return false;
    }

    private Department mapResultSetToDepartment(ResultSet rs) throws SQLException {
        Department dept = new Department();
        dept.setId(rs.getInt("id"));
        dept.setDeptName(rs.getString("dept_name"));
        int mgrIdVal = rs.getInt("manager_id");
        dept.setManagerId(rs.wasNull() ? null : mgrIdVal);
        dept.setBudget(rs.getDouble("budget"));
        return dept;
    }
}
