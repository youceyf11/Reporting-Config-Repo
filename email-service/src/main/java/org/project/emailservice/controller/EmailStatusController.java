package org.project.emailservice.controller;

import java.util.List;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/email/status")
public class EmailStatusController {

  private final EmailService emailService;

  public EmailStatusController(EmailService emailService) {
    this.emailService = emailService;
  }

  @GetMapping("/{emailId}")
  public Mono<ResponseEntity<EmailResponse>> getEmailStatus(@PathVariable String emailId) {
    return emailService
        .getEmailStatus(emailId)
        .map(ResponseEntity::ok)
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
  }

  @PostMapping("/bulk")
  public Flux<EmailResponse> getBulkEmailStatus(@RequestBody List<String> emailIds) {
    return emailService.getBulkEmailStatus(emailIds);
  }
}
