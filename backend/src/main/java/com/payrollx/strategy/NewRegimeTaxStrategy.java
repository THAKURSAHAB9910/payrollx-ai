package com.payrollx.strategy;

/**
 * New Tax Regime Calculation. Standard tax slabs, lower rates, no deductions allowed.
 */
public class NewRegimeTaxStrategy implements TaxCalculationStrategy {

    @Override
    public double calculateTax(double annualIncome, double deductions) {
        // No deductions allowed under New Regime (except standard deduction of 50000, let's include it)
        double taxableIncome = Math.max(0.0, annualIncome - 50000.0);
        
        double tax = 0.0;
        if (taxableIncome <= 300000) {
            tax = 0.0;
        } else if (taxableIncome <= 600000) {
            tax = (taxableIncome - 300000) * 0.05;
        } else if (taxableIncome <= 900000) {
            tax = (300000 * 0.05) + (taxableIncome - 600000) * 0.10;
        } else if (taxableIncome <= 1200000) {
            tax = (300000 * 0.05) + (300000 * 0.10) + (taxableIncome - 900000) * 0.15;
        } else if (taxableIncome <= 1500000) {
            tax = (300000 * 0.05) + (300000 * 0.10) + (300000 * 0.15) + (taxableIncome - 1200000) * 0.20;
        } else {
            tax = (300000 * 0.05) + (300000 * 0.10) + (300000 * 0.15) + (300000 * 0.20) + (taxableIncome - 1500000) * 0.30;
        }
        
        // Add 4% cess
        return tax * 1.04;
    }

    @Override
    public String getStrategyName() {
        return "NEW_REGIME";
    }
}
