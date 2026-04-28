package com.resumeai.notification.repository;

import com.resumeai.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientIdOrderBySentAtDesc(String recipientId);
    List<Notification> findByRecipientIdAndIsReadFalseOrderBySentAtDesc(String recipientId);
    long countByRecipientIdAndIsReadFalse(String recipientId);
    List<Notification> findByRelatedId(String relatedId);
}