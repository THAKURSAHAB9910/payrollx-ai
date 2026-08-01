package com.payrollx.strategy;

/**
 * Old Tax Regime Calculation. Allows standard tax deductions up to a limit.
 */
public class OldRegimeTaxStrategy implements TaxCalculationStrategy {

    @Override
    public double calculateTax(double annualIncome, double deductions) {
        // Taxable income = Annual Income - Deductions (capped at say 150000 for standard Section 80C)
        double cappedDeductions = Math.min(deductions, 150000.0);
        double taxableIncome = Math.max(0.0, annualIncome - cappedDeductions);
        
        double tax = 0.0;
        if (taxableIncome <= 250000) {
            tax = 0.0;
        } else if (taxableIncome <= 500000) {
            tax = (taxableIncome - 250000) * 0.05;
        } else if (taxableIncome <= 1000000) {
            tax = (250000 * 0.05) + (taxableIncome - 500000) * 0.20;
        } else {
            tax = (250000 * 0.05) + (500000 * 0.20) + (taxableIncome - 1000000) * 0.30;
        }
        
        // Add 4% cess
        return tax * 1.04;
    }

    @Override
    public String getStrategyName() {
        return "OLD_REGIME";
    }
}
