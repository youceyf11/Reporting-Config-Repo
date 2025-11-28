package org.project.emailservice.controller;

import lombok.RequiredArgsConstructor;
import org.project.emailservice.dto.EmailRequestDto;
import org.project.emailservice.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    // Simple endpoint to test SMTP without RabbitMQ
    @PostMapping("/test")
    public ResponseEntity<String> testEmail(@RequestParam String to) {
        EmailRequestDto request = EmailRequestDto.builder()
                .to(to)
                .subject("Test Email")
                .body("This is a test email from the Email Service.")
                .build();

        emailService.sendEmailWithAttachment(request);
        return ResponseEntity.ok("Test email sent to " + to);
    }

    @GetMapping("/health")
    public String health() {
        return "Email Service is UP";
    }
}