package com.payrollx.observer;

import com.payrollx.dao.NotificationDao;
import com.payrollx.model.Notification;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Concrete Observer that simulates creating In-App alerts.
 */
public class InAppNotificationListener implements Observer {
    private static final Logger LOGGER = Logger.getLogger(InAppNotificationListener.class.getName());
    private final NotificationDao notificationDao = new NotificationDao();

    @Override
    public void onNotificationReceived(String type, String message, int employeeId) {
        if ("IN_APP".equalsIgnoreCase(type) || "ALL".equalsIgnoreCase(type)) {
            LOGGER.info("[In-App Service] SIMULATED DISPATCH to Employee ID " + employeeId + " -> Message: " + message);
            
            // Save to DB
            Notification notif = new Notification();
            notif.setEmployeeId(employeeId);
            notif.setNotificationType("IN_APP");
            notif.setMessage(message);
            notif.setRead(false);
            notif.setCreatedAt(LocalDateTime.now());
            notificationDao.create(notif);
        }
    }
}
