package org.project.reportingservice.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.IssueSimpleDto;
import org.project.reportingservice.dto.MonthlyStatsDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.dto.WeeklyStatsDto;
import org.project.reportingservice.iservice.IJiraClient;
import org.project.reportingservice.iservice.IReportingService;
import org.project.reportingservice.iservice.ITimeUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
public class ReportingService implements IReportingService {

  private final IJiraClient jiraClient;
  private final ITimeUtils timeUtils;

  public ReportingService(IJiraClient jiraClient, ITimeUtils timeUtils) {
    this.jiraClient = jiraClient;
    this.timeUtils = timeUtils;
  }

  @Override
  public Mono<ReportingResultDto> generateMonthlyReport(String projectKey) {
    return jiraClient
        .fetchProjectIssues(projectKey, 100)
        .filter(this::isResolvedInCurrentMonth)
        .filter(issue -> issue.getAssignee() != null && !issue.getAssignee().trim().isEmpty())
        .collectList()
        .flatMap(
            issues -> {
              if (issues.isEmpty()) {
                return Mono.just(
                    new ReportingResultDto(
                        timeUtils.getCurrentMonth(LocalDateTime.now()),
                        timeUtils.getCurrentYear(),
                        projectKey,
                        List.of()));
              }
              return Mono.just(processIssuesWithEnhancedAnalytics(issues, projectKey));
            })
        .defaultIfEmpty(
            new ReportingResultDto(
                timeUtils.getCurrentMonth(LocalDateTime.now()),
                timeUtils.getCurrentYear(),
                projectKey,
                List.of()));
  }

  @Override
  public Mono<List<EmployeePerformanceDto>> getTopActiveEmployees(String projectKey, Integer topN) {
    // Handle zero limit immediately
    if (topN != null && topN == 0) {
      return Mono.just(List.of());
    }

    return generateMonthlyReport(projectKey)
        .map(
            report -> {
              List<EmployeePerformanceDto> rankings =
                  report.getEmployeeRankings() != null ? report.getEmployeeRankings() : List.of();
              return rankings.stream().limit(topN != null ? topN : Integer.MAX_VALUE).toList();
            });
  }

  @Override
  public Mono<Tuple2<Double, Integer>> getMonthlyStatistics(String projectKey) {
    return generateMonthlyReport(projectKey)
        .map(
            report -> {
              List<EmployeePerformanceDto> rankings = report.getEmployeeRankings();

              if (rankings == null || rankings.isEmpty()) {
                return reactor.util.function.Tuples.of(0.0, 0);
              }

              double totalHours =
                  rankings.stream()
                      .mapToDouble(
                          e -> e.getTotalHoursWorked() != null ? e.getTotalHoursWorked() : 0.0)
                      .sum();

              int employeeCount = rankings.size();

              return reactor.util.function.Tuples.of(totalHours, employeeCount);
            });
  }

  @Override
  public Mono<Map<String, Map<Integer, Double>>> getWeeklyStatistics(String projectKey) {
    return jiraClient
        .fetchProjectIssues(projectKey, 100)
        .filter(this::isResolvedInCurrentMonth)
        .filter(issue -> issue.getAssignee() != null && !issue.getAssignee().trim().isEmpty())
        .collectList()
        .map(this::calculateHoursByEmployeeByWeek);
  }

  @Override
  public Mono<Map<String, Map<Integer, Double>>> getDetailedMonthlyStatistics(String projectKey) {
    return jiraClient
        .fetchProjectIssues(projectKey, 100)
        .filter(this::isResolvedInCurrentMonth)
        .filter(issue -> issue.getAssignee() != null && !issue.getAssignee().trim().isEmpty())
        .collectList()
        .map(this::calculateHoursByEmployeeByMonth);
  }

  @Override
  public Mono<WeeklyStatsDto> getEmployeeWeeklyStats(String projectKey, String assignee) {
    return getWeeklyStatistics(projectKey)
        .map(
            weeklyStats -> {
              Map<Integer, Double> employeeWeeklyHours = weeklyStats.get(assignee);
              return new WeeklyStatsDto(assignee, employeeWeeklyHours);
            });
  }

  @Override
  public Mono<MonthlyStatsDto> getEmployeeMonthlyStats(
      String projectKey, String assignee, Double expectedHours) {
    return getDetailedMonthlyStatistics(projectKey)
        .map(
            monthlyStats -> {
              Map<Integer, Double> employeeMonthlyHours = monthlyStats.get(assignee);
              return new MonthlyStatsDto(assignee, employeeMonthlyHours, expectedHours);
            });
  }

  // --- Internal business logic methods ---

  private boolean isResolvedInCurrentMonth(IssueSimpleDto issue) {
    return issue != null && issue.isResolved() && timeUtils.isInCurrentMonth(issue.getResolved());
  }

  /** Traite tous les issues avec une analyse améliorée incluant les calculs par semaine/mois */
  private ReportingResultDto processIssuesWithEnhancedAnalytics(
      List<IssueSimpleDto> allIssues, String projectKey) {
    if (allIssues == null || allIssues.isEmpty()) {
      return new ReportingResultDto(
          timeUtils.getCurrentMonth(LocalDateTime.now()),
          timeUtils.getCurrentYear(),
          projectKey,
          List.of());
    }

    // 1. Calcul par semaine
    Map<String, Map<Integer, Double>> hoursByEmployeeByWeek =
        calculateHoursByEmployeeByWeek(allIssues);

    // 2. Calcul par mois
    Map<String, Map<Integer, Double>> hoursByEmployeeByMonth =
        calculateHoursByEmployeeByMonth(allIssues);

    // 3. Calcul des heures attendues par employé
    Map<String, Double> expectedHoursByEmployee =
        calculateExpectedHoursByEmployee(hoursByEmployeeByMonth);

    // 4. Grouper par employé et calculer les métriques
    Map<String, List<IssueSimpleDto>> issuesByEmployee =
        allIssues.stream().collect(Collectors.groupingBy(IssueSimpleDto::getAssignee));

    List<EmployeePerformanceDto> employeeReports =
        issuesByEmployee.entrySet().stream()
            .map(
                entry ->
                    calculateEnhancedEmployeeMetrics(
                        entry.getKey(),
                        entry.getValue(),
                        hoursByEmployeeByWeek.get(entry.getKey()),
                        hoursByEmployeeByMonth.get(entry.getKey()),
                        expectedHoursByEmployee.get(entry.getKey())))
            .toList();

    return createReportWithRanking(employeeReports, projectKey);
  }

  /** Calcul des heures par employé par semaine */
  private Map<String, Map<Integer, Double>> calculateHoursByEmployeeByWeek(
      List<IssueSimpleDto> issues) {
    Map<String, Map<Integer, Double>> hoursByEmployeeByWeek = new HashMap<>();

    for (IssueSimpleDto issue : issues) {
      if (issue.getAssignee() != null
          && issue.getTimeSpentSeconds() != null
          && issue.getResolved() != null) {
        String assignee = issue.getAssignee();
        int week = issue.getResolved().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        double hours = issue.getTimeSpentSeconds() / 3600.0;

        hoursByEmployeeByWeek
            .computeIfAbsent(assignee, k -> new HashMap<>())
            .merge(week, hours, Double::sum);
      }
    }

    return hoursByEmployeeByWeek;
  }

  /** Calcul des heures par employé par mois */
  private Map<String, Map<Integer, Double>> calculateHoursByEmployeeByMonth(
      List<IssueSimpleDto> issues) {
    Map<String, Map<Integer, Double>> hoursByEmployeeByMonth = new HashMap<>();

    for (IssueSimpleDto issue : issues) {
      if (issue.getAssignee() != null
          && issue.getTimeSpentSeconds() != null
          && issue.getResolved() != null) {
        String assignee = issue.getAssignee();
        int month = issue.getResolved().getMonthValue();
        double hours = issue.getTimeSpentSeconds() / 3600.0;

        hoursByEmployeeByMonth
            .computeIfAbsent(assignee, k -> new HashMap<>())
            .merge(month, hours, Double::sum);
      }
    }

    return hoursByEmployeeByMonth;
  }

  /** Calcul des heures attendues par employé */
  private Map<String, Double> calculateExpectedHoursByEmployee(
      Map<String, Map<Integer, Double>> hoursByEmployeeByMonth) {
    Map<String, Double> expectedHoursByEmployee = new HashMap<>();
    YearMonth currentMonth = YearMonth.now();
    double expectedHours = timeUtils.getExpectedHoursForMonth(currentMonth, 8.0);

    for (String assignee : hoursByEmployeeByMonth.keySet()) {
      expectedHoursByEmployee.put(assignee, expectedHours);
    }

    return expectedHoursByEmployee;
  }

  /** Calcul amélioré des métriques d'employé avec les nouvelles données */
  private EmployeePerformanceDto calculateEnhancedEmployeeMetrics(
      String assignee,
      List<IssueSimpleDto> issues,
      Map<Integer, Double> weeklyHours,
      Map<Integer, Double> monthlyHours,
      Double expectedHours) {

    if (issues == null || issues.isEmpty()) {
      return new EmployeePerformanceDto(
          assignee != null ? assignee : "Unknown",
          0.0,
          0,
          0.0,
          0.0,
          "UNKNOWN",
          expectedHours != null ? expectedHours : 0.0);
    }

    // Calcul du temps total travaillé (en heures)
    double totalHoursWorked =
        issues.stream()
            .mapToDouble(
                issue ->
                    issue != null && issue.getTimeSpentSeconds() != null
                        ? issue.getTimeSpentSeconds() / 3600.0
                        : 0.0)
            .sum();

    // Calcul de la moyenne du temps de résolution
    double averageResolutionTimeHours =
        issues.stream()
            .filter(
                issue -> issue != null && issue.getCreated() != null && issue.getResolved() != null)
            .mapToDouble(
                issue ->
                    timeUtils.calculateHoursDifference(issue.getCreated(), issue.getResolved()))
            .average()
            .orElse(0.0);

    // Nombre d'issues résolues
    Integer resolvedIssuesCount = issues.size();

    // Heures attendues (utiliser la valeur calculée ou la valeur par défaut)
    double employeeExpectedHours =
        expectedHours != null
            ? expectedHours
            : timeUtils.getExpectedHoursForMonth(YearMonth.now(), 8.0);

    // Calcul de la performance
    double performancePercentage =
        timeUtils.calculatePerformancePercentage(totalHoursWorked, employeeExpectedHours);

    // Niveau de performance
    String performanceLevel =
        timeUtils.determinePerformanceLevel(performancePercentage, 0.9, 0.7, 0.5);

    // Créer le DTO avec les métriques améliorées
    EmployeePerformanceDto dto =
        new EmployeePerformanceDto(
            assignee,
            timeUtils.roundToTwoDecimals(totalHoursWorked),
            resolvedIssuesCount,
            timeUtils.roundToTwoDecimals(averageResolutionTimeHours),
            timeUtils.roundToTwoDecimals(performancePercentage),
            performanceLevel,
            employeeExpectedHours);

    // Ajouter les données hebdomadaires et mensuelles si disponibles
    if (weeklyHours != null) {
      // Vous pouvez ajouter ces données au DTO si nécessaire
      System.out.println("Heures hebdomadaires pour " + assignee + ": " + weeklyHours);
    }

    if (monthlyHours != null) {
      // Vous pouvez ajouter ces données au DTO si nécessaire
      System.out.println("Heures mensuelles pour " + assignee + ": " + monthlyHours);
    }

    return dto;
  }

  private ReportingResultDto createReportWithRanking(
      List<EmployeePerformanceDto> employeeReports, String projectKey) {
    if (employeeReports == null || employeeReports.isEmpty()) {
      return new ReportingResultDto(
          timeUtils.getCurrentMonth(LocalDateTime.now()),
          timeUtils.getCurrentYear(),
          projectKey,
          List.of());
    }

    // Tri par heures travaillées (décroissant) pour le classement
    List<EmployeePerformanceDto> sortedReports =
        employeeReports.stream()
            .sorted(
                (e1, e2) ->
                    Double.compare(
                        e2.getTotalHoursWorked() != null ? e2.getTotalHoursWorked() : 0.0,
                        e1.getTotalHoursWorked() != null ? e1.getTotalHoursWorked() : 0.0))
            .toList();

    // Attribution des rangs
    AtomicInteger rank = new AtomicInteger(1);
    sortedReports.forEach(report -> report.setRanking(rank.getAndIncrement()));

    return new ReportingResultDto(
        timeUtils.getCurrentMonth(LocalDateTime.now()),
        timeUtils.getCurrentYear(),
        projectKey,
        sortedReports);
  }
}
