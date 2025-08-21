package org.project.excelservice.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.project.excelservice.dto.ReportingDtos.EmployeePerformanceDto;
import org.project.excelservice.dto.ReportingDtos.ReportingResultDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ExcelReportService {

  private final WebClient reportingClient;

  public ExcelReportService(WebClient reportingWebClient) {
    this.reportingClient = reportingWebClient;
  }

  public ByteArrayResource generateEmployeeReport(String projectKey) {
    Mono<ReportingResultDto> monthlyReportMono =
        reportingClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/api/reporting/monthly")
                        .queryParam("projectKey", projectKey)
                        .build())
            .retrieve()
            .bodyToMono(ReportingResultDto.class);

    Mono<Map<String, Map<Integer, Double>>> weeklyStatsMono =
        reportingClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/api/reporting/weekly/stats")
                        .queryParam("projectKey", projectKey)
                        .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Map<Integer, Double>>>() {});

    return Mono.zip(monthlyReportMono, weeklyStatsMono)
        .map(tuple -> buildWorkbook(tuple.getT1(), tuple.getT2()))
        .map(this::toResource)
        .block();
  }

  private XSSFWorkbook buildWorkbook(
      ReportingResultDto report, Map<String, Map<Integer, Double>> weeklyStats) {

    List<EmployeePerformanceDto> rows =
        Optional.ofNullable(report.getEmployeeRankings()).orElseGet(List::of);

    XSSFWorkbook wb = new XSSFWorkbook();
    CreationHelper helper = wb.getCreationHelper();
    Sheet sheet = wb.createSheet("Performance");

    // Styles
    CellStyle headerStyle = wb.createCellStyle();
    Font headerFont = wb.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);

    CellStyle percentStyle = wb.createCellStyle();
    percentStyle.setDataFormat(helper.createDataFormat().getFormat("0.00%"));

    CellStyle hoursStyle = wb.createCellStyle();
    hoursStyle.setDataFormat(helper.createDataFormat().getFormat("0.00"));

    // Header
    String[] header = {
      "Employee",
      "Week-1 (h)",
      "Week-2 (h)",
      "Week-3 (h)",
      "Week-4 (h)",
      "MonthHours (h)",
      "Efficiency (%)",
      "PerformanceLevel",
      "AvgResolution (h)",
      "Rank",
      "TicketsResolved"
    };
    Row h = sheet.createRow(0);
    for (int i = 0; i < header.length; i++) {
      Cell c = h.createCell(i);
      c.setCellValue(header[i]);
      c.setCellStyle(headerStyle);
    }

    // Determine last 4 ISO week numbers present in data (fallback: current week-3..current)
    List<Integer> last4Weeks = inferLast4Weeks(weeklyStats);

    // Body
    int rIdx = 1;
    for (EmployeePerformanceDto e : rows) {
      Row r = sheet.createRow(rIdx++);

      String assignee = nvl(e.getAssignee(), "Unknown");
      r.createCell(0).setCellValue(assignee);

      Map<Integer, Double> w = weeklyStats.getOrDefault(assignee, Collections.emptyMap());
      for (int i = 0; i < 4; i++) {
        Double val = nvl(w.get(last4Weeks.get(i)), 0.0);
        Cell c = r.createCell(1 + i);
        c.setCellValue(val);
        c.setCellStyle(hoursStyle);
      }

      Cell monthHoursCell = r.createCell(5);
      monthHoursCell.setCellValue(nvl(e.getTotalHoursWorked(), 0.0));
      monthHoursCell.setCellStyle(hoursStyle);

      Cell perfCell = r.createCell(6);
      // Convert 0..100 to 0..1 for Excel percentage style
      perfCell.setCellValue(nvl(e.getPerformancePercentage(), 0.0) / 100.0);
      perfCell.setCellStyle(percentStyle);

      // Performance Level
      r.createCell(7).setCellValue(nvl(e.getPerformanceLevel(), "N/A"));

      Cell avgCell = r.createCell(8);
      avgCell.setCellValue(nvl(e.getAverageResolutionTimeHours(), 0.0));
      avgCell.setCellStyle(hoursStyle);

      r.createCell(9).setCellValue(nvl(e.getRanking(), 0));
      r.createCell(10).setCellValue(nvl(e.getResolvedIssuesCount(), 0));
    }

    IntStream.range(0, header.length).forEach(sheet::autoSizeColumn);
    return wb;
  }

  private List<Integer> inferLast4Weeks(Map<String, Map<Integer, Double>> weeklyStats) {
    // collect all week numbers, sort desc, take 4
    Set<Integer> weeks = new HashSet<>();
    weeklyStats.values().forEach(m -> weeks.addAll(m.keySet()));
    List<Integer> sorted = new ArrayList<>(weeks);
    sorted.sort(Comparator.reverseOrder());

    // fallback if none
    if (sorted.isEmpty()) {
      // simple fallback: 1,2,3,4
      return List.of(1, 2, 3, 4);
    }
    // ensure size 4 with padding
    List<Integer> top = sorted.subList(0, Math.min(4, sorted.size()));
    while (top.size() < 4) top.add(0, top.get(0)); // duplicate oldest if less than 4
    return top;
  }

  private ByteArrayResource toResource(XSSFWorkbook wb) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      wb.write(out);
      return new ByteArrayResource(out.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("Excel generation failed", e);
    }
  }

  private static <T> T nvl(T v, T def) {
    return v != null ? v : def;
  }
}
