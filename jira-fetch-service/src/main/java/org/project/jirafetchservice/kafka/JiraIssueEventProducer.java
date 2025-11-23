package org.project.jirafetchservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.project.issueevents.events.IssueUpsertedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class JiraIssueEventProducer {

  private static final Logger logger = LoggerFactory.getLogger(JiraIssueEventProducer.class);
  private static final String TOPIC = "jira.issue.upserted";

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper mapper;

  public JiraIssueEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
    this.mapper = new ObjectMapper();
    this.mapper.registerModule(new JavaTimeModule());
  }

  public void publish(IssueUpsertedEvent event) {
    try {
      String key = event.getIssueKey();
      String payload = mapper.writeValueAsString(event);

      logger.debug("📤 Publishing issue event: {} to topic: {}", key, TOPIC);

      CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, key, payload);
      future.whenComplete((result, ex) -> {
        if (ex == null) {
          logger.info("✅ Successfully published issue event: {} to partition: {}", event.getIssueKey(), result.getRecordMetadata().partition());
        } else {
          logger.error("❌ Failed to publish issue event: {}", event.getIssueKey(), ex);
        }
      });
    } catch (Exception e) {
      logger.error("❌ Failed to serialize event for issue: {}", event.getIssueKey(), e);
    }
  }
}