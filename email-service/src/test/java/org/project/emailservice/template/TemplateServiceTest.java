package org.project.emailservice.template;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.project.emailservice.service.TemplateService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.test.StepVerifier;

class TemplateServiceTest {

  @Test
  void renderTemplate_returns_engine_output() {
    TemplateEngine engine = Mockito.mock(TemplateEngine.class);
    Mockito.when(engine.process(Mockito.eq("chart-email"), Mockito.any(Context.class)))
        .thenReturn("<h1>OK</h1>");

    TemplateService svc = new TemplateService(engine);

    StepVerifier.create(svc.renderTemplate("chart-email", Map.of("k", "v")))
        .expectNext("<h1>OK</h1>")
        .verifyComplete();
  }
}
