package com.payrollx.template;

import com.payrollx.model.Payroll;
import java.util.List;

/**
 * Concrete implementation of ReportGenerator for payroll summaries.
 */
public class PayrollReportGenerator extends ReportGenerator<Payroll> {

    @Override
    protected String generateHeader(String title) {
        return "========================================================================\n" +
               "                     " + title.toUpperCase() + "\n" +
               "========================================================================\n" +
               String.format("%-10s | %-7s | %-12s | %-12s | %-12s | %-12s\n", 
                       "Emp ID", "Month", "Basic Sal", "Allowances", "Deductions", "Net Salary") +
               "------------------------------------------------------------------------";
    }

    @Override
    protected String generateBody(List<Payroll> data) {
        StringBuilder sb = new StringBuilder();
        for (Payroll p : data) {
            double allowances = p.getHra() + p.getDa() + p.getMedicalAllowance() + p.getTravelAllowance() + p.getOvertimePay() + p.getBonus();
            double deductions = p.getPf() + p.getTax() + p.getInsurance() + p.getOtherDeductions();
            
            sb.append(String.format("%-10d | %-7s | $%-10.2f | $%-10.2f | $%-10.2f | $%-10.2f\n",
                    p.getEmployeeId(),
                    p.getPayrollMonth(),
                    p.getBasicSalary(),
                    allowances,
                    deductions,
                    p.getNetSalary()));
        }
        return sb.toString();
    }
}
