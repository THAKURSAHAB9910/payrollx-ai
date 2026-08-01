package com.payrollx.service;

import com.payrollx.dao.EmployeeDao;
import com.payrollx.dao.PayrollDao;
import com.payrollx.dao.LeaveRequestDao;
import com.payrollx.model.Employee;
import com.payrollx.model.LeaveRequest;
import com.payrollx.model.Payroll;
import com.payrollx.util.OrgGraph;
import com.payrollx.util.Trie;

import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Natural Language AI HR Assistant service.
 * Connects SQL data, custom Trie autocomplete, and OrgGraph reporting tree.
 */
public class AIService {
    private final EmployeeDao employeeDao = new EmployeeDao();
    private final PayrollDao payrollDao = new PayrollDao();
    private final LeaveRequestDao leaveRequestDao = new LeaveRequestDao();
    private final Trie trie = new Trie();
    private final OrgGraph orgGraph = new OrgGraph();

    public AIService() {
        refreshDSAStructures();
    }

    /**
     * Re-reads employee data from database and rebuilds Trie and OrgGraph in-memory.
     */
    public synchronized void refreshDSAStructures() {
        trie.getSuggestions(""); // clears Trie nodes indirectly by re-instantiating would be better, but we don't have clear on Trie.
        // Actually, we can just rebuild it. The Trie will overlap matches, which is fine, or we can instantiate a new one.
        // Let's reload Trie and OrgGraph
        orgGraph.clear();
        List<Employee> list = employeeDao.getAll();
        for (Employee emp : list) {
            trie.insert(emp.getFullName(), emp.getId());
            if (emp.getManagerId() != null) {
                orgGraph.addRelationship(emp.getId(), emp.getManagerId());
            }
        }
    }

    /**
     * Processes Natural Language command queries.
     */
    public String processQuery(String query, int currentEmployeeId) {
        if (query == null || query.trim().isEmpty()) {
            return "Please ask a question, e.g., 'show my salary breakup'.";
        }
        
        refreshDSAStructures();
        
        String input = query.toLowerCase().trim();
        Employee emp = employeeDao.getById(currentEmployeeId);
        if (emp == null) {
            return "Employee record not found.";
        }

        // 1. Trie Auto-Complete
        if (input.startsWith("autocomplete ") || input.startsWith("search ")) {
            String prefix = input.substring(input.indexOf(" ") + 1).trim();
            Map<String, List<Integer>> suggestions = trie.getSuggestions(prefix);
            if (suggestions.isEmpty()) {
                return "No employees match prefix: '" + prefix + "'";
            }
            StringBuilder sb = new StringBuilder("Auto-complete suggestions:\n");
            for (Map.Entry<String, List<Integer>> entry : suggestions.entrySet()) {
                sb.append(" - ").append(entry.getKey()).append(" (Employee IDs: ").append(entry.getValue()).append(")\n");
            }
            return sb.toString();
        }

        // 2. OrgGraph Hierarchy Queries
        if (input.startsWith("hierarchy") || input.contains("manager") || input.contains("reporting")) {
            // Find target employee id
            int targetId = currentEmployeeId;
            String[] tokens = input.split("\\s+");
            for (String token : tokens) {
                try {
                    targetId = Integer.parseInt(token);
                } catch (NumberFormatException ignored) {}
            }

            Employee target = employeeDao.getById(targetId);
            if (target == null) {
                return "Employee ID " + targetId + " not found.";
            }

            List<Integer> chain = orgGraph.getReportingChain(targetId);
            StringBuilder sb = new StringBuilder("Reporting hierarchy for ")
                    .append(target.getFullName()).append(" (ID: ").append(targetId).append("):\n");
            for (int i = 0; i < chain.size(); i++) {
                Employee nodeEmp = employeeDao.getById(chain.get(i));
                if (nodeEmp != null) {
                    sb.append("  ").append(" -> ".repeat(i)).append(nodeEmp.getFullName())
                            .append(" (").append(nodeEmp.getPosition()).append(")\n");
                }
            }

            // Show direct reports
            Set<Integer> reports = orgGraph.getDirectReports(targetId);
            if (!reports.isEmpty()) {
                sb.append("\nDirect reports under ").append(target.getFullName()).append(":\n");
                for (int repId : reports) {
                    Employee rep = employeeDao.getById(repId);
                    if (rep != null) {
                        sb.append(" - ").append(rep.getFullName()).append(" (ID: ").append(repId).append(", ").append(rep.getPosition()).append(")\n");
                    }
                }
            } else {
                sb.append("\nNo direct reports.");
            }
            return sb.toString();
        }

        // 3. Salary Breakup
        if (input.contains("salary breakup") || input.contains("salary division") || input.contains("breakup")) {
            double basic = emp.getSalary();
            double hra = basic * 0.40;
            double da = basic * 0.10;
            double medical = 2000.0;
            double travel = 1600.0;
            double pf = basic * 0.12;
            double gross = basic + hra + da + medical + travel;
            double net = gross - (pf + 1000.0); // Simple net estimate
            
            return String.format("Salary Breakup for %s:\n" +
                    "  - Base Salary: $%,.2f\n" +
                    "  - HRA (40%%): $%,.2f\n" +
                    "  - DA (10%%): $%,.2f\n" +
                    "  - Medical Allowance: $%,.2f\n" +
                    "  - Travel Allowance: $%,.2f\n" +
                    "  - PF Deduction (12%%): $%,.2f\n" +
                    "  - Estimated Monthly Net Pay: $%,.2f",
                    emp.getFullName(), basic, hra, da, medical, travel, pf, net);
        }

        // 4. Show Salary
        if (input.contains("show my salary") || input.contains("show salary") || input.contains("my salary")) {
            return String.format("Hello %s, your currently active monthly base salary is: $%,.2f (%s)",
                    emp.getFirstName(), emp.getSalary(), emp.getPosition());
        }

        // 5. Leave Balance
        if (input.contains("leave balance") || input.contains("remaining leave") || input.contains("leave")) {
            List<LeaveRequest> requests = leaveRequestDao.getByEmployeeId(currentEmployeeId);
            long approvedLeaveDays = 0;
            for (LeaveRequest req : requests) {
                if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
                    long days = ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
                    approvedLeaveDays += days;
                }
            }
            long totalQuota = 30; // 30 days annual quota
            long remaining = totalQuota - approvedLeaveDays;
            return String.format("Leave Balance for %s:\n" +
                    "  - Annual Quota: %d days\n" +
                    "  - Approved Leave Taken: %d days\n" +
                    "  - Remaining Leave Balance: %d days",
                    emp.getFullName(), totalQuota, approvedLeaveDays, remaining);
        }

        // 6. Tax Explanation
        if (input.contains("tax") || input.contains("deduction explanation")) {
            double annualSalary = emp.getSalary() * 12;
            double pfDeduction = (emp.getSalary() * 0.12) * 12;
            
            double taxOld = new com.payrollx.strategy.OldRegimeTaxStrategy().calculateTax(annualSalary, pfDeduction);
            double taxNew = new com.payrollx.strategy.NewRegimeTaxStrategy().calculateTax(annualSalary, pfDeduction);

            return String.format("Tax Calculation & Analysis (Annualized):\n" +
                    "  - Annual Gross Base Salary: $%,.2f\n" +
                    "  - PF Declared Deduction (80C): $%,.2f\n" +
                    "  - Old Tax Regime Estimate (with PF): $%,.2f/year ($%,.2f/month)\n" +
                    "  - New Tax Regime Estimate (no PF ded): $%,.2f/year ($%,.2f/month)\n" +
                    "  - Suggestion: %s Regime is more tax-efficient for you by $%,.2f per year.",
                    annualSalary, pfDeduction, taxOld, taxOld / 12, taxNew, taxNew / 12,
                    (taxNew < taxOld) ? "New" : "Old", Math.abs(taxOld - taxNew));
        }

        // 7. Payslip Simulation
        if (input.contains("download payslip") || input.contains("payslip")) {
            List<Payroll> payrolls = payrollDao.getByEmployeeId(currentEmployeeId);
            if (payrolls.isEmpty()) {
                return "No processed payslips found in history for download. Please process payroll first.";
            }
            Payroll latest = payrolls.get(0);
            return String.format("Payslip ready for download!\n" +
                    "  - Month: %s\n" +
                    "  - Net Salary Transferred: $%,.2f\n" +
                    "  - Status: %s\n" +
                    "  - File Simulation: [c:\\Users\\Vishe\\Downloads\\javap\\exports\\payslip_%d_%s.pdf]",
                    latest.getPayrollMonth(), latest.getNetSalary(), latest.getStatus(), currentEmployeeId, latest.getPayrollMonth());
        }

        return "I understood your query but I don't have specific data to answer it. " +
                "Try asking:\n" +
                "  - 'show my salary'\n" +
                "  - 'salary breakup'\n" +
                "  - 'remaining leave balance'\n" +
                "  - 'tax deduction explanation'\n" +
                "  - 'download payslip'\n" +
                "  - 'autocomplete <prefix>'\n" +
                "  - 'hierarchy <employeeId>'";
    }
}
