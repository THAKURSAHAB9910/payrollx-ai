package com.payrollx.model;

public class TaxRecord {
    private int id;
    private int employeeId;
    private String financialYear; // YYYY-YYYY
    private double taxableIncome;
    private double taxPaid;
    private double deductionsDeclared;
    private String taxSavingSuggestions;

    public TaxRecord() {}

    public TaxRecord(int id, int employeeId, String financialYear, double taxableIncome, double taxPaid, double deductionsDeclared, String taxSavingSuggestions) {
        this.id = id;
        this.employeeId = employeeId;
        this.financialYear = financialYear;
        this.taxableIncome = taxableIncome;
        this.taxPaid = taxPaid;
        this.deductionsDeclared = deductionsDeclared;
        this.taxSavingSuggestions = taxSavingSuggestions;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getFinancialYear() { return financialYear; }
    public void setFinancialYear(String financialYear) { this.financialYear = financialYear; }

    public double getTaxableIncome() { return taxableIncome; }
    public void setTaxableIncome(double taxableIncome) { this.taxableIncome = taxableIncome; }

    public double getTaxPaid() { return taxPaid; }
    public void setTaxPaid(double taxPaid) { this.taxPaid = taxPaid; }

    public double getDeductionsDeclared() { return deductionsDeclared; }
    public void setDeductionsDeclared(double deductionsDeclared) { this.deductionsDeclared = deductionsDeclared; }

    public String getTaxSavingSuggestions() { return taxSavingSuggestions; }
    public void setTaxSavingSuggestions(String taxSavingSuggestions) { this.taxSavingSuggestions = taxSavingSuggestions; }
}
