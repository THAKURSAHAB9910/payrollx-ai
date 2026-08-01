package com.payrollx.observer;

import com.payrollx.dao.NotificationDao;
import com.payrollx.model.Notification;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Concrete Observer that simulates dispatching SMS alerts.
 */
public class SmsNotificationListener implements Observer {
    private static final Logger LOGGER = Logger.getLogger(SmsNotificationListener.class.getName());
    private final NotificationDao notificationDao = new NotificationDao();

    @Override
    public void onNotificationReceived(String type, String message, int employeeId) {
        if ("SMS".equalsIgnoreCase(type) || "ALL".equalsIgnoreCase(type)) {
            LOGGER.info("[SMS Service] SIMULATED DISPATCH to Employee ID " + employeeId + " -> Message: " + message);
            
            // Save to DB
            Notification notif = new Notification();
            notif.setEmployeeId(employeeId);
            notif.setNotificationType("SMS");
            notif.setMessage(message);
            notif.setRead(false);
            notif.setCreatedAt(LocalDateTime.now());
            notificationDao.create(notif);
        }
    }
}
