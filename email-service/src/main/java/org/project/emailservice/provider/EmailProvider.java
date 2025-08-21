package org.project.emailservice.provider;

import org.project.emailservice.dto.EmailRequest;
import reactor.core.publisher.Mono;

public interface EmailProvider {
  String getName();

  boolean isAvailable();

  Mono<String> sendEmail(EmailRequest emailRequest);

  int getPriority();
}
