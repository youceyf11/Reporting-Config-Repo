package org.project.jirafetchservice.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
    Map<String, String> error = new HashMap<>();
    error.put("error", "Argument invalide");
    error.put("message", ex.getMessage());
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, String>> handleValidation(ConstraintViolationException ex) {
    Map<String, String> error = new HashMap<>();
    error.put("error", "Validation échouée");
    error.put("message", ex.getMessage());
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
    Map<String, String> error = new HashMap<>();
    error.put("error", "Erreur Integererne");
    error.put("message", "Service temporairement indisponible");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
