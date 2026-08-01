package com.payrollx.decorator;

/**
 * Concrete Component representing the Base Salary.
 */
public class BaseSalary implements SalaryComponent {
    private final double amount;
    private final String description;

    public BaseSalary(double amount) {
        this.amount = amount;
        this.description = "Base Salary";
    }

    @Override
    public double getAmount() {
        return amount;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
