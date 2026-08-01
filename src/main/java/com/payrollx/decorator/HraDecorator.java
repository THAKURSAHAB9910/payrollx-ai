package com.payrollx.decorator;

/**
 * Concrete Decorator adding HRA (House Rent Allowance).
 */
public class HraDecorator extends SalaryDecorator {
    private final double baseSalaryVal;

    public HraDecorator(SalaryComponent tempSalaryComponent, double baseSalaryVal) {
        super(tempSalaryComponent);
        this.baseSalaryVal = baseSalaryVal;
    }

    @Override
    public double getAmount() {
        // HRA is 40% of base salary
        return tempSalaryComponent.getAmount() + (baseSalaryVal * 0.40);
    }

    @Override
    public String getDescription() {
        return tempSalaryComponent.getDescription() + " + HRA (40%)";
    }
}
