package org.project.emailservice.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.provider.EmailProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProviderService {

  private final List<EmailProvider> providers;
  private final CircuitBreakerRegistry circuitBreakerRegistry;

  public Mono<String> sendEmail(EmailRequest emailRequest) {
    return getAvailableProvider()
        .flatMap(provider -> sendWithCircuitBreaker(provider, emailRequest))
        .doOnSuccess(messageId -> log.info("Email sent successfully with messageId: {}", messageId))
        .doOnError(error -> log.error("Failed to send email after trying all providers", error));
  }

  private Mono<EmailProvider> getAvailableProvider() {
    return Mono.fromCallable(
        () ->
            providers.stream()
                .filter(EmailProvider::isAvailable)
                .sorted(Comparator.comparingInt(EmailProvider::getPriority))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No email provider available")));
  }

  private Mono<String> sendWithCircuitBreaker(EmailProvider provider, EmailRequest emailRequest) {
    CircuitBreaker circuitBreaker =
        circuitBreakerRegistry.circuitBreaker(provider.getName().toLowerCase());

    return Mono.fromCallable(
        () -> circuitBreaker.executeSupplier(() -> provider.sendEmail(emailRequest).block()));
  }

  public Mono<Boolean> isProviderAvailable(String providerName) {
    return Mono.fromCallable(
        () ->
            providers.stream()
                .filter(p -> p.getName().equalsIgnoreCase(providerName))
                .findFirst()
                .map(EmailProvider::isAvailable)
                .orElse(false));
  }
}
