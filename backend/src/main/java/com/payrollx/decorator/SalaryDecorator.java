package com.payrollx.decorator;

/**
 * Abstract Decorator class for SalaryComponent.
 */
public abstract class SalaryDecorator implements SalaryComponent {
    protected final SalaryComponent tempSalaryComponent;

    public SalaryDecorator(SalaryComponent tempSalaryComponent) {
        this.tempSalaryComponent = tempSalaryComponent;
    }

    @Override
    public double getAmount() {
        return tempSalaryComponent.getAmount();
    }

    @Override
    public String getDescription() {
        return tempSalaryComponent.getDescription();
    }
}
