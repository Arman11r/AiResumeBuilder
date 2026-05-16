package com.resumeai.notification.service;

import com.resumeai.notification.dto.BroadcastRequest;
import com.resumeai.notification.dto.NotificationResponse;
import com.resumeai.notification.dto.SendNotificationRequest;
import com.resumeai.notification.entity.Notification;
import com.resumeai.notification.repository.NotificationRepository;
import com.resumeai.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NotificationServiceImpl}.
 * All tests follow the Arrange-Act-Assert (AAA) pattern.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    // ── Shared helpers ────────────────────────────────────────────────────────

    private Notification buildNotification(String id, boolean isRead) {
        return Notification.builder()
                .notificationId(id)
                .recipientId("user-001")
                .type(Notification.NotificationType.RESUME_CREATED)
                .title("Resume Created")
                .message("Your resume was created.")
                .channel(Notification.Channel.APP)
                .isRead(isRead)
                .build();
    }

    private SendNotificationRequest buildSendRequest() {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId("user-001");
        req.setType("RESUME_CREATED");
        req.setMessage("Your resume was created.");
        req.setChannel("APP");
        return req;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // send()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("send()")
    class Send {

        @Test
        @DisplayName("should save and return a notification response")
        void send_validRequest_savesAndReturnsResponse() {
            // Arrange
            SendNotificationRequest request = buildSendRequest();
            Notification saved = buildNotification("notif-001", false);
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            // Act
            NotificationResponse response = notificationService.send(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getNotificationId()).isEqualTo("notif-001");
            assertThat(response.isRead()).isFalse();
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("should auto-generate title from type when title is blank")
        void send_blankTitle_usesFormattedTypeAsTitle() {
            // Arrange
            SendNotificationRequest request = buildSendRequest();
            request.setTitle(""); // blank – should trigger formatTitle()
            request.setType("PLAN_UPGRADED");

            Notification saved = Notification.builder()
                    .notificationId("notif-002")
                    .recipientId("user-001")
                    .type(Notification.NotificationType.PLAN_UPGRADED)
                    .title("Plan upgraded")
                    .message("Congrats!")
                    .channel(Notification.Channel.APP)
                    .isRead(false)
                    .build();
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            // Act
            NotificationResponse response = notificationService.send(request);

            // Assert
            assertThat(response.getTitle()).isEqualTo("Plan upgraded");
        }

        @Test
        @DisplayName("should throw BAD_REQUEST for an invalid notification type")
        void send_invalidType_throwsBadRequest() {
            // Arrange
            SendNotificationRequest request = buildSendRequest();
            request.setType("INVALID_TYPE");

            // Act & Assert
            assertThatThrownBy(() -> notificationService.send(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid notification type");
        }

        @Test
        @DisplayName("should fall back to APP channel when channel is unrecognised")
        void send_unknownChannel_defaultsToApp() {
            // Arrange
            SendNotificationRequest request = buildSendRequest();
            request.setChannel("SLACK"); // not a valid channel

            Notification saved = buildNotification("notif-003", false);
            // The service falls back to APP silently, so just verify save is called
            when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

            // Act
            NotificationResponse response = notificationService.send(request);

            // Assert
            assertThat(response).isNotNull();
            verify(notificationRepository).save(argThat(n ->
                    n.getChannel() == Notification.Channel.APP));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // sendBulk()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendBulk()")
    class SendBulk {

        @Test
        @DisplayName("should save one notification per recipient")
        void sendBulk_multipleRecipients_savesAll() {
            // Arrange
            BroadcastRequest request = new BroadcastRequest();
            request.setRecipientIds(List.of("user-001", "user-002", "user-003"));
            request.setType("ADMIN_BROADCAST");
            request.setTitle("Maintenance tonight");
            request.setMessage("The system will be down at midnight.");

            // Act
            notificationService.sendBulk(request);

            // Assert
            verify(notificationRepository).saveAll(argThat((List<Notification> list) ->
                    list.size() == 3));
        }

        @Test
        @DisplayName("should do nothing when recipient list is empty")
        void sendBulk_emptyRecipients_doesNotSave() {
            // Arrange
            BroadcastRequest request = new BroadcastRequest();
            request.setRecipientIds(List.of());
            request.setType("ADMIN_BROADCAST");
            request.setMessage("Ignored.");

            // Act
            notificationService.sendBulk(request);

            // Assert
            verify(notificationRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("should do nothing when recipient list is null")
        void sendBulk_nullRecipients_doesNotSave() {
            // Arrange
            BroadcastRequest request = new BroadcastRequest();
            request.setRecipientIds(null);
            request.setType("ADMIN_BROADCAST");
            request.setMessage("Ignored.");

            // Act
            notificationService.sendBulk(request);

            // Assert
            verify(notificationRepository, never()).saveAll(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // markAsRead()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("should set isRead=true and return the updated notification")
        void markAsRead_unreadNotification_marksRead() {
            // Arrange
            Notification notif = buildNotification("notif-001", false);
            when(notificationRepository.findById("notif-001")).thenReturn(Optional.of(notif));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            NotificationResponse response = notificationService.markAsRead("notif-001");

            // Assert
            assertThat(response.isRead()).isTrue();
            verify(notificationRepository).save(notif);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when notification does not exist")
        void markAsRead_nonExistingNotification_throwsNotFound() {
            // Arrange
            when(notificationRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> notificationService.markAsRead("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Notification not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // markAllRead()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAllRead()")
    class MarkAllRead {

        @Test
        @DisplayName("should mark all unread notifications as read for a recipient")
        void markAllRead_withUnreadNotifications_marksAll() {
            // Arrange
            Notification n1 = buildNotification("notif-001", false);
            Notification n2 = buildNotification("notif-002", false);
            when(notificationRepository.findByRecipientIdAndIsReadFalseOrderBySentAtDesc("user-001"))
                    .thenReturn(List.of(n1, n2));

            // Act
            notificationService.markAllRead("user-001");

            // Assert
            assertThat(n1.isRead()).isTrue();
            assertThat(n2.isRead()).isTrue();
            verify(notificationRepository).saveAll(List.of(n1, n2));
        }

        @Test
        @DisplayName("should do nothing when there are no unread notifications")
        void markAllRead_noUnread_savesEmptyList() {
            // Arrange
            when(notificationRepository.findByRecipientIdAndIsReadFalseOrderBySentAtDesc("user-001"))
                    .thenReturn(List.of());

            // Act
            notificationService.markAllRead("user-001");

            // Assert
            verify(notificationRepository).saveAll(List.of());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getByRecipient()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByRecipient()")
    class GetByRecipient {

        @Test
        @DisplayName("should return sorted notifications for the recipient")
        void getByRecipient_withNotifications_returnsList() {
            // Arrange
            Notification n1 = buildNotification("notif-001", true);
            Notification n2 = buildNotification("notif-002", false);
            when(notificationRepository.findByRecipientIdOrderBySentAtDesc("user-001"))
                    .thenReturn(List.of(n1, n2));

            // Act
            List<NotificationResponse> responses = notificationService.getByRecipient("user-001");

            // Assert
            assertThat(responses).hasSize(2);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUnreadCount()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCount {

        @Test
        @DisplayName("should return the count of unread notifications")
        void getUnreadCount_returnsCorrectCount() {
            // Arrange
            when(notificationRepository.countByRecipientIdAndIsReadFalse("user-001")).thenReturn(5L);

            // Act
            long count = notificationService.getUnreadCount("user-001");

            // Assert
            assertThat(count).isEqualTo(5L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteNotification()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteNotification()")
    class DeleteNotification {

        @Test
        @DisplayName("should delete the notification when it exists")
        void deleteNotification_existingNotification_deletesFromRepo() {
            // Arrange
            Notification notif = buildNotification("notif-001", false);
            when(notificationRepository.findById("notif-001")).thenReturn(Optional.of(notif));

            // Act
            notificationService.deleteNotification("notif-001");

            // Assert
            verify(notificationRepository).delete(notif);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when notification does not exist")
        void deleteNotification_nonExistingNotification_throwsNotFound() {
            // Arrange
            when(notificationRepository.findById("ghost-id")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> notificationService.deleteNotification("ghost-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Notification not found");
        }
    }
}
