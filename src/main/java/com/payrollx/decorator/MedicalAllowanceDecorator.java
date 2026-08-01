package com.payrollx.decorator;

/**
 * Concrete Decorator adding Medical Allowance.
 */
public class MedicalAllowanceDecorator extends SalaryDecorator {
    private final double medicalAllowance;

    public MedicalAllowanceDecorator(SalaryComponent tempSalaryComponent, double medicalAllowance) {
        super(tempSalaryComponent);
        this.medicalAllowance = medicalAllowance;
    }

    @Override
    public double getAmount() {
        return tempSalaryComponent.getAmount() + medicalAllowance;
    }

    @Override
    public String getDescription() {
        return tempSalaryComponent.getDescription() + " + Medical Allowance";
    }
}
