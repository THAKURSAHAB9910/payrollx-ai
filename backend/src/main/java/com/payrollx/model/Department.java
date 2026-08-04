package com.payrollx.model;

public class Department {
    private int id;
    private String deptName;
    private Integer managerId; // employee_id of manager
    private double budget;

    public Department() {}

    public Department(int id, String deptName, Integer managerId, double budget) {
        this.id = id;
        this.deptName = deptName;
        this.managerId = managerId;
        this.budget = budget;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }
}
