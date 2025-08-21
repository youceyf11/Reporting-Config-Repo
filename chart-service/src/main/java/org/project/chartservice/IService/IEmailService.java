package org.project.chartservice.IService;

import reactor.core.publisher.Mono;

public interface IEmailService {
  Mono<Void> sendChartByEmail(
      String toEmail, String subject, byte[] chartData, String chartName, String projectKey);
}
