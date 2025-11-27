package org.project.excelservice.client;

import org.project.excelservice.dto.JiraIssueDto;
import org.project.excelservice.dto.ReportingDataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class ServiceClient {

    private final RestClient jiraClient;
    private final RestClient reportingClient;

    public ServiceClient(
            @Value("${jira.service.url:http://jira-fetch-service:8081}") String jiraUrl,
            @Value("${reporting.service.url:http://reporting-service:8082}") String reportingUrl) {

        this.jiraClient = RestClient.builder().baseUrl(jiraUrl).build();
        this.reportingClient = RestClient.builder().baseUrl(reportingUrl).build();
    }

    // 1. Fetch Raw Issues (For File 1)
    public List<JiraIssueDto> getRawIssues(String projectKey) {
        return jiraClient.get()
                .uri("/api/jira/projects/{key}/issues?limit=1000", projectKey)
                .retrieve()
                .body(new ParameterizedTypeReference<List<JiraIssueDto>>() {});
    }

    // 2. Fetch Weekly Detailed Report (For File 2)
    public Map<String, ReportingDataDto> getWeeklyReport(String start, String end) {
        return reportingClient.get()
                .uri(uri -> uri.path("/api/reports/weekly-detailed")
                        .queryParam("startDate", start)
                        .queryParam("endDate", end)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, ReportingDataDto>>() {});
    }
}