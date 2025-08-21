package org.project.reportingservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.project.issueevents.events.IssueUpsertedEvent;
import org.project.reportingservice.ReportingServiceApplication;
import org.project.reportingservice.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ReportingServiceApplication.class)
@DirtiesContext
@ActiveProfiles("ci")
public class KafkaFlowIT {

  @Autowired private KafkaTemplate<String, IssueUpsertedEvent> kafkaTemplate;

  @Autowired private ReportingService reportingService;

  @Test
  void issueUpsertEvent_reaches_reportingService() {
    // 1) simulate jira-fetch publishing
    IssueUpsertedEvent evt =
        new IssueUpsertedEvent("PROJ", "PROJ-1", "user@example.com", 8.0, Instant.now());
    kafkaTemplate.send("jira.issue.upserted", evt.getProjectKey(), evt);

    // 2) wait & assert reporting-service produced a report
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> assertThat(reportingService.generateMonthlyReport("PROJ").block()).isNotNull());
  }
}
