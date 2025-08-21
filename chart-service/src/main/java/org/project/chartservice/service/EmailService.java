package org.project.chartservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.project.chartservice.IService.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
public class EmailService implements IEmailService {

  private final JavaMailSender mailSender;

  @Autowired
  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public Mono<Void> sendChartByEmail(
      String toEmail, String subject, byte[] chartData, String chartName, String projectKey) {
    return Mono.fromRunnable(
            () -> {
              try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);

                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(
                    String.format(
                        "Please find attached the %s chart for project %s.", chartName, projectKey),
                    false);

                helper.addAttachment(
                    String.format("%s-%s.png", chartName.replace(" ", "_"), projectKey),
                    new ByteArrayResource(chartData));

                mailSender.send(message);
              } catch (MessagingException e) {
                throw new RuntimeException("Failed to send email", e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
  }
}
