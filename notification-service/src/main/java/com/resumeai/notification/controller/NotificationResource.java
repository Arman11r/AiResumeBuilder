package com.resumeai.notification.controller;

import com.resumeai.notification.dto.*;
import com.resumeai.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationResource {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> send(
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@Valid @RequestBody BroadcastRequest request) {
        notificationService.sendBulk(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{recipientId}")
    public ResponseEntity<List<NotificationResponse>> getByRecipient(
            @PathVariable String recipientId) {
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    @GetMapping("/{recipientId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable String recipientId) {
        long count = notificationService.getUnreadCount(recipientId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    public ResponseEntity<Void> markAllRead(@PathVariable String recipientId) {
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete(@PathVariable String notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }
}