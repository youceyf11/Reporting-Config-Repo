package org.project.excelservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.project.excelservice.entity.Issue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExcelFileWriter {

  /** Répertoire cible configurable : application.properties -> excel.report.dir: reports */
  private final Path reportsDir;

  public ExcelFileWriter(@Value("${excel.reports.dir:reports}") String dir) {
    this.reportsDir = Paths.get(dir);
  }

  public void append(String projectKey, List<Issue> rows) {
    try {
      // S'assurer que le dossier existe
      Files.createDirectories(reportsDir);

      Path file = reportsDir.resolve(projectKey + ".xlsx");
      Workbook workbook;

      log.info("Appending issues to file: {}", file.toAbsolutePath());
      log.info("Number of issues to append: {}", rows.size());

      // Ouvrir le workbook existant ou en créer un nouveau
      if (Files.exists(file)) {
        try (InputStream in = Files.newInputStream(file)) {
          workbook = WorkbookFactory.create(in);
        }
      } else {
        workbook = new XSSFWorkbook();
      }

      // Récupérer (ou créer) la première feuille
      Sheet sheet =
          workbook.getNumberOfSheets() > 0
              ? workbook.getSheetAt(0)
              : workbook.createSheet("Issues");

      // Déterminer si la feuille vient d'être créée (aucune ligne)
      boolean isNewSheet = sheet.getPhysicalNumberOfRows() == 0;

      // En-têtes dans l'ordre exact souhaité (conforme à la capture)
      String[] headers = {
        "Key",
        "Summary",
        "Issue Type",
        "Status",
        "Priority",
        "Resolution",
        "Assignee",
        "Reporter",
        "Created",
        "Updated",
        "Resolved",
        "Time Spent (s)",
        "Organization",
        "Classification",
        "Entity",
        "Issue Quality",
        "Medium",
        "TTS Days",
        "Site",
        "Month",
        "Quota/Project"
      };

      // Vérifier si la première ligne (header) existe et possède le bon nombre de colonnes
      boolean headerMissingOrCorrupt =
          sheet.getRow(0) == null || sheet.getRow(0).getLastCellNum() != headers.length;

      // (Re)générer l'en-tête si la feuille est nouvelle OU corrompue
      if (isNewSheet || headerMissingOrCorrupt) {
        Row header = sheet.createRow(0);

        // Style: teal background + white bold text
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
          Cell c = header.createCell(i);
          c.setCellValue(headers[i]);
          c.setCellStyle(headerStyle);
        }

        // Freeze header row
        sheet.createFreezePane(0, 1);
      }

      // Première ligne libre (après header)
      int rowIndex = Math.max(sheet.getLastRowNum(), 0) + 1;

      // Écrire chaque issue, en respectant strictement l'ordre des en-têtes
      for (Issue issue : rows) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(issue.getIssueKey());
        row.createCell(1).setCellValue(issue.getSummary());
        row.createCell(2).setCellValue(issue.getIssueType());
        row.createCell(3).setCellValue(issue.getStatus());
        row.createCell(4).setCellValue(issue.getPriority());
        row.createCell(5).setCellValue(issue.getResolution());
        row.createCell(6).setCellValue(issue.getAssignee());
        row.createCell(7).setCellValue(issue.getReporter());
        row.createCell(8).setCellValue(issue.getCreated());
        row.createCell(9).setCellValue(issue.getUpdated());
        row.createCell(10).setCellValue(issue.getResolved());
        row.createCell(11)
            .setCellValue(issue.getTimeSpentSeconds() != null ? issue.getTimeSpentSeconds() : 0);
        row.createCell(12).setCellValue(issue.getOrganization());
        row.createCell(13).setCellValue(issue.getClassification());
        row.createCell(14).setCellValue(issue.getEntity());
        row.createCell(15).setCellValue(issue.getIssueQuality());
        row.createCell(16).setCellValue(issue.getMedium());
        Double ttsDays = issue.getTtsDays();
        if (ttsDays != null) {
          row.createCell(17).setCellValue(ttsDays);
        } else {
          row.createCell(17).setBlank();
        }
        row.createCell(18).setCellValue(issue.getSite());
        row.createCell(19).setCellValue(issue.getMonth());
        row.createCell(20).setCellValue(issue.getQuotaPerProject());
      }

      // Autosize des colonnes avant l'écriture, tant que le classeur est ouvert
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      // Sauvegarder puis fermer le classeur
      try (OutputStream out =
          Files.newOutputStream(
              file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
        workbook.write(out);
      }

      workbook.close();

      log.info("Appended {} issues to {}", rows.size(), file.toAbsolutePath());

    } catch (IOException e) {
      throw new RuntimeException("Failed to append issues to file " + projectKey, e);
    }
  }
}
