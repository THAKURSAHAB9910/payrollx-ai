package com.payrollx.command;

import com.payrollx.dao.EmployeeDao;
import com.payrollx.dao.AuditLogDao;
import com.payrollx.model.Employee;
import com.payrollx.model.AuditLog;

import java.time.LocalDateTime;

/**
 * Concrete Command to promote an employee (updates position and salary). Supports undo.
 */
public class PromoteEmployeeCommand implements Command {
    private final int employeeId;
    private final String newPosition;
    private final double newSalary;
    private final Integer adminUserId;

    private String oldPosition;
    private double oldSalary;
    
    private final EmployeeDao employeeDao;
    private final AuditLogDao auditLogDao;

    public PromoteEmployeeCommand(int employeeId, String newPosition, double newSalary, Integer adminUserId) {
        this.employeeId = employeeId;
        this.newPosition = newPosition;
        this.newSalary = newSalary;
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
        this.oldPosition = emp.getPosition();
        this.oldSalary = emp.getSalary();

        emp.setPosition(newPosition);
        emp.setSalary(newSalary);

        boolean updated = employeeDao.update(emp);
        if (updated) {
            AuditLog log = new AuditLog();
            log.setUserId(adminUserId);
            log.setAction("PROMOTE_EMPLOYEE");
            log.setDetails("Promoted Employee ID " + employeeId + " to " + newPosition + " with salary " + newSalary + " (from " + oldPosition + ", " + oldSalary + ")");
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
        emp.setPosition(oldPosition);
        emp.setSalary(oldSalary);

        boolean updated = employeeDao.update(emp);
        if (updated) {
            AuditLog log = new AuditLog();
            log.setUserId(adminUserId);
            log.setAction("UNDO_PROMOTE_EMPLOYEE");
            log.setDetails("Reverted promotion of Employee ID " + employeeId + " back to " + oldPosition + " and salary " + oldSalary);
            log.setTimestamp(LocalDateTime.now());
            auditLogDao.create(log);
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Promote Employee ID " + employeeId + " to " + newPosition + " (Salary: " + newSalary + ")";
    }
}
