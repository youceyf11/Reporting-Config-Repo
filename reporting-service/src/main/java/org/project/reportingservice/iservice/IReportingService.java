package org.project.reportingservice.iservice;

import java.util.List;
import java.util.Map;
import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.MonthlyStatsDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.dto.WeeklyStatsDto;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface IReportingService {
  Mono<ReportingResultDto> generateMonthlyReport(String projectKey);

  Mono<List<EmployeePerformanceDto>> getTopActiveEmployees(String projectKey, Integer topN);

  Mono<Tuple2<Double, Integer>> getMonthlyStatistics(String projectKey);

  /**
   * Obtient les statistiques hebdomadaires pour tous les employés d'un projet
   *
   * @param projectKey clé du projet
   * @return Map avec assignee -> (semaine -> heures)
   */
  Mono<Map<String, Map<Integer, Double>>> getWeeklyStatistics(String projectKey);

  /**
   * Obtient les statistiques mensuelles détaillées pour tous les employés d'un projet
   *
   * @param projectKey clé du projet
   * @return Map avec assignee -> (mois -> heures)
   */
  Mono<Map<String, Map<Integer, Double>>> getDetailedMonthlyStatistics(String projectKey);

  /**
   * Obtient les statistiques hebdomadaires formatées pour un employé spécifique
   *
   * @param projectKey clé du projet
   * @param assignee nom de l'employé
   * @return statistiques hebdomadaires de l'employé
   */
  Mono<WeeklyStatsDto> getEmployeeWeeklyStats(String projectKey, String assignee);

  /**
   * Obtient les statistiques mensuelles formatées pour un employé spécifique
   *
   * @param projectKey clé du projet
   * @param assignee nom de l'employé
   * @param expectedHours heures attendues pour la comparaison
   * @return statistiques mensuelles de l'employé
   */
  Mono<MonthlyStatsDto> getEmployeeMonthlyStats(
      String projectKey, String assignee, Double expectedHours);
}
