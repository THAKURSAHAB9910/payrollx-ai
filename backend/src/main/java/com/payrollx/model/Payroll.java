package com.payrollx.model;

import java.time.LocalDate;

public class Payroll {
    private int id;
    private int employeeId;
    private String payrollMonth; // Format: YYYY-MM
    private double basicSalary;
    private double hra;
    private double da;
    private double bonus;
    private double medicalAllowance;
    private double travelAllowance;
    private double overtimePay;
    private double pf;
    private double tax;
    private double insurance;
    private double otherDeductions;
    private double netSalary;
    private LocalDate processedDate;
    private String status; // DRAFT, PROCESSED, PAID

    public Payroll() {}

    public Payroll(int id, int employeeId, String payrollMonth, double basicSalary, double hra, double da, double bonus,
                   double medicalAllowance, double travelAllowance, double overtimePay, double pf, double tax,
                   double insurance, double otherDeductions, double netSalary, LocalDate processedDate, String status) {
        this.id = id;
        this.employeeId = employeeId;
        this.payrollMonth = payrollMonth;
        this.basicSalary = basicSalary;
        this.hra = hra;
        this.da = da;
        this.bonus = bonus;
        this.medicalAllowance = medicalAllowance;
        this.travelAllowance = travelAllowance;
        this.overtimePay = overtimePay;
        this.pf = pf;
        this.tax = tax;
        this.insurance = insurance;
        this.otherDeductions = otherDeductions;
        this.netSalary = netSalary;
        this.processedDate = processedDate;
        this.status = status;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getPayrollMonth() { return payrollMonth; }
    public void setPayrollMonth(String payrollMonth) { this.payrollMonth = payrollMonth; }

    public double getBasicSalary() { return basicSalary; }
    public void setBasicSalary(double basicSalary) { this.basicSalary = basicSalary; }

    public double getHra() { return hra; }
    public void setHra(double hra) { this.hra = hra; }

    public double getDa() { return da; }
    public void setDa(double da) { this.da = da; }

    public double getBonus() { return bonus; }
    public void setBonus(double bonus) { this.bonus = bonus; }

    public double getMedicalAllowance() { return medicalAllowance; }
    public void setMedicalAllowance(double medicalAllowance) { this.medicalAllowance = medicalAllowance; }

    public double getTravelAllowance() { return travelAllowance; }
    public void setTravelAllowance(double travelAllowance) { this.travelAllowance = travelAllowance; }

    public double getOvertimePay() { return overtimePay; }
    public void setOvertimePay(double overtimePay) { this.overtimePay = overtimePay; }

    public double getPf() { return pf; }
    public void setPf(double pf) { this.pf = pf; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getInsurance() { return insurance; }
    public void setInsurance(double insurance) { this.insurance = insurance; }

    public double getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(double otherDeductions) { this.otherDeductions = otherDeductions; }

    public double getNetSalary() { return netSalary; }
    public void setNetSalary(double netSalary) { this.netSalary = netSalary; }

    public LocalDate getProcessedDate() { return processedDate; }
    public void setProcessedDate(LocalDate processedDate) { this.processedDate = processedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
