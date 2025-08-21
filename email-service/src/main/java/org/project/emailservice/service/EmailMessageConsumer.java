package org.project.emailservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.project.emailservice.config.RabbitMQReactiveConfig;
import org.project.emailservice.dto.EmailMessagePayload;
import org.project.emailservice.enums.EmailStatus;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.Receiver;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class EmailMessageConsumer {

  private final Receiver receiver;
  private final ProviderService providerService;
  private final EmailService emailService;
  private final ObjectMapper objectMapper;
  private Disposable subscription;

  public EmailMessageConsumer(
      Receiver receiver,
      ProviderService providerService,
      EmailService emailService,
      ObjectMapper objectMapper) {
    this.receiver = receiver;
    this.providerService = providerService;
    this.emailService = emailService;
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  private void startConsuming() {
    log.info("Starting reactive consumers for all priority queues...");

    // Add small delay to ensure queues are created
    try {
      Thread.sleep(1000); // 1 second delay
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    Flux<Void> urgentPriorityConsumer =
        receiver
            .consumeManualAck(RabbitMQReactiveConfig.URGENT_PRIORITY_QUEUE_NAME)
            .doOnNext(
                d ->
                    log.info(
                        " URGENT: Received message from queue: {}",
                        RabbitMQReactiveConfig.URGENT_PRIORITY_QUEUE_NAME))
            .flatMap(delivery -> handleDelivery(delivery))
            .retryWhen(
                Retry.backoff(3, Duration.ofSeconds(5))
                    .doBeforeRetry(
                        rs ->
                            log.warn(
                                "Retrying URGENT consumer connection, attempt: {}",
                                rs.totalRetries() + 1)));

    Flux<Void> highPriorityConsumer =
        receiver
            .consumeManualAck(RabbitMQReactiveConfig.HIGH_PRIORITY_QUEUE_NAME)
            .doOnNext(
                d ->
                    log.info(
                        " HIGH: Received message from queue: {}",
                        RabbitMQReactiveConfig.HIGH_PRIORITY_QUEUE_NAME))
            .flatMap(this::handleDelivery);

    Flux<Void> normalPriorityConsumer =
        receiver
            .consumeManualAck(RabbitMQReactiveConfig.NORMAL_QUEUE_NAME)
            .doOnNext(
                d ->
                    log.info(
                        " NORMAL: Received message from queue: {}",
                        RabbitMQReactiveConfig.NORMAL_QUEUE_NAME))
            .flatMap(this::handleDelivery);

    Flux<Void> lowPriorityConsumer =
        receiver
            .consumeManualAck(RabbitMQReactiveConfig.LOW_PRIORITY_QUEUE_NAME)
            .doOnNext(
                d ->
                    log.info(
                        " LOW: Received message from queue: {}",
                        RabbitMQReactiveConfig.LOW_PRIORITY_QUEUE_NAME))
            .flatMap(this::handleDelivery);

    subscription =
        Flux.merge(
                urgentPriorityConsumer,
                highPriorityConsumer,
                normalPriorityConsumer,
                lowPriorityConsumer)
            .doOnSubscribe(s -> log.info("Email message consumers subscribed successfully"))
            .subscribe(
                null, // onNext - we don't need to handle Void results
                error -> log.error("Critical error in merged consumer stream", error),
                () -> log.info("Consumer stream completed"));

    log.info("Email message consumers started successfully");
  }

  private Mono<Void> processMessage(byte[] messageBody) {
    return Mono.fromCallable(
            () -> {
              if (messageBody == null) {
                log.warn("Received null message body, skipping message");
                return null;
              }

              String messageContent = new String(messageBody, StandardCharsets.UTF_8);
              if (messageContent.trim().isEmpty()) {
                log.warn("Received empty message content, skipping");
                return null;
              }

              try {
                EmailMessagePayload payload =
                    objectMapper.readValue(messageContent, EmailMessagePayload.class);
                if (payload == null || payload.getEmailId() == null) {
                  log.warn("Parsed payload is null or missing email ID, skipping");
                  return null;
                }
                return payload;
              } catch (Exception e) {
                log.error("Failed to deserialize message. Content: {}", messageContent, e);
                return null;
              }
            })
        .flatMap(
            payload -> {
              if (payload == null) {
                return Mono.<Void>empty();
              }

              log.info(
                  "Processing email request from queue for ID: {} using routingKey: {}",
                  payload.getEmailId(),
                  payload.getRoutingKey());
              return providerService
                  .sendEmail(payload.getRequest())
                  .flatMap(
                      msgId -> {
                        // Mark as SENT in Redis
                        return emailService
                            .updateEmailStatus(payload.getEmailId(), EmailStatus.SENT, null)
                            .thenReturn(msgId);
                      })
                  .doOnSubscribe(
                      s ->
                          log.debug(
                              "Invoking ProviderService for emailId={}", payload.getEmailId()))
                  .doOnSuccess(
                      mId ->
                          log.info(
                              "Successfully sent email for ID: {} , provider messageId={}",
                              payload.getEmailId(),
                              mId))
                  .onErrorResume(
                      e -> {
                        log.error("ProviderService failed for emailId={}", payload.getEmailId(), e);
                        // Mark as FAILED in Redis
                        return emailService
                            .updateEmailStatus(
                                payload.getEmailId(), EmailStatus.FAILED, e.getMessage())
                            .then(Mono.error(e));
                      })
                  .then();
            });
  }

  /** Traite le delivery et gère les ACK/NACK. */
  private Mono<Void> handleDelivery(AcknowledgableDelivery delivery) {
    log.debug(
        "Received delivery: tag={} routingKey={} size={} bytes",
        delivery.getEnvelope().getDeliveryTag(),
        delivery.getEnvelope().getRoutingKey(),
        delivery.getBody().length);
    return processMessage(delivery.getBody())
        .doOnSuccess(
            v -> {
              log.debug("ACKing message tag={}", delivery.getEnvelope().getDeliveryTag());
              delivery.ack();
            })
        .doOnError(
            e -> {
              log.error(
                  "Error while processing delivery tag={}",
                  delivery.getEnvelope().getDeliveryTag(),
                  e);
              delivery.nack(true);
            });
  }

  @PreDestroy
  public void stopConsuming() {
    if (subscription != null && !subscription.isDisposed()) {
      log.info("Stopping email message consumers...");
      subscription.dispose();
      log.info("Email message consumers stopped");
    }
  }
}
