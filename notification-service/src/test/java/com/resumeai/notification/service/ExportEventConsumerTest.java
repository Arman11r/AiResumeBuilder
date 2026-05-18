package com.resumeai.notification.service;

import com.resumeai.notification.dto.ExportCompletedEvent;
import com.resumeai.notification.dto.SendNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class ExportEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ExportEventConsumer exportEventConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConsumeExportCompletedEvent() {
        // 1. Set up the test conditions
        ExportCompletedEvent event = ExportCompletedEvent.builder()
                .jobId("job123")
                .userId("user456")
                .fileUrl("/tmp/file.pdf")
                .format("PDF")
                .completedAt(LocalDateTime.now())
                .build();

        // 2. Run the method under test
        exportEventConsumer.consumeExportCompletedEvent(event);

        // 3. Verify the outcome
        ArgumentCaptor<SendNotificationRequest> requestCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(notificationService).send(requestCaptor.capture());

        SendNotificationRequest capturedRequest = requestCaptor.getValue();
        assertEquals("user456", capturedRequest.getRecipientId());
        assertEquals("EXPORT_COMPLETED", capturedRequest.getType());
        assertEquals("Export Ready", capturedRequest.getTitle());
        assertEquals("Your PDF export (Job ID: job123) is complete and ready for download.", capturedRequest.getMessage());
        assertEquals("APP", capturedRequest.getChannel());
        assertEquals("job123", capturedRequest.getRelatedId());
        assertEquals("EXPORT_JOB", capturedRequest.getRelatedType());
    }
}
