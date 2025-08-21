package org.project.emailservice.provider;

import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.emailservice.dto.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class GmailProvider implements EmailProvider {

  private final JavaMailSender javaMailSender;

  @Value("${spring.mail.username:}")
  private String defaultFrom;

  @Override
  public String getName() {
    return "Gmail";
  }

  @Override
  public boolean isAvailable() {
    try {
      // Test de connexion simple
      javaMailSender.createMimeMessage();
      return true;
    } catch (Exception e) {
      log.warn("Gmail provider not available: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public Mono<String> sendEmail(EmailRequest emailRequest) {
    return Mono.fromCallable(
        () -> {
          try {
            log.info("Sending email via Gmail to: {}", emailRequest.getTo());

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailRequest.getTo());
            // Utilise l'adresse fournie, sinon la valeur de spring.mail.username
            String fromAddress =
                StringUtils.hasText(emailRequest.getFrom())
                    ? emailRequest.getFrom()
                    : (StringUtils.hasText(defaultFrom) ? defaultFrom : "no-reply@localhost");
            helper.setFrom(fromAddress);

            helper.setSubject(emailRequest.getSubject());

            if (emailRequest.getTemplateData() != null
                && emailRequest.getTemplateData().containsKey("htmlContent")) {
              helper.setText((String) emailRequest.getTemplateData().get("htmlContent"), true);
            } else {
              helper.setText("Default email content", false);
            }

            if (emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty()) {
              for (var attachment : emailRequest.getAttachments()) {
                DataSource dataSource =
                    new ByteArrayDataSource(attachment.getContent(), attachment.getContentType());
                helper.addAttachment(attachment.getFilename(), dataSource);
              }
            }

            javaMailSender.send(message);
            log.info("Email sent successfully to: {}", emailRequest.getTo());
            return "Email sent successfully";

          } catch (MessagingException e) {
            log.error("Failed to send email via Gmail", e);
            throw new RuntimeException("Email sending failed", e);
          }
        });
  }

  // Custom ByteArrayDataSource implementation
  private static class ByteArrayDataSource implements DataSource {
    private final byte[] data;
    private final String contentType;

    public ByteArrayDataSource(byte[] data, String contentType) {
      this.data = data;
      this.contentType = contentType;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(data);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      throw new IOException("Read-only data source");
    }

    @Override
    public String getContentType() {
      return contentType;
    }

    @Override
    public String getName() {
      return "ByteArrayDataSource";
    }
  }

  @Override
  public int getPriority() {
    return 1; // Priorité haute pour Gmail
  }
}
