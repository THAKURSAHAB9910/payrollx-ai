package com.payrollx.factory;

import com.payrollx.template.EmployeeReportGenerator;
import com.payrollx.template.PayrollReportGenerator;
import com.payrollx.template.ReportGenerator;

/**
 * Factory Design Pattern to instantiate concrete ReportGenerators.
 */
public class ReportFactory {

    public enum ReportType {
        EMPLOYEE,
        PAYROLL
    }

    /**
     * Instantiates the matching ReportGenerator.
     */
    @SuppressWarnings("rawtypes")
    public static ReportGenerator getReportGenerator(ReportType type) {
        switch (type) {
            case EMPLOYEE:
                return new EmployeeReportGenerator();
            case PAYROLL:
                return new PayrollReportGenerator();
            default:
                throw new IllegalArgumentException("Unknown report type: " + type);
        }
    }
}
