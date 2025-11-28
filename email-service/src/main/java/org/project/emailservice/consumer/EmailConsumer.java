package org.project.emailservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.emailservice.config.RabbitConfig;
import org.project.emailservice.dto.EmailRequestDto;
import org.project.emailservice.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void consumeEmailRequest(EmailRequestDto request) {
        log.info("📩 Received email request from RabbitMQ: {}", request.getSubject());
        emailService.sendEmailWithAttachment(request);
    }
}