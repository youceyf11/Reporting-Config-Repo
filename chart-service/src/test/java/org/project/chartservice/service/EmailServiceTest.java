package org.project.chartservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class EmailServiceTest {

  @Test
  void sendChartByEmail_sendsMimeMessage() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    EmailService emailService = new EmailService(mailSender);

    byte[] dummy = new byte[] {1, 2, 3};
    Mono<Void> mono =
        emailService.sendChartByEmail("dest@mail.com", "Subject", dummy, "Monthly Hours", "PROJ");

    StepVerifier.create(mono).verifyComplete();

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());
    assertThat(captor.getValue()).isSameAs(mimeMessage);
  }
}
