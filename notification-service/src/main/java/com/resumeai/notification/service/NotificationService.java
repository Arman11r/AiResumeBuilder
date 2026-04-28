package com.resumeai.notification.service;

import com.resumeai.notification.dto.*;
import java.util.List;

public interface NotificationService {
    NotificationResponse send(SendNotificationRequest request);
    void sendBulk(BroadcastRequest request);
    NotificationResponse markAsRead(String notificationId);
    void markAllRead(String recipientId);
    List<NotificationResponse> getByRecipient(String recipientId);
    long getUnreadCount(String recipientId);
    void deleteNotification(String notificationId);
}