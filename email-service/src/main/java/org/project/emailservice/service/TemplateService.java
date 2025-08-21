package org.project.emailservice.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateService {

  private final TemplateEngine templateEngine;

  public Mono<String> renderTemplate(String templateName, Map<String, Object> variables) {
    return Mono.fromCallable(
        () -> {
          try {
            log.debug("Rendering template: {} with variables: {}", templateName, variables);

            Context context = new Context();
            if (variables != null) {
              variables.forEach(context::setVariable);
            }

            String htmlContent = templateEngine.process(templateName, context);

            log.debug("Template rendered successfully: {}", templateName);
            return htmlContent;

          } catch (Exception e) {
            log.error("Failed to render template: {}", templateName, e);
            throw new RuntimeException("Template rendering failed: " + e.getMessage());
          }
        });
  }

  public Mono<String> renderChartEmailTemplate(Map<String, Object> chartData) {
    // Ajout de données par défaut pour le template chart-email
    chartData.putIfAbsent("subject", "Chart Report");
    chartData.putIfAbsent("generatedAt", java.time.Instant.now().toString());

    return renderTemplate("chart-email", chartData);
  }

  public Mono<String> renderSimpleTemplate(String subject, String content) {
    Map<String, Object> variables =
        Map.of(
            "subject", subject,
            "content", content);

    return Mono.fromCallable(
        () -> {
          // Template HTML simple si pas de template spécifique
          return String.format(
              """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                </head>
                <body>
                    <h2>%s</h2>
                    <p>%s</p>
                </body>
                </html>
                """,
              subject, subject, content);
        });
  }
}
