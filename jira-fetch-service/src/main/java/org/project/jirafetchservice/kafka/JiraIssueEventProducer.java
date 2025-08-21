package org.project.jirafetchservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.project.issueevents.events.IssueUpsertedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Component
public class JiraIssueEventProducer {

  private static final Logger logger = LoggerFactory.getLogger(JiraIssueEventProducer.class);
  private static final String TOPIC = "jira.issue.upserted";

  private final KafkaSender<String, String> sender;
  private final ObjectMapper mapper;

  public JiraIssueEventProducer(KafkaSender<String, String> sender) {
    this.sender = sender;
    this.mapper = new ObjectMapper();
    this.mapper.registerModule(new JavaTimeModule());
  }

  public Mono<Void> publish(IssueUpsertedEvent event) {
    return Mono.fromCallable(
            () -> {
              try {
                String key = event.getIssueKey();
                String payload = mapper.writeValueAsString(event);

                logger.debug("📤 Publishing issue event: {} to topic: {}", key, TOPIC);

                var record = SenderRecord.create(new ProducerRecord<>(TOPIC, key, payload), key);
                return record;
              } catch (Exception e) {
                logger.error("❌ Failed to serialize event for issue: {}", event.getIssueKey(), e);
                throw new RuntimeException("Failed to serialize issue event", e);
              }
            })
        .flatMap(
            record ->
                sender
                    .send(Mono.just(record))
                    .doOnNext(
                        result ->
                            logger.info(
                                "✅ Successfully published issue event: {} to partition: {}",
                                event.getIssueKey(),
                                result.recordMetadata().partition()))
                    .doOnError(
                        error ->
                            logger.error(
                                "❌ Failed to publish issue event: {}", event.getIssueKey(), error))
                    .then())
        .onErrorResume(
            error -> {
              logger.error(
                  "💥 Critical error publishing issue event: {}", event.getIssueKey(), error);
              return Mono.empty(); // Don't fail the main flow, just log the error
            });
  }
}
