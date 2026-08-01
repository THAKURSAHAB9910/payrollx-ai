package com.payrollx.model;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private int employeeId;
    private String notificationType; // EMAIL, SMS, IN_APP
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(int id, int employeeId, String notificationType, String message, boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.notificationType = notificationType;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
