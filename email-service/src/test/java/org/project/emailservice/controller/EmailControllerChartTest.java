package org.project.emailservice.controller;

import static org.springframework.web.reactive.function.BodyInserters.fromMultipartData;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.enums.EmailStatus;
import org.project.emailservice.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = EmailController.class)
@ActiveProfiles("test")
class EmailControllerChartTest {

  @Autowired WebTestClient client;
  @MockBean EmailService emailService;

  @Test
  void sendChartEmail_multipart_returns_202() {
    EmailRequest meta =
        EmailRequest.builder().to("u@test.com").from("me@test.com").subject("chart").build();

    EmailResponse res = EmailResponse.builder().emailId("999").status(EmailStatus.QUEUED).build();

    Mockito.when(emailService.processEmailRequest(Mockito.any())).thenReturn(Mono.just(res));

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    String metaJson = "{\"to\":\"u@test.com\",\"from\":\"me@test.com\",\"subject\":\"chart\"}";
    builder.part("request", metaJson).contentType(MediaType.APPLICATION_JSON);
    builder.part("file", new ByteArrayResource("img".getBytes())).filename("chart.png");

    MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipartData = builder.build();

    client
        .post()
        .uri("/api/emails/send/chart")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(fromMultipartData(multipartData))
        .exchange()
        .expectStatus()
        .isAccepted()
        .expectBody()
        .jsonPath("$.emailId")
        .isEqualTo("999");
  }
}
