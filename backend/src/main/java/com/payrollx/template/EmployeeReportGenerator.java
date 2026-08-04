package com.payrollx.template;

import com.payrollx.model.Employee;
import java.util.List;

/**
 * Concrete implementation of ReportGenerator for employee rosters.
 */
public class EmployeeReportGenerator extends ReportGenerator<Employee> {

    @Override
    protected String generateHeader(String title) {
        return "========================================================================\n" +
               "                     " + title.toUpperCase() + "\n" +
               "========================================================================\n" +
               String.format("%-5s | %-20s | %-25s | %-15s | %-10s\n", "ID", "Name", "Email", "Position", "Salary") +
               "------------------------------------------------------------------------";
    }

    @Override
    protected String generateBody(List<Employee> data) {
        StringBuilder sb = new StringBuilder();
        for (Employee emp : data) {
            sb.append(String.format("%-5d | %-20s | %-25s | %-15s | $%-9.2f\n",
                    emp.getId(),
                    emp.getFullName(),
                    emp.getEmail(),
                    emp.getPosition(),
                    emp.getSalary()));
        }
        return sb.toString();
    }
}
