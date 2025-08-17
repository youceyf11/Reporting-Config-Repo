package org.project.emailservice.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.enums.EmailStatus;
import org.project.emailservice.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = EmailController.class)
@ActiveProfiles("test")
class EmailControllerTest {

    @Autowired WebTestClient client;
    @MockBean EmailService emailService;

    @Test
    void sendEmail_returns_202_and_body() {
        EmailRequest req = EmailRequest.builder()
                .to("u@test.com").from("me@test.com").subject("hi").build();

        EmailResponse res = EmailResponse.builder()
                .emailId("123").status(EmailStatus.QUEUED).build();

        Mockito.when(emailService.processEmailRequest(Mockito.any()))
               .thenReturn(Mono.just(res));

        client.post().uri("/api/emails/send")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(req)
              .exchange()
              .expectStatus().isAccepted()
              .expectBody()
              .jsonPath("$.emailId").isEqualTo("123")
              .jsonPath("$.status").isEqualTo("QUEUED");
    }

    @Test
    void getEmailStatus_404_when_not_found() {
        Mockito.when(emailService.getEmailStatus("notFound"))
               .thenReturn(Mono.just(
                       new EmailResponse("notFound", EmailStatus.NOT_FOUND, null, null, null, null)));

        client.get().uri("/api/emails/status/notFound")
              .exchange()
              .expectStatus().isOk()
              .expectBody()
              .jsonPath("$.status").isEqualTo("NOT_FOUND");
    }
}