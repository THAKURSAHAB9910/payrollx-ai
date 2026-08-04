package com.payrollx.factory;

import com.payrollx.model.Employee;
import java.time.LocalDate;

/**
 * Factory Design Pattern to create Employee templates based on employment types.
 */
public class EmployeeFactory {

    public enum EmploymentType {
        FULL_TIME,
        CONTRACTOR,
        INTERN
    }

    /**
     * Creates a pre-configured Employee record.
     */
    public static Employee createEmployee(EmploymentType type, String firstName, String lastName, String email, 
                                          String phone, String bankAccount, double baseSalary, Integer departmentId, Integer managerId) {
        Employee.Builder builder = new Employee.Builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .bankAccount(bankAccount)
                .hireDate(LocalDate.now())
                .departmentId(departmentId)
                .managerId(managerId)
                .status("ACTIVE");

        switch (type) {
            case FULL_TIME:
                return builder
                        .position("Full-Time Engineer")
                        .salary(baseSalary)
                        .build();
            case CONTRACTOR:
                return builder
                        .position("Contractor Consultant")
                        .salary(baseSalary)
                        .build();
            case INTERN:
                return builder
                        .position("Intern Associate")
                        .salary(baseSalary)
                        .build();
            default:
                throw new IllegalArgumentException("Unknown employment type: " + type);
        }
    }
}
