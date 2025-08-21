package org.project.reportingservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import org.project.issueevents.events.IssueUpsertedEvent;
import org.project.reportingservice.config.KafkaConfig;
import org.project.reportingservice.dto.ReportingResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

@Component
public class IssueUpsertedConsumer {

  private static final Logger logger = LoggerFactory.getLogger(IssueUpsertedConsumer.class);
  private static final String TOPIC = "jira.issue.upserted";
  private static final String GROUP_ID = "reporting-service-issues";

  private final KafkaConfig kafkaConfig;
  private final ObjectMapper mapper;
  private KafkaReceiver<String, String> receiver;
  private Disposable subscription;
  private final ReportingService reportingService;
  private final ReactiveRedisTemplate<String, ReportingResultDto> cache;
  private final KafkaTemplate<String, ReportingResultDto> kafkaTemplate;

  public IssueUpsertedConsumer(
      KafkaConfig kafkaConfig,
      ReportingService reportingService,
      ReactiveRedisTemplate<String, ReportingResultDto> cache,
      KafkaTemplate<String, ReportingResultDto> kafkaTemplate) {
    this.kafkaConfig = kafkaConfig;
    this.reportingService = reportingService;
    this.cache = cache;
    this.kafkaTemplate = kafkaTemplate;
    this.mapper = new ObjectMapper();
    this.mapper.registerModule(new JavaTimeModule());
  }

  @PostConstruct
  public void subscribe() {
    logger.info("🚀 Starting Kafka consumer for topic: {} with group: {}", TOPIC, GROUP_ID);

    ReceiverOptions<String, String> options =
        kafkaConfig.baseReceiverOptions(GROUP_ID).subscription(List.of(TOPIC));

    this.receiver = KafkaReceiver.create(options);

    this.subscription =
        receiver
            .receive()
            .concatMap(
                record -> {
                  try {
                    logger.debug(
                        "📥 Received message from topic: {}, partition: {}, offset: {}, key: {}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key());

                    IssueUpsertedEvent event =
                        mapper.readValue(record.value(), IssueUpsertedEvent.class);
                    logger.info("✅ Parsed issue event: {}", event);

                    return processIssueEvent(event)
                        .doOnSuccess(
                            v ->
                                logger.debug(
                                    "✅ Successfully processed issue: {}", event.getIssueKey()))
                        .doOnError(
                            error ->
                                logger.error(
                                    "❌ Failed to process issue: {}", event.getIssueKey(), error))
                        .then(record.receiverOffset().commit())
                        .doOnSuccess(
                            v ->
                                logger.debug(
                                    "✅ Committed offset for issue: {}", event.getIssueKey()));

                  } catch (Exception e) {
                    logger.error(
                        "❌ Failed to parse message from topic: {}, key: {}",
                        record.topic(),
                        record.key(),
                        e);
                    // Commit even on parse error to avoid infinite retry
                    return record.receiverOffset().commit();
                  }
                })
            .onErrorContinue(
                (error, obj) -> {
                  logger.error("💥 Consumer error, continuing...", error);
                })
            .subscribe();

    logger.info("✅ Kafka consumer started successfully for topic: {}", TOPIC);
  }

  private Mono<Void> processIssueEvent(IssueUpsertedEvent event) {
    return reportingService // 1. Recalculate aggregates
        .generateMonthlyReport(event.getProjectKey())
        .flatMap(
            report -> // 2. Cache the new report and propagate it
            cache
                    .opsForValue()
                    .set("report:" + event.getProjectKey(), report, Duration.ofMinutes(10))
                    // After caching, notify other services via Kafka
                    .then(
                        Mono.fromFuture(
                            kafkaTemplate.send(
                                "report.monthly.updated", event.getProjectKey(), report)))
                    .thenReturn(report))
        .then() // convert to Mono<Void> at the end for simpler generics
        .onErrorResume(
            ex -> { // 3. Never block the consumer
              logger.error("Processing failed", ex);
              return Mono.empty();
            });
  }

  @PreDestroy
  public void shutdown() {
    if (subscription != null && !subscription.isDisposed()) {
      logger.info("🛑 Disposing Kafka consumer subscription for topic: {}", TOPIC);
      subscription.dispose();
    }
  }
}
