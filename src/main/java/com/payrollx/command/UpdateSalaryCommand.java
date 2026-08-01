package com.payrollx.command;

import com.payrollx.dao.EmployeeDao;
import com.payrollx.dao.AuditLogDao;
import com.payrollx.model.Employee;
import com.payrollx.model.AuditLog;

import java.time.LocalDateTime;

/**
 * Concrete Command to update an employee's salary. Supports undo operation.
 */
public class UpdateSalaryCommand implements Command {
    private final int employeeId;
    private final double newSalary;
    private final String reason;
    private final Integer adminUserId;
    
    private double oldSalary;
    private final EmployeeDao employeeDao;
    private final AuditLogDao auditLogDao;

    public UpdateSalaryCommand(int employeeId, double newSalary, String reason, Integer adminUserId) {
        this.employeeId = employeeId;
        this.newSalary = newSalary;
        this.reason = reason;
        this.adminUserId = adminUserId;
        this.employeeDao = new EmployeeDao();
        this.auditLogDao = new AuditLogDao();
    }

    @Override
    public boolean execute() {
        Employee emp = employeeDao.getById(employeeId);
        if (emp == null) {
            return false;
        }
        this.oldSalary = emp.getSalary();
        emp.setSalary(newSalary);
        
        boolean updated = employeeDao.update(emp);
        if (updated) {
            // Write to Audit Logs
            AuditLog log = new AuditLog();
            log.setUserId(adminUserId);
            log.setAction("UPDATE_SALARY");
            log.setDetails("Salary of Employee ID " + employeeId + " updated from " + oldSalary + " to " + newSalary + ". Reason: " + reason);
            log.setTimestamp(LocalDateTime.now());
            auditLogDao.create(log);
            return true;
        }
        return false;
    }

    @Override
    public boolean undo() {
        Employee emp = employeeDao.getById(employeeId);
        if (emp == null) {
            return false;
        }
        emp.setSalary(oldSalary);
        boolean updated = employeeDao.update(emp);
        if (updated) {
            AuditLog log = new AuditLog();
            log.setUserId(adminUserId);
            log.setAction("UNDO_UPDATE_SALARY");
            log.setDetails("Reverted salary of Employee ID " + employeeId + " back to " + oldSalary + " from " + newSalary);
            log.setTimestamp(LocalDateTime.now());
            auditLogDao.create(log);
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Update Salary for Employee ID " + employeeId + " to " + newSalary + " (Original: " + oldSalary + ")";
    }
}
