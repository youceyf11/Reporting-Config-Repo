package org.project.chartservice.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.project.chartservice.enums.ChartType;
import org.project.chartservice.service.ChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest // charge uniquement la couche web
class ChartControllerTest {

  @Autowired private WebTestClient webClient;

  @MockBean private ChartService chartService;

  @Test
  void getMonthlyChart_returnsPngBytes() {
    when(chartService.generateMonthlyChart(eq("PROJ"), eq(ChartType.MONTHLY_BAR)))
        .thenReturn(Mono.just(new byte[] {99}));

    webClient
        .get()
        .uri("/api/charts/monthly/summary?projectKey=PROJ")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.IMAGE_PNG)
        .expectBody()
        .consumeWith(resp -> assertTrue(resp.getResponseBody().length > 0));
  }

  @Test
  void emailChart_returnsConfirmation() {
    when(chartService.generateAndEmailChart(
            eq("PROJ"), eq(ChartType.MONTHLY_BAR), eq("dest@mail.com")))
        .thenReturn(Mono.empty());

    webClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/charts/monthly/email")
                    .queryParam("projectKey", "PROJ")
                    .queryParam("chartType", "MONTHLY_BAR")
                    .queryParam("email", "dest@mail.com")
                    .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Chart sent successfully to dest@mail.com");
  }
}
