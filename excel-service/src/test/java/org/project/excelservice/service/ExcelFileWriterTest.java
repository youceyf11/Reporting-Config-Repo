package org.project.excelservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.excelservice.entity.Issue;

class ExcelFileWriterTest {

  Path tempDir;
  ExcelFileWriter writer;

  @BeforeEach
  void setUp() throws Exception {
    tempDir = Files.createTempDirectory("reports");
    writer = new ExcelFileWriter(tempDir.toString());
  }

  @AfterEach
  void tearDown() throws Exception {
    Files.walk(tempDir).sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
  }

  @Test
  @DisplayName("append() creates workbook with header row when file absent")
  void append_createsWorkbook() throws Exception {
    Issue issue = Issue.builder().issueKey("KEY-1").build();
    writer.append("PROJ", List.of(issue));

    Path file = tempDir.resolve("PROJ.xlsx");
    assertThat(Files.exists(file)).isTrue();

    try (var in = Files.newInputStream(file);
        var wb = WorkbookFactory.create(in)) {
      var sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Key");
      assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
    }
  }
}
