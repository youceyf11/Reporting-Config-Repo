package org.project.chartservice.client;

import org.project.chartservice.dto.EmployeeMetricDto;
import org.project.chartservice.dto.WeeklySummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ReportingClient {

    private static final Logger logger = LoggerFactory.getLogger(ReportingClient.class);
    private final RestClient restClient;

    public ReportingClient(@Value("${reporting.service.url:http://localhost:8082}") String reportingUrl) {
        this.restClient = RestClient.builder().baseUrl(reportingUrl).build();
    }

    // Fetch Monthly Data
    public List<EmployeeMetricDto> getMonthlyMetrics(String period) {
        try {
            return restClient.get()
                    .uri("/api/reports/performance/{period}", period)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EmployeeMetricDto>>() {});
        } catch (Exception e) {
            logger.error("Failed to fetch monthly metrics: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // Fetch Weekly Data (The new Best Practice requirement)
    // Expecting Map<"Week 1", Velocity>
    public Map<String, WeeklySummaryDto> getWeeklyBreakdown(String startDate, String endDate) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reports/weekly-detailed")
                            .queryParam("startDate", startDate)
                            .queryParam("endDate", endDate)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, WeeklySummaryDto>>() {});
        } catch (Exception e) {
            logger.error("Failed to fetch weekly breakdown: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}