package com.payrollx.model;

public class Project {
    private int id;
    private String projectName;
    private double budget;
    private String status; // IN_PROGRESS, COMPLETED, SUSPENDED

    public Project() {}

    public Project(int id, String projectName, double budget, String status) {
        this.id = id;
        this.projectName = projectName;
        this.budget = budget;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
