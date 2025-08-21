package org.project.chartservice.IService;

import org.project.chartservice.enums.ChartType;
import reactor.core.publisher.Mono;

public interface IChartService {
  Mono<byte[]> generateMonthlyChart(String projectKey, ChartType chartType);

  Mono<Void> generateAndEmailChart(String projectKey, ChartType chartType, String toEmail);

  Mono<byte[]> generateWeeklyChart(String projectKey, ChartType chartType);

  Mono<byte[]> generateComparativeChart(String projectKey);
}
