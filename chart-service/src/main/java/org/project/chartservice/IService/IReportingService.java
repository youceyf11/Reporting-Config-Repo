package org.project.chartservice.IService;

import org.project.chartservice.dto.ReportingResultDto;
import reactor.core.publisher.Mono;

public interface IReportingService {

  Mono<ReportingResultDto> getMonthlyReportingData(String projectKey);
}
