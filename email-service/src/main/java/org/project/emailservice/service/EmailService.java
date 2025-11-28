package org.project.emailservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.project.emailservice.dto.EmailRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmailWithAttachment(EmailRequestDto request) {
        try {
            log.info("📧 Sending email to: {}", request.getTo());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody());

            if (request.getAttachmentData() != null && request.getAttachmentName() != null) {
                helper.addAttachment(
                        request.getAttachmentName(),
                        new ByteArrayResource(request.getAttachmentData())
                );
            }

            mailSender.send(message);
            log.info("✅ Email sent successfully to {}", request.getTo());

        } catch (MessagingException e) {
            log.error("❌ Failed to send email: {}", e.getMessage());
        }
    }
}