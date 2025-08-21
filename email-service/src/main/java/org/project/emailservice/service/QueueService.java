package org.project.emailservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.project.emailservice.dto.EmailMessagePayload;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.enums.EmailPriority;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Sender;

/**
 * Service de gestion des files d'attente RabbitMQ. Cette classe utilise RabbitTemplate pour envoyer
 * des messages à RabbitMQ.
 */
@Service
@Slf4j
public class QueueService {

  @Value("${rabbitmq.routing.key.urgent}")
  private String urgentPriorityRoutingKey;

  @Value("${rabbitmq.routing.key.high}")
  private String highPriorityRoutingKey;

  @Value("${rabbitmq.routing.key.normal}")
  private String normalPriorityRoutingKey;

  @Value("${rabbitmq.routing.key.low}")
  private String lowPriorityRoutingKey;

  @Value("${rabbitmq.exchange.name}")
  private String exchangeName;

  private final RabbitTemplate rabbitTemplate;

  private final Sender sender;

  private final ObjectMapper objectMapper;

  public QueueService(RabbitTemplate rabbitTemplate, Sender sender, ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.sender = sender;
    this.objectMapper = objectMapper;

    // Enable mandatory flag so unroutable messages are returned
    this.rabbitTemplate.setMandatory(true);
    this.rabbitTemplate.setReturnsCallback(
        returned -> {
          log.error(
              "Message returned by broker. ReplyCode={} replyText={} exchange={} routingKey={}",
              returned.getReplyCode(),
              returned.getReplyText(),
              returned.getExchange(),
              returned.getRoutingKey());
        });
    this.rabbitTemplate.setConfirmCallback(
        (correlation, ack, cause) -> {
          if (ack) {
            log.debug("Broker ACK for correlationData={} ", correlation);
          } else {
            log.error("Broker NACK for correlationData={} cause={}", correlation, cause);
          }
        });
    this.rabbitTemplate.setChannelTransacted(false);
  }

  /**
   * Met en file d'attente un email pour un traitement asynchrone.
   *
   * @param emailRequest Les détails de l'email à envoyer.
   * @param emailId L'ID unique de l'email pour le suivi.
   * @return Mono&lt;Void&gt; qui se complete quand le message est envoyé
   */
  public Mono<Void> queueEmail(EmailRequest emailRequest, String emailId) {
    return Mono.fromRunnable(
        () -> {
          String routingKey = getRoutingKeyForPriority(emailRequest.getPriority());
          EmailMessagePayload payload = new EmailMessagePayload(emailId, emailRequest, routingKey);
          log.info(
              "Sending email payload to queue for emailId: {} with routing key: {}",
              emailId,
              routingKey);
          try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, jsonPayload);
          } catch (Exception e) {
            log.error("Failed to serialise email payload for id {}", emailId, e);
          }
        });
  }

  private String getRoutingKeyForPriority(EmailPriority priority) {
    if (priority == null) {
      return normalPriorityRoutingKey;
    }
    switch (priority) {
      case URGENT:
        return urgentPriorityRoutingKey;
      case HIGH:
        return highPriorityRoutingKey;
      case LOW:
        return lowPriorityRoutingKey;
      case NORMAL:
      default:
        return normalPriorityRoutingKey;
    }
  }
}
