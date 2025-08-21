package org.project.chartservice.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.chartservice.dto.EmployeePerformanceDto;
import org.project.chartservice.enums.ChartType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ChartGenerationServiceTest {

  private final ChartGenerationService service = new ChartGenerationService();

  private static final List<EmployeePerformanceDto> SAMPLE_EMPLOYEES =
      List.of(
          new EmployeePerformanceDto("alice@mail.com", 120.0, 160.0),
          new EmployeePerformanceDto("bob@mail.com", 100.0, 160.0));

  @Nested
  class Success {

    @Test
    @DisplayName("WEEKLY_BAR génère un PNG non-vide")
    void weeklyChart() {
      Mono<byte[]> mono = service.generateChart(SAMPLE_EMPLOYEES, ChartType.WEEKLY_BAR, "PROJ");
      StepVerifier.create(mono)
          .assertNext(bytes -> assertThat(bytes).isNotEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("MONTHLY_BAR génère un PNG non-vide")
    void monthlyChart() {
      Mono<byte[]> mono = service.generateChart(SAMPLE_EMPLOYEES, ChartType.MONTHLY_BAR, "PROJ");
      StepVerifier.create(mono)
          .assertNext(bytes -> assertThat(bytes).isNotEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("COMPARATIVE génère un PNG non-vide")
    void comparativeChart() {
      Mono<byte[]> mono = service.generateChart(SAMPLE_EMPLOYEES, ChartType.COMPARATIVE, "PROJ");
      StepVerifier.create(mono)
          .assertNext(bytes -> assertThat(bytes).isNotEmpty())
          .verifyComplete();
    }
  }

  @Nested
  class Errors {

    @Test
    @DisplayName("Null employee list -> IllegalArgumentException")
    void nullEmployees() {
      Mono<byte[]> mono = service.generateChart(null, ChartType.MONTHLY_BAR, "PROJ");
      StepVerifier.create(mono)
          .expectErrorMatches(
              e -> e instanceof IllegalArgumentException && e.getMessage().contains("employés"))
          .verify();
    }
  }
}
