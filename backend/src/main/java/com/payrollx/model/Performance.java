package com.payrollx.model;

import java.time.LocalDate;

public class Performance {
    private int id;
    private int employeeId;
    private double rating; // 1.0 to 5.0
    private double kpiScore; // 0 to 100
    private String feedback;
    private LocalDate evaluationDate;

    public Performance() {}

    public Performance(int id, int employeeId, double rating, double kpiScore, String feedback, LocalDate evaluationDate) {
        this.id = id;
        this.employeeId = employeeId;
        this.rating = rating;
        this.kpiScore = kpiScore;
        this.feedback = feedback;
        this.evaluationDate = evaluationDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public double getKpiScore() { return kpiScore; }
    public void setKpiScore(double kpiScore) { this.kpiScore = kpiScore; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDate getEvaluationDate() { return evaluationDate; }
    public void setEvaluationDate(LocalDate evaluationDate) { this.evaluationDate = evaluationDate; }
}
