package com.payrollx.service;

import com.payrollx.dao.DepartmentDao;
import com.payrollx.dao.EmployeeDao;
import com.payrollx.dao.PayrollDao;
import com.payrollx.model.Department;
import com.payrollx.model.Employee;
import com.payrollx.model.Payroll;

import java.util.*;

/**
 * Service to aggregate business intelligence, generate summary statistics, and ASCII dashboards.
 */
public class AnalyticsService {
    private final EmployeeDao employeeDao = new EmployeeDao();
    private final DepartmentDao departmentDao = new DepartmentDao();
    private final PayrollDao payrollDao = new PayrollDao();

    /**
     * Generates a text-based ASCII Dashboard containing metrics, distributions, and department costs.
     */
    public String generateDashboardReport() {
        List<Employee> employees = employeeDao.getAll();
        List<Department> departments = departmentDao.getAll();
        List<Payroll> payrollList = payrollDao.getAll();

        int totalEmployees = employees.size();
        long activeCount = employees.stream().filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus())).count();
        double totalSalaryBudget = employees.stream().mapToDouble(Employee::getSalary).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================\n");
        sb.append("                       ENTERPRISE PAYROLLX DASHBOARD                    \n");
        sb.append("========================================================================\n");
        sb.append(String.format(" Total Employees: %-10d | Active Employees: %-10d\n", totalEmployees, activeCount));
        sb.append(String.format(" Monthly Base Salary Liability: $%,.2f\n", totalSalaryBudget));
        sb.append("------------------------------------------------------------------------\n\n");

        // 1. Department Breakdown (Budget vs Actual Spent)
        sb.append("DEPARTMENT-WISE COST ANALYSIS:\n");
        sb.append(String.format(" %-20s | %-15s | %-15s | %-10s\n", "Department", "Budget", "Monthly Cost", "Utilization"));
        sb.append(" ----------------------------------------------------------------------\n");

        for (Department dept : departments) {
            List<Employee> deptEmps = employeeDao.getByDepartmentId(dept.getId());
            double deptCost = deptEmps.stream().mapToDouble(Employee::getSalary).sum();
            double utilization = dept.getBudget() > 0 ? (deptCost / dept.getBudget()) * 100 : 0.0;
            
            sb.append(String.format(" %-20s | $%-14.2f | $%-14.2f | %-8.1f%%\n",
                    dept.getDeptName(),
                    dept.getBudget(),
                    deptCost,
                    utilization));
        }
        sb.append("------------------------------------------------------------------------\n\n");

        // 2. Salary Distribution (ASCII Bar Chart Histogram)
        sb.append("SALARY DISTRIBUTION HISTOGRAM:\n");
        if (employees.isEmpty()) {
            sb.append(" No employee records available to show salary distribution.\n");
        } else {
            double maxSalary = employees.stream().mapToDouble(Employee::getSalary).max().orElse(0.0);
            double minSalary = employees.stream().mapToDouble(Employee::getSalary).min().orElse(0.0);
            
            // Divide range into 4 buckets
            double bucketSize = (maxSalary - minSalary) / 4;
            if (bucketSize <= 0) bucketSize = 1000;
            
            int[] buckets = new int[4];
            for (Employee emp : employees) {
                double sal = emp.getSalary();
                int bIdx = (int) ((sal - minSalary) / bucketSize);
                if (bIdx >= 4) bIdx = 3;
                if (bIdx < 0) bIdx = 0;
                buckets[bIdx]++;
            }

            for (int i = 0; i < 4; i++) {
                double rangeStart = minSalary + i * bucketSize;
                double rangeEnd = rangeStart + bucketSize;
                String stars = "*".repeat(buckets[i]);
                sb.append(String.format("  $%-8.0f - $%-8.0f : %s (%d)\n", rangeStart, rangeEnd, stars, buckets[i]));
            }
        }
        sb.append("------------------------------------------------------------------------\n\n");

        // 3. Historical Payouts
        sb.append("RECENT MONTHLY PAYOUT HISTORY:\n");
        Map<String, Double> monthlyTotals = new TreeMap<>(Collections.reverseOrder());
        for (Payroll p : payrollList) {
            monthlyTotals.put(p.getPayrollMonth(), monthlyTotals.getOrDefault(p.getPayrollMonth(), 0.0) + p.getNetSalary());
        }

        if (monthlyTotals.isEmpty()) {
            sb.append("  No payroll data processed yet.\n");
        } else {
            int cnt = 0;
            for (Map.Entry<String, Double> entry : monthlyTotals.entrySet()) {
                if (cnt >= 5) break; // show recent 5 months
                sb.append(String.format("  Month: %s | Total Payout: $%,.2f\n", entry.getKey(), entry.getValue()));
                cnt++;
            }
        }
        sb.append("========================================================================\n");
        
        return sb.toString();
    }
}
