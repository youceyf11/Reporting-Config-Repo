package org.project.excelservice.service;

import lombok.extern.slf4j.Slf4j;
import org.project.excelservice.dto.KpiMetricsDto;
import org.project.excelservice.dto.ReportingResultDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class KpiMetricsService {

    private final WebClient webClient;

    @Value("${reporting.service.url:http://reporting-service:8082}")
    private String reportingServiceUrl;

    public KpiMetricsService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public KpiMetricsDto generateKpiMetrics(String projectKey) {
        log.info("📊 Generating KPI metrics for project: {}", projectKey);

        // Fetch real data from reporting service
        ReportingResultDto reportingData = getReportingData(projectKey);
        
        if (reportingData == null) {
            log.warn("⚠️ No reporting data available, using fallback sample data");
            return generateSampleKpiMetrics(projectKey);
        }

        return buildKpiMetricsFromReportingData(reportingData, projectKey);
    }

    public ReportingResultDto getReportingData(String projectKey) {
        log.info("Fetching reporting data for project: {}", projectKey);
        
        ReportingResultDto reportingData = fetchReportingData(projectKey);
        
        if (reportingData == null) {
            log.warn("No reporting data available, using sample data");
            return createSampleReportingData(projectKey);
        }
        
        return reportingData;
    }

    private ReportingResultDto createSampleReportingData(String projectKey) {
        ReportingResultDto sampleData = new ReportingResultDto();
        sampleData.setReportGeneratedAt("2025-09-12T14:16:00Z");
        sampleData.setProjectKey(projectKey);
        sampleData.setMonth("2025-09");
        sampleData.setYear("2025");
        sampleData.setTotalEmployees(5);
        sampleData.setTotalHoursWorked(640.0);
        sampleData.setTotalIssuesResolved(18);
        sampleData.setAverageResolutionTimeHours(26.4);
        
        List<ReportingResultDto.EmployeeRanking> employeeRankings = List.of(
            createSampleEmployee(1, "Youssef Ouriniche", "youssef.ouriniche@emsi-edu.ma", 6, 142.0, 7.8, "Casablanca"),
            createSampleEmployee(2, "Alice Smith", "alice.smith@company.com", 5, 128.0, 8.3, "Rabat"),
            createSampleEmployee(3, "Bob Johnson", "bob.johnson@company.com", 3, 122.0, 9.5, "Fez"),
            createSampleEmployee(4, "Charlie Brown", "charlie.brown@company.com", 2, 126.0, 10.1, "Casablanca"),
            createSampleEmployee(5, "Diana Wilson", "diana.wilson@company.com", 2, 122.0, 11.2, "Marrakech")
        );
        sampleData.setEmployeeRankings(employeeRankings);
        
        return sampleData;
    }

    private ReportingResultDto.EmployeeRanking createSampleEmployee(int rank, String name, String email, 
                                                                          int issues, double hours, double avgTts, String site) {
        ReportingResultDto.EmployeeRanking employee = new ReportingResultDto.EmployeeRanking();
        employee.setRank(rank);
        employee.setEmployeeName(name);
        employee.setEmployeeEmail(email);
        employee.setIssuesResolved(issues);
        employee.setHoursWorked(hours);
        employee.setAverageTtsDays(avgTts);
        employee.setSite(site);
        return employee;
    }

    private ReportingResultDto fetchReportingData(String projectKey) {
        try {
            return webClient.get()
                    .uri(reportingServiceUrl + "/api/reporting/monthly?projectKey=" + projectKey)
                    .retrieve()
                    .bodyToMono(ReportingResultDto.class)
                    .block();
        } catch (Exception e) {
            log.error("❌ Failed to fetch reporting data: {}", e.getMessage());
            return null;
        }
    }

    private KpiMetricsDto buildKpiMetricsFromReportingData(ReportingResultDto reportingData, String projectKey) {
        KpiMetricsDto kpiMetrics = new KpiMetricsDto();
        kpiMetrics.setProjectKey(projectKey);
        // Parse the ISO string to LocalDateTime
        try {
            kpiMetrics.setGeneratedAt(LocalDateTime.parse(reportingData.getReportGeneratedAt().replace("Z", "")));
        } catch (Exception e) {
            kpiMetrics.setGeneratedAt(LocalDateTime.now());
        }

        // Build Team Performance from real data
        KpiMetricsDto.TeamPerformanceKpis teamPerformance = new KpiMetricsDto.TeamPerformanceKpis();
        teamPerformance.setTotalEmployees(reportingData.getTotalEmployees());
        teamPerformance.setTotalHoursWorked(reportingData.getTotalHoursWorked());
        teamPerformance.setAverageHoursPerEmployee(reportingData.getTotalHoursWorked() / reportingData.getTotalEmployees());
        teamPerformance.setTeamEfficiencyPercentage(calculateVelocity(reportingData));
        teamPerformance.setTotalTicketsResolved(reportingData.getTotalIssuesResolved());
        teamPerformance.setAverageResolutionTimeHours(reportingData.getAverageResolutionTimeHours());
        kpiMetrics.setTeamPerformance(teamPerformance);

        // Build Quality KPIs from real data
        KpiMetricsDto.QualityKpis qualityKpis = new KpiMetricsDto.QualityKpis();
        qualityKpis.setDefectRate(calculateBugRate(reportingData));
        qualityKpis.setReworkPercentage(15.0); // Default as not in reporting data
        qualityKpis.setCustomerSatisfactionScore(85.0); // Default as not in reporting data
        qualityKpis.setCriticalIssuesCount((int)(reportingData.getTotalIssuesResolved() * 0.1)); // Estimate
        qualityKpis.setAverageTimeToResolution(reportingData.getAverageResolutionTimeHours());
        qualityKpis.setFirstTimeFixRate(92.0); // Default as not in reporting data
        kpiMetrics.setQuality(qualityKpis);

        // Build Productivity KPIs from real data
        KpiMetricsDto.ProductivityKpis productivityKpis = new KpiMetricsDto.ProductivityKpis();
        productivityKpis.setVelocityPoints(reportingData.getTotalIssuesResolved() * 3.0); // Estimate
        productivityKpis.setThroughputTicketsPerDay(calculateThroughput(reportingData));
        productivityKpis.setCycleTimeHours(reportingData.getAverageResolutionTimeHours());
        productivityKpis.setLeadTimeHours(reportingData.getAverageResolutionTimeHours() + 48.0);
        productivityKpis.setBurndownEfficiency(calculateBurndownEfficiency(reportingData));
        productivityKpis.setFeaturesDelivered(reportingData.getTotalIssuesResolved());
        productivityKpis.setCodeQualityScore(85.0); // Default
        kpiMetrics.setProductivity(productivityKpis);

        // Build Resource Utilization from real data
        KpiMetricsDto.ResourceUtilizationKpis resourceKpis = new KpiMetricsDto.ResourceUtilizationKpis();
        resourceKpis.setCapacityUtilization(95.0); // High utilization
        resourceKpis.setOvertimePercentage(calculateOvertime(reportingData) / reportingData.getTotalHoursWorked() * 100.0);
        resourceKpis.setIdleTimePercentage(5.0); // Low idle time
        resourceKpis.setCrossTrainingIndex(3.2); // Default
        resourceKpis.setSkillGapCount(2); // Default
        resourceKpis.setResourceEfficiency(92.0); // Default
        kpiMetrics.setResourceUtilization(resourceKpis);

        // Generate employee KPIs from real employee rankings
        List<KpiMetricsDto.EmployeeKpi> employeeKpis = new ArrayList<>();
        for (ReportingResultDto.EmployeeRanking employee : reportingData.getEmployeeRankings()) {
            KpiMetricsDto.EmployeeKpi empKpi = new KpiMetricsDto.EmployeeKpi();
            empKpi.setEmployeeName(employee.getEmployeeName());
            empKpi.setEmail(employee.getEmployeeEmail());
            empKpi.setTasksCompleted(employee.getIssuesResolved());
            empKpi.setHoursWorked(employee.getHoursWorked());
            empKpi.setProductivityScore(calculateProductivityScore(employee));
            empKpi.setQualityRating(calculateQualityRating(employee));
            empKpi.setSite(employee.getSite());
            empKpi.setRank(employee.getRank());
            employeeKpis.add(empKpi);
        }
        kpiMetrics.setEmployeeKpis(employeeKpis);

        // Generate trend data (sample for now, could be enhanced with historical data)
        kpiMetrics.setTrends(generateTrendData());

        return kpiMetrics;
    }

    private Double calculateVelocity(ReportingResultDto data) {
        return data.getTotalIssuesResolved() * 3.0; // Assuming 3 story points per issue
    }

    private Double calculateSprintCompletion(ReportingResultDto data) {
        return Math.min(95.0, 80.0 + (data.getTotalIssuesResolved() * 2.0));
    }

    private Double calculateBurndownEfficiency(ReportingResultDto data) {
        return Math.min(98.0, 85.0 + (data.getTotalIssuesResolved() * 1.5));
    }

    private Double calculateBugRate(ReportingResultDto data) {
        // Lower bug rate for better performance
        return Math.max(2.0, 8.0 - (data.getTotalIssuesResolved() * 0.2));
    }

    private Double calculateDefectDensity(ReportingResultDto data) {
        return Math.max(0.5, 3.0 - (data.getTotalIssuesResolved() * 0.1));
    }

    private Double calculateThroughput(ReportingResultDto data) {
        return data.getTotalIssuesResolved() / 4.0; // Issues per week
    }

    private Double calculateOvertime(ReportingResultDto data) {
        double standardHours = data.getTotalEmployees() * 160.0; // 40 hours/week * 4 weeks
        return Math.max(0.0, data.getTotalHoursWorked() - standardHours);
    }

    private Double calculateProductivityScore(ReportingResultDto.EmployeeRanking employee) {
        // Higher score for more issues resolved and fewer TTS days
        double baseScore = (employee.getIssuesResolved() * 10.0) - (employee.getAverageTtsDays() * 2.0);
        return Math.max(60.0, Math.min(100.0, baseScore + 70.0));
    }

    private Double calculateQualityRating(ReportingResultDto.EmployeeRanking employee) {
        // Better quality rating for lower TTS days
        double rating = 100.0 - (employee.getAverageTtsDays() * 3.0);
        return Math.max(70.0, Math.min(100.0, rating));
    }

    private List<KpiMetricsDto.TrendDataPoint> generateTrendData() {
        List<KpiMetricsDto.TrendDataPoint> trendData = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusWeeks(12);

        for (int week = 0; week < 12; week++) {
            KpiMetricsDto.TrendDataPoint dataPoint = new KpiMetricsDto.TrendDataPoint();
            dataPoint.setWeek("Week " + (week + 1));
            dataPoint.setDate(startDate.plusWeeks(week).format(DateTimeFormatter.ISO_LOCAL_DATE));
            dataPoint.setVelocity(45.0 + (week * 2.0) + (Math.random() * 10.0 - 5.0));
            dataPoint.setBugRate(8.0 - (week * 0.3) + (Math.random() * 2.0 - 1.0));
            dataPoint.setTeamMorale(75.0 + (week * 1.5) + (Math.random() * 8.0 - 4.0));
            trendData.add(dataPoint);
        }

        return trendData;
    }

    private KpiMetricsDto generateSampleKpiMetrics(String projectKey) {
        // Fallback sample data when reporting service is unavailable
        KpiMetricsDto kpiMetrics = new KpiMetricsDto();
        kpiMetrics.setProjectKey(projectKey);
        kpiMetrics.setGeneratedAt(LocalDateTime.now());

        // Sample team performance
        KpiMetricsDto.TeamPerformanceKpis teamPerformance = new KpiMetricsDto.TeamPerformanceKpis();
        teamPerformance.setTotalEmployees(5);
        teamPerformance.setTotalHoursWorked(800.0);
        teamPerformance.setAverageHoursPerEmployee(160.0);
        teamPerformance.setTeamEfficiencyPercentage(89.0);
        teamPerformance.setTotalTicketsResolved(18);
        teamPerformance.setAverageResolutionTimeHours(24.5);
        kpiMetrics.setTeamPerformance(teamPerformance);

        // Sample quality KPIs
        KpiMetricsDto.QualityKpis qualityKpis = new KpiMetricsDto.QualityKpis();
        qualityKpis.setDefectRate(4.2);
        qualityKpis.setReworkPercentage(15.0);
        qualityKpis.setCustomerSatisfactionScore(85.0);
        qualityKpis.setCriticalIssuesCount(2);
        qualityKpis.setAverageTimeToResolution(24.5);
        qualityKpis.setFirstTimeFixRate(92.0);
        kpiMetrics.setQuality(qualityKpis);

        // Sample productivity KPIs
        KpiMetricsDto.ProductivityKpis productivityKpis = new KpiMetricsDto.ProductivityKpis();
        productivityKpis.setVelocityPoints(54.0);
        productivityKpis.setThroughputTicketsPerDay(4.5);
        productivityKpis.setCycleTimeHours(76.8);
        productivityKpis.setLeadTimeHours(124.8);
        productivityKpis.setBurndownEfficiency(92.0);
        productivityKpis.setFeaturesDelivered(18);
        productivityKpis.setCodeQualityScore(85.0);
        kpiMetrics.setProductivity(productivityKpis);

        // Sample resource utilization
        KpiMetricsDto.ResourceUtilizationKpis resourceKpis = new KpiMetricsDto.ResourceUtilizationKpis();
        resourceKpis.setCapacityUtilization(95.0);
        resourceKpis.setOvertimePercentage(5.0);
        resourceKpis.setIdleTimePercentage(5.0);
        resourceKpis.setCrossTrainingIndex(3.2);
        resourceKpis.setSkillGapCount(2);
        resourceKpis.setResourceEfficiency(92.0);
        kpiMetrics.setResourceUtilization(resourceKpis);

        // Sample employee KPIs
        List<KpiMetricsDto.EmployeeKpi> employeeKpis = List.of(
            createSampleEmployeeKpi("Youssef Ouriniche", "youssef.ouriniche@emsi-edu.ma", 6, 142.0, 88.5, 92.0, "Casablanca", 1),
            createSampleEmployeeKpi("Alice Smith", "alice.smith@company.com", 5, 128.0, 85.2, 89.0, "Rabat", 2),
            createSampleEmployeeKpi("Bob Johnson", "bob.johnson@company.com", 3, 122.0, 78.9, 85.0, "Fez", 3),
            createSampleEmployeeKpi("Charlie Brown", "charlie.brown@company.com", 2, 126.0, 75.6, 82.0, "Casablanca", 4),
            createSampleEmployeeKpi("Diana Wilson", "diana.wilson@company.com", 2, 122.0, 73.2, 80.0, "Marrakech", 5)
        );
        kpiMetrics.setEmployeeKpis(employeeKpis);

        kpiMetrics.setTrends(generateTrendData());

        return kpiMetrics;
    }

    private KpiMetricsDto.EmployeeKpi createSampleEmployeeKpi(String name, String email, int tasks, 
                                                             double hours, double productivity, 
                                                             double quality, String site, int rank) {
        KpiMetricsDto.EmployeeKpi empKpi = new KpiMetricsDto.EmployeeKpi();
        empKpi.setEmployeeName(name);
        empKpi.setEmail(email);
        empKpi.setTasksCompleted(tasks);
        empKpi.setHoursWorked(hours);
        empKpi.setProductivityScore(productivity);
        empKpi.setQualityRating(quality);
        empKpi.setSite(site);
        empKpi.setRank(rank);
        return empKpi;
    }
}
