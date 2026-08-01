package com.payrollx.dao;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.model.Employee;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Employee entity.
 */
public class EmployeeDao {
    private static final Logger LOGGER = Logger.getLogger(EmployeeDao.class.getName());
    private final DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();

    public boolean create(Employee emp) {
        String sql = "INSERT INTO employees (user_id, first_name, last_name, email, phone, hire_date, " +
                     "department_id, manager_id, position, salary, status, bank_account) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, emp.getUserId());
            stmt.setString(2, emp.getFirstName());
            stmt.setString(3, emp.getLastName());
            stmt.setString(4, emp.getEmail());
            stmt.setString(5, emp.getPhone());
            stmt.setDate(6, Date.valueOf(emp.getHireDate()));
            if (emp.getDepartmentId() != null) stmt.setInt(7, emp.getDepartmentId());
            else stmt.setNull(7, Types.INTEGER);
            if (emp.getManagerId() != null) stmt.setInt(8, emp.getManagerId());
            else stmt.setNull(8, Types.INTEGER);
            stmt.setString(9, emp.getPosition());
            stmt.setDouble(10, emp.getSalary());
            stmt.setString(11, emp.getStatus());
            stmt.setString(12, emp.getBankAccount());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        emp.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating employee " + emp.getFullName(), e);
        }
        return false;
    }

    public Employee getById(int id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEmployee(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching employee by ID " + id, e);
        }
        return null;
    }

    public Employee getByUserId(int userId) {
        String sql = "SELECT * FROM employees WHERE user_id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEmployee(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching employee by User ID " + userId, e);
        }
        return null;
    }

    public Employee getByEmail(String email) {
        String sql = "SELECT * FROM employees WHERE email = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEmployee(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching employee by email " + email, e);
        }
        return null;
    }

    public List<Employee> getAll() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employees";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing all employees", e);
        }
        return list;
    }

    public List<Employee> getByDepartmentId(int deptId) {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employees WHERE department_id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deptId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEmployee(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing employees in dept " + deptId, e);
        }
        return list;
    }

    public boolean update(Employee emp) {
        String sql = "UPDATE employees SET first_name = ?, last_name = ?, email = ?, phone = ?, " +
                     "department_id = ?, manager_id = ?, position = ?, salary = ?, status = ?, bank_account = ? " +
                     "WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, emp.getFirstName());
            stmt.setString(2, emp.getLastName());
            stmt.setString(3, emp.getEmail());
            stmt.setString(4, emp.getPhone());
            if (emp.getDepartmentId() != null) stmt.setInt(5, emp.getDepartmentId());
            else stmt.setNull(5, Types.INTEGER);
            if (emp.getManagerId() != null) stmt.setInt(6, emp.getManagerId());
            else stmt.setNull(6, Types.INTEGER);
            stmt.setString(7, emp.getPosition());
            stmt.setDouble(8, emp.getSalary());
            stmt.setString(9, emp.getStatus());
            stmt.setString(10, emp.getBankAccount());
            stmt.setInt(11, emp.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating employee " + emp.getFullName(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM employees WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting employee ID " + id, e);
        }
        return false;
    }

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        LocalDate hDate = rs.getDate("hire_date").toLocalDate();
        int deptIdVal = rs.getInt("department_id");
        Integer deptId = rs.wasNull() ? null : deptIdVal;
        int mgrIdVal = rs.getInt("manager_id");
        Integer mgrId = rs.wasNull() ? null : mgrIdVal;

        return new Employee.Builder()
                .id(rs.getInt("id"))
                .userId(rs.getInt("user_id"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .hireDate(hDate)
                .departmentId(deptId)
                .managerId(mgrId)
                .position(rs.getString("position"))
                .salary(rs.getDouble("salary"))
                .status(rs.getString("status"))
                .bankAccount(rs.getString("bank_account"))
                .build();
    }
}
