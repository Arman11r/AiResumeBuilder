package com.resumeai.notification.service.impl;

import com.resumeai.notification.dto.*;
import com.resumeai.notification.entity.Notification;
import com.resumeai.notification.repository.NotificationRepository;
import com.resumeai.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .type(parseType(request.getType()))
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(parseChannel(request.getChannel()))
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .isRead(false)
                .build();
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void sendBulk(BroadcastRequest request) {
        if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
            log.warn("Broadcast called with empty recipient list");
            return;
        }
        List<Notification> notifications = request.getRecipientIds().stream()
                .map(recipientId -> Notification.builder()
                        .recipientId(recipientId)
                        .type(parseType(request.getType()))
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .channel(Notification.Channel.APP)
                        .isRead(false)
                        .build())
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        Notification notification = findOrThrow(notificationId);
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllRead(String recipientId) {
        List<Notification> unread = notificationRepository
                .findByRecipientIdAndIsReadFalseOrderBySentAtDesc(recipientId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    public List<NotificationResponse> getByRecipient(String recipientId) {
        return notificationRepository
                .findByRecipientIdOrderBySentAtDesc(recipientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Override
    @Transactional
    public void deleteNotification(String notificationId) {
        Notification notification = findOrThrow(notificationId);
        notificationRepository.delete(notification);
    }

    private Notification findOrThrow(String notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found: " + notificationId));
    }

    private Notification.NotificationType parseType(String type) {
        try {
            return Notification.NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid notification type: " + type);
        }
    }

    private Notification.Channel parseChannel(String channel) {
        try {
            return Notification.Channel.valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Notification.Channel.APP;
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .recipientId(n.getRecipientId())
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .channel(n.getChannel().name())
                .relatedId(n.getRelatedId())
                .relatedType(n.getRelatedType())
                .isRead(n.isRead())
                .sentAt(n.getSentAt())
                .build();
    }
}