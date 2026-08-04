package com.payrollx.strategy;

/**
 * Strategy interface for Tax Calculation.
 */
public interface TaxCalculationStrategy {
    /**
     * Calculates tax on annual income after deductions.
     */
    double calculateTax(double annualIncome, double deductions);
    String getStrategyName();
}
