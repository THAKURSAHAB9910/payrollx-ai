package com.payrollx.dao;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.model.TaxRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for TaxRecord entity.
 */
public class TaxRecordDao {
    private static final Logger LOGGER = Logger.getLogger(TaxRecordDao.class.getName());
    private final DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();

    public boolean create(TaxRecord rec) {
        String sql = "INSERT INTO tax_records (employee_id, financial_year, taxable_income, tax_paid, deductions_declared, tax_saving_suggestions) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, rec.getEmployeeId());
            stmt.setString(2, rec.getFinancialYear());
            stmt.setDouble(3, rec.getTaxableIncome());
            stmt.setDouble(4, rec.getTaxPaid());
            stmt.setDouble(5, rec.getDeductionsDeclared());
            stmt.setString(6, rec.getTaxSavingSuggestions());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        rec.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating tax record for employee " + rec.getEmployeeId(), e);
        }
        return false;
    }

    public TaxRecord getByEmployeeIdAndYear(int employeeId, String financialYear) {
        String sql = "SELECT * FROM tax_records WHERE employee_id = ? AND financial_year = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setString(2, financialYear);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTaxRecord(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching tax record for employee " + employeeId + " and year " + financialYear, e);
        }
        return null;
    }

    public List<TaxRecord> getByEmployeeId(int employeeId) {
        List<TaxRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM tax_records WHERE employee_id = ? ORDER BY financial_year DESC";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTaxRecord(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching tax records for employee " + employeeId, e);
        }
        return list;
    }

    public boolean update(TaxRecord rec) {
        String sql = "UPDATE tax_records SET taxable_income = ?, tax_paid = ?, deductions_declared = ?, tax_saving_suggestions = ? WHERE id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, rec.getTaxableIncome());
            stmt.setDouble(2, rec.getTaxPaid());
            stmt.setDouble(3, rec.getDeductionsDeclared());
            stmt.setString(4, rec.getTaxSavingSuggestions());
            stmt.setInt(5, rec.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating tax record ID " + rec.getId(), e);
        }
        return false;
    }

    private TaxRecord mapResultSetToTaxRecord(ResultSet rs) throws SQLException {
        TaxRecord rec = new TaxRecord();
        rec.setId(rs.getInt("id"));
        rec.setEmployeeId(rs.getInt("employee_id"));
        rec.setFinancialYear(rs.getString("financial_year"));
        rec.setTaxableIncome(rs.getDouble("taxable_income"));
        rec.setTaxPaid(rs.getDouble("tax_paid"));
        rec.setDeductionsDeclared(rs.getDouble("deductions_declared"));
        rec.setTaxSavingSuggestions(rs.getString("tax_saving_suggestions"));
        return rec;
    }
}
