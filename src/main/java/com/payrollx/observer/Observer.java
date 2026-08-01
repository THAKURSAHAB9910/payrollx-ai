package com.payrollx.observer;

/**
 * Observer interface for the Observer Design Pattern.
 * Receives updates from the NotificationSystem.
 */
public interface Observer {
    void onNotificationReceived(String type, String message, int employeeId);
}
