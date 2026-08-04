package com.payrollx.strategy;

import com.payrollx.model.Employee;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Tenure-based Bonus Strategy.
 * Bonus is calculated based on employee's years of service.
 */
public class TenureBonusStrategy implements BonusCalculationStrategy {

    @Override
    public double calculateBonus(Employee employee, double performanceRating) {
        // Calculate years of service
        long years = ChronoUnit.YEARS.between(employee.getHireDate(), LocalDate.now());
        
        // 2% of monthly salary for every year of service, capped at 30%
        double percentage = Math.min(years * 0.02, 0.30);
        return employee.getSalary() * percentage;
    }

    @Override
    public String getStrategyName() {
        return "TENURE";
    }
}
