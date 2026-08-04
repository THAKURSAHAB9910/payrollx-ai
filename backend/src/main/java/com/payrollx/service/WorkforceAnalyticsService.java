package com.payrollx.service;

import com.payrollx.dao.EmployeeDao;
import com.payrollx.dao.PayrollDao;
import com.payrollx.dao.PerformanceDao;
import com.payrollx.dao.AttendanceDao;
import com.payrollx.model.Employee;
import com.payrollx.model.Payroll;
import com.payrollx.model.Performance;
import com.payrollx.model.Attendance;
import com.payrollx.strategy.NewRegimeTaxStrategy;
import com.payrollx.strategy.OldRegimeTaxStrategy;
import com.payrollx.strategy.TaxCalculationStrategy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service handling salary prediction, promotion recommendation, and salary simulation.
 */
public class WorkforceAnalyticsService {
    private static final Logger LOGGER = Logger.getLogger(WorkforceAnalyticsService.class.getName());
    
    private final EmployeeDao employeeDao = new EmployeeDao();
    private final PayrollDao payrollDao = new PayrollDao();
    private final PerformanceDao performanceDao = new PerformanceDao();
    private final AttendanceDao attendanceDao = new AttendanceDao();

    /**
     * Predicts next month's payroll budget using simple linear regression on historical payroll costs.
     */
    public double predictNextMonthPayrollCost() {
        List<Payroll> allPayroll = payrollDao.getAll();
        if (allPayroll.isEmpty()) {
            return 0.0;
        }

        // Group payroll records by month and sum the net salary
        Map<String, Double> monthlyCosts = new TreeMap<>(); // sorted by month chronologically
        for (Payroll p : allPayroll) {
            monthlyCosts.put(p.getPayrollMonth(), monthlyCosts.getOrDefault(p.getPayrollMonth(), 0.0) + p.getNetSalary());
        }

        if (monthlyCosts.size() < 2) {
            // Not enough data for regression, return the average or the last month's cost
            double sum = 0.0;
            for (double cost : monthlyCosts.values()) {
                sum += cost;
            }
            return monthlyCosts.isEmpty() ? 0.0 : sum / monthlyCosts.size();
        }

        // Fit a linear regression line: y = mx + c
        int n = monthlyCosts.size();
        double[] x = new double[n];
        double[] y = new double[n];
        int idx = 0;
        for (Map.Entry<String, Double> entry : monthlyCosts.entrySet()) {
            x[idx] = idx + 1; // Month index: 1, 2, 3...
            y[idx] = entry.getValue();
            idx++;
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumXX += x[i] * x[i];
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // Predict for month n + 1
        double prediction = slope * (n + 1) + intercept;
        return Math.max(0.0, prediction);
    }

    /**
     * Holds recommendation details.
     */
    public static class PromotionCandidate {
        public Employee employee;
        public double score;
        public String reasons;

        public PromotionCandidate(Employee employee, double score, String reasons) {
            this.employee = employee;
            this.score = score;
            this.reasons = reasons;
        }
    }

    /**
     * Ranks and recommends employees for promotion using a PriorityQueue.
     * Score combines rating (50%), attendance (20%), and tenure (30%).
     */
    public List<PromotionCandidate> getPromotionRecommendations() {
        List<Employee> employees = employeeDao.getAll();
        // PriorityQueue to sort candidates in descending order of score
        PriorityQueue<PromotionCandidate> pq = new PriorityQueue<>((a, b) -> Double.compare(b.score, a.score));

        for (Employee emp : employees) {
            if (!"ACTIVE".equalsIgnoreCase(emp.getStatus())) {
                continue;
            }

            // 1. Performance Rating (Max 5.0)
            Performance perf = performanceDao.getRecentPerformance(emp.getId());
            double rating = (perf != null) ? perf.getRating() : 3.0; // Default to average 3.0

            // 2. Attendance rate (present days / total tracked days)
            List<Attendance> attendanceList = attendanceDao.getByEmployeeId(emp.getId());
            double attendanceRate = 1.0;
            if (!attendanceList.isEmpty()) {
                long presentCount = attendanceList.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus())).count();
                attendanceRate = (double) presentCount / attendanceList.size();
            }

            // 3. Tenure in years
            long years = ChronoUnit.YEARS.between(emp.getHireDate(), LocalDate.now());

            // Score calculation
            double score = (rating / 5.0) * 50.0 + (attendanceRate * 20.0) + (Math.min(years, 10.0) / 10.0) * 30.0;
            
            StringBuilder reasons = new StringBuilder();
            reasons.append(String.format("Performance Rating: %.1f/5.0. ", rating));
            reasons.append(String.format("Attendance: %.1f%%. ", attendanceRate * 100));
            reasons.append(String.format("Tenure: %d years. ", years));

            pq.add(new PromotionCandidate(emp, score, reasons.toString()));
        }

        List<PromotionCandidate> recommendations = new ArrayList<>();
        while (!pq.isEmpty()) {
            recommendations.add(pq.poll());
        }
        return recommendations;
    }

    /**
     * Simulates net salary for an employee.
     */
    public double simulateSalary(Employee emp, double newBaseSalary, double additionalAllowances, String taxRegime) {
        TaxCalculationStrategy taxStrategy = "NEW_REGIME".equalsIgnoreCase(taxRegime) 
                ? new NewRegimeTaxStrategy() 
                : new OldRegimeTaxStrategy();

        // Standard HRA: 40% of base salary
        double hra = newBaseSalary * 0.40;
        // Standard DA: 10% of base salary
        double da = newBaseSalary * 0.10;
        double medical = 2000.0;
        double travel = 1600.0;

        double grossSalary = newBaseSalary + hra + da + medical + travel + additionalAllowances;
        
        // Deductions
        double pf = newBaseSalary * 0.12; // 12% PF deduction
        double tax = taxStrategy.calculateTax(grossSalary * 12, pf * 12) / 12; // monthly tax
        double insurance = 1000.0;

        double totalDeductions = pf + tax + insurance;
        return Math.max(0.0, grossSalary - totalDeductions);
    }
}
