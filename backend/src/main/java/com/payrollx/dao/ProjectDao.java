package com.payrollx.dao;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.model.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Project and EmployeeProject relationships.
 */
public class ProjectDao {
    private static final Logger LOGGER = Logger.getLogger(ProjectDao.class.getName());
    private final DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();

    public boolean create(Project project) {
        String sql = "INSERT INTO projects (project_name, budget, status) VALUES (?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, project.getProjectName());
            stmt.setDouble(2, project.getBudget());
            stmt.setString(3, project.getStatus());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        project.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating project " + project.getProjectName(), e);
        }
        return false;
    }

    public Project getById(int id) {
        String sql = "SELECT * FROM projects WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProject(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching project by ID " + id, e);
        }
        return null;
    }

    public List<Project> getAll() {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM projects";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing all projects", e);
        }
        return list;
    }

    public boolean update(Project project) {
        String sql = "UPDATE projects SET project_name = ?, budget = ?, status = ? WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, project.getProjectName());
            stmt.setDouble(2, project.getBudget());
            stmt.setString(3, project.getStatus());
            stmt.setInt(4, project.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating project " + project.getProjectName(), e);
        }
        return false;
    }

    // Many-to-Many Operations

    public boolean allocateEmployeeToProject(int employeeId, int projectId, double hoursAllocated) {
        String checkSql = "SELECT COUNT(*) FROM employee_projects WHERE employee_id = ? AND project_id = ?";
        String insertSql = "INSERT INTO employee_projects (employee_id, project_id, hours_allocated) VALUES (?, ?, ?)";
        String updateSql = "UPDATE employee_projects SET hours_allocated = ? WHERE employee_id = ? AND project_id = ?";

        try (Connection conn = connectionManager.getConnection()) {
            // Check if mapping exists
            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, employeeId);
                checkStmt.setInt(2, projectId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        exists = rs.getInt(1) > 0;
                    }
                }
            }

            if (exists) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setDouble(1, hoursAllocated);
                    updateStmt.setInt(2, employeeId);
                    updateStmt.setInt(3, projectId);
                    return updateStmt.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, employeeId);
                    insertStmt.setInt(2, projectId);
                    insertStmt.setDouble(3, hoursAllocated);
                    return insertStmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error allocating employee " + employeeId + " to project " + projectId, e);
        }
        return false;
    }

    public List<Project> getProjectsForEmployee(int employeeId) {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT p.* FROM projects p INNER JOIN employee_projects ep ON p.id = ep.project_id WHERE ep.employee_id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProject(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching projects for employee " + employeeId, e);
        }
        return list;
    }

    public Map<Integer, Double> getEmployeesForProject(int projectId) {
        Map<Integer, Double> map = new HashMap<>();
        String sql = "SELECT employee_id, hours_allocated FROM employee_projects WHERE project_id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("employee_id"), rs.getDouble("hours_allocated"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching employees for project " + projectId, e);
        }
        return map;
    }

    public boolean removeEmployeeFromProject(int employeeId, int projectId) {
        String sql = "DELETE FROM employee_projects WHERE employee_id = ? AND project_id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setInt(2, projectId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error removing employee " + employeeId + " from project " + projectId, e);
        }
        return false;
    }

    private Project mapResultSetToProject(ResultSet rs) throws SQLException {
        Project proj = new Project();
        proj.setId(rs.getInt("id"));
        proj.setProjectName(rs.getString("project_name"));
        proj.setBudget(rs.getDouble("budget"));
        proj.setStatus(rs.getString("status"));
        return proj;
    }
}
