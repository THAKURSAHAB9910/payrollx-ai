package com.payrollx.decorator;

/**
 * Component interface for the Decorator Design Pattern.
 * Used for building custom salary structures with dynamic allowances.
 */
public interface SalaryComponent {
    double getAmount();
    String getDescription();
}
