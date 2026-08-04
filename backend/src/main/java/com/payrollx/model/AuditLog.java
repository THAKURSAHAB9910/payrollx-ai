package com.payrollx.model;

import java.time.LocalDateTime;

public class AuditLog {
    private int id;
    private Integer userId;
    private String action;
    private String details;
    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(int id, Integer userId, String action, String details, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
