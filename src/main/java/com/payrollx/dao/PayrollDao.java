package com.payrollx.dao;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.model.Payroll;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Payroll entity.
 */
public class PayrollDao {
    private static final Logger LOGGER = Logger.getLogger(PayrollDao.class.getName());
    private final DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();

    public boolean create(Payroll p) {
        String sql = "INSERT INTO payroll (employee_id, payroll_month, basic_salary, hra, da, bonus, " +
                     "medical_allowance, travel_allowance, overtime_pay, pf, tax, insurance, other_deductions, " +
                     "net_salary, processed_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, p.getEmployeeId());
            stmt.setString(2, p.getPayrollMonth());
            stmt.setDouble(3, p.getBasicSalary());
            stmt.setDouble(4, p.getHra());
            stmt.setDouble(5, p.getDa());
            stmt.setDouble(6, p.getBonus());
            stmt.setDouble(7, p.getMedicalAllowance());
            stmt.setDouble(8, p.getTravelAllowance());
            stmt.setDouble(9, p.getOvertimePay());
            stmt.setDouble(10, p.getPf());
            stmt.setDouble(11, p.getTax());
            stmt.setDouble(12, p.getInsurance());
            stmt.setDouble(13, p.getOtherDeductions());
            stmt.setDouble(14, p.getNetSalary());
            stmt.setDate(15, Date.valueOf(p.getProcessedDate()));
            stmt.setString(16, p.getStatus());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating payroll record for employee " + p.getEmployeeId(), e);
        }
        return false;
    }

    public Payroll getById(int id) {
        String sql = "SELECT * FROM payroll WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPayroll(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching payroll by ID " + id, e);
        }
        return null;
    }

    public Payroll getByEmployeeIdAndMonth(int employeeId, String yearMonth) {
        String sql = "SELECT * FROM payroll WHERE employee_id = ? AND payroll_month = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setString(2, yearMonth);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPayroll(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching payroll for emp " + employeeId + " on " + yearMonth, e);
        }
        return null;
    }

    public List<Payroll> getByEmployeeId(int employeeId) {
        List<Payroll> list = new ArrayList<>();
        String sql = "SELECT * FROM payroll WHERE employee_id = ? ORDER BY payroll_month DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPayroll(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching payroll history for employee " + employeeId, e);
        }
        return list;
    }

    public List<Payroll> getByMonth(String yearMonth) {
        List<Payroll> list = new ArrayList<>();
        String sql = "SELECT * FROM payroll WHERE payroll_month = ? ORDER BY employee_id ASC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, yearMonth);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPayroll(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching payroll records for month " + yearMonth, e);
        }
        return list;
    }

    public List<Payroll> getAll() {
        List<Payroll> list = new ArrayList<>();
        String sql = "SELECT * FROM payroll ORDER BY payroll_month DESC, employee_id ASC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToPayroll(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listing all payroll entries", e);
        }
        return list;
    }

    public boolean update(Payroll p) {
        String sql = "UPDATE payroll SET basic_salary = ?, hra = ?, da = ?, bonus = ?, medical_allowance = ?, " +
                     "travel_allowance = ?, overtime_pay = ?, pf = ?, tax = ?, insurance = ?, other_deductions = ?, " +
                     "net_salary = ?, processed_date = ?, status = ? WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, p.getBasicSalary());
            stmt.setDouble(2, p.getHra());
            stmt.setDouble(3, p.getDa());
            stmt.setDouble(4, p.getBonus());
            stmt.setDouble(5, p.getMedicalAllowance());
            stmt.setDouble(6, p.getTravelAllowance());
            stmt.setDouble(7, p.getOvertimePay());
            stmt.setDouble(8, p.getPf());
            stmt.setDouble(9, p.getTax());
            stmt.setDouble(10, p.getInsurance());
            stmt.setDouble(11, p.getOtherDeductions());
            stmt.setDouble(12, p.getNetSalary());
            stmt.setDate(13, Date.valueOf(p.getProcessedDate()));
            stmt.setString(14, p.getStatus());
            stmt.setInt(15, p.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating payroll record ID " + p.getId(), e);
        }
        return false;
    }

    private Payroll mapResultSetToPayroll(ResultSet rs) throws SQLException {
        Payroll p = new Payroll();
        p.setId(rs.getInt("id"));
        p.setEmployeeId(rs.getInt("employee_id"));
        p.setPayrollMonth(rs.getString("payroll_month"));
        p.setBasicSalary(rs.getDouble("basic_salary"));
        p.setHra(rs.getDouble("hra"));
        p.setDa(rs.getDouble("da"));
        p.setBonus(rs.getDouble("bonus"));
        p.setMedicalAllowance(rs.getDouble("medical_allowance"));
        p.setTravelAllowance(rs.getDouble("travel_allowance"));
        p.setOvertimePay(rs.getDouble("overtime_pay"));
        p.setPf(rs.getDouble("pf"));
        p.setTax(rs.getDouble("tax"));
        p.setInsurance(rs.getDouble("insurance"));
        p.setOtherDeductions(rs.getDouble("other_deductions"));
        p.setNetSalary(rs.getDouble("net_salary"));
        p.setProcessedDate(rs.getDate("processed_date").toLocalDate());
        p.setStatus(rs.getString("status"));
        return p;
    }
}
