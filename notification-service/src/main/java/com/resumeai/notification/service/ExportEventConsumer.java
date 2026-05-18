package com.resumeai.notification.service;

import com.resumeai.notification.dto.ExportCompletedEvent;
import com.resumeai.notification.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.queue.export-notification}")
    public void consumeExportCompletedEvent(ExportCompletedEvent event) {
        log.info("Received ExportCompletedEvent for job: {}", event.getJobId());

        try {
            SendNotificationRequest request = new SendNotificationRequest();
            request.setRecipientId(event.getUserId());
            request.setType("EXPORT_COMPLETED");
            request.setTitle("Export Ready");
            request.setMessage(String.format("Your %s export (Job ID: %s) is complete and ready for download.", 
                    event.getFormat(), event.getJobId()));
            request.setChannel("APP");
            request.setRelatedId(event.getJobId());
            request.setRelatedType("EXPORT_JOB");

            notificationService.send(request);
            log.info("Successfully processed ExportCompletedEvent and sent notification for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to process ExportCompletedEvent: {}", e.getMessage(), e);
            // Re-throw if you want RabbitMQ to retry or send to a DLQ
            // throw e;
        }
    }
}
