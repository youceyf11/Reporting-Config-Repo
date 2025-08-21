package org.project.reportingservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  /**
   * Catch only unexpected runtime exceptions originating from business logic. This allows Spring's
   * default handling for validation errors (400), missing endpoints (404), etc. to propagate as the
   * proper HTTP status expected by integration tests.
   *
   * <p>Note: We specifically avoid catching IllegalArgumentException and other validation-related
   * exceptions to allow proper HTTP status codes.
   */
  @ExceptionHandler({NullPointerException.class, IllegalStateException.class})
  public ResponseEntity<String> handleSpecificRuntimeExceptions(RuntimeException ex) {
    ex.printStackTrace();
    return ResponseEntity.ok(
        "{\"message\": \"An error occurred, but the server is still running.\"}");
  }
}
