package org.project.chartservice.IService;

import java.util.List;
import org.project.chartservice.dto.EmployeePerformanceDto;
import org.project.chartservice.enums.ChartType;
import reactor.core.publisher.Mono;

public interface IChartGenerationService {
  Mono<byte[]> generateChart(
      List<EmployeePerformanceDto> employees, ChartType chartType, String projectKey);
}
