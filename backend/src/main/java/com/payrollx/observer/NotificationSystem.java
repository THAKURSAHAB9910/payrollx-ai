package com.payrollx.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * Subject class in the Observer Pattern.
 * Manages observers and triggers notifications.
 */
public class NotificationSystem {
    private static NotificationSystem instance;
    private final List<Observer> observers = new ArrayList<>();

    private NotificationSystem() {}

    public static synchronized NotificationSystem getInstance() {
        if (instance == null) {
            instance = new NotificationSystem();
        }
        return instance;
    }

    public synchronized void attach(Observer observer) {
        observers.add(observer);
    }

    public synchronized void detach(Observer observer) {
        observers.remove(observer);
    }

    public synchronized void notify(String type, String message, int employeeId) {
        for (Observer observer : observers) {
            observer.onNotificationReceived(type, message, employeeId);
        }
    }
}
