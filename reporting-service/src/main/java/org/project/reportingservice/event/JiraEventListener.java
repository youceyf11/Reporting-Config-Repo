package org.project.reportingservice.event;

import org.project.issueevents.events.IssueUpsertedEvent;
import org.project.reportingservice.service.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JiraEventListener {

    private static final Logger logger = LoggerFactory.getLogger(JiraEventListener.class);
    private final ReportingService reportingService;

    public JiraEventListener(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @KafkaListener(
            topics = "jira.issue.upserted",
            groupId = "reporting-group",
            containerFactory = "kafkaListenerContainerFactory" // Ensure this exists in config if customizing
    )
    public void handleIssueUpserted(IssueUpsertedEvent event) {
        logger.info("Received Kafka Event: Issue {} updated for {}", event.getIssueKey(), event.getAssignee());
        reportingService.processEvent(event);
    }
}