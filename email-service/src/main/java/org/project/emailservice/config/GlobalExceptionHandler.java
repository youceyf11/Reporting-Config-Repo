package org.project.emailservice.config;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleValidationExceptions(
      WebExchangeBindException ex) {

    Map<String, Object> errors = new HashMap<>();
    errors.put("error", "Validation failed");
    errors.put("status", HttpStatus.BAD_REQUEST.value());

    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
    errors.put("fieldErrors", fieldErrors);

    log.warn("Validation error: {}", fieldErrors);

    return Mono.just(ResponseEntity.badRequest().body(errors));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("error", "Internal server error");
    error.put("message", ex.getMessage());
    error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

    log.error("Unexpected error: ", ex);

    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
  }
}
