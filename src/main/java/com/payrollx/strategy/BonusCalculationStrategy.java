package com.payrollx.strategy;

import com.payrollx.model.Employee;

/**
 * Strategy interface for Bonus Calculation.
 */
public interface BonusCalculationStrategy {
    /**
     * Calculates the bonus amount for an employee.
     */
    double calculateBonus(Employee employee, double performanceRating);
    String getStrategyName();
}
