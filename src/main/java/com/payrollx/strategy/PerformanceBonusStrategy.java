package com.payrollx.strategy;

import com.payrollx.model.Employee;

/**
 * Performance-based Bonus Strategy.
 * Bonus is proportional to performance rating.
 */
public class PerformanceBonusStrategy implements BonusCalculationStrategy {

    @Override
    public double calculateBonus(Employee employee, double performanceRating) {
        // Rating ranges from 1.0 to 5.0. 
        // 5.0 gets 20% of monthly salary, 4.0 gets 10%, 3.0 gets 5%, < 3 gets 0%.
        if (performanceRating >= 4.8) {
            return employee.getSalary() * 0.20;
        } else if (performanceRating >= 4.0) {
            return employee.getSalary() * 0.12;
        } else if (performanceRating >= 3.0) {
            return employee.getSalary() * 0.05;
        }
        return 0.0;
    }

    @Override
    public String getStrategyName() {
        return "PERFORMANCE";
    }
}
