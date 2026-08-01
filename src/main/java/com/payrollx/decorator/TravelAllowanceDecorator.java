package com.payrollx.decorator;

/**
 * Concrete Decorator adding Travel Allowance.
 */
public class TravelAllowanceDecorator extends SalaryDecorator {
    private final double travelAllowance;

    public TravelAllowanceDecorator(SalaryComponent tempSalaryComponent, double travelAllowance) {
        super(tempSalaryComponent);
        this.travelAllowance = travelAllowance;
    }

    @Override
    public double getAmount() {
        return tempSalaryComponent.getAmount() + travelAllowance;
    }

    @Override
    public String getDescription() {
        return tempSalaryComponent.getDescription() + " + Travel Allowance";
    }
}
