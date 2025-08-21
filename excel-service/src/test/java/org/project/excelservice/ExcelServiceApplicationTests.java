package org.project.excelservice;

import org.junit.jupiter.api.Test;
import org.project.excelservice.service.CheckpointStore;
import org.project.excelservice.service.ExcelFileWriter;
import org.project.excelservice.service.ExcelSyncService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ExcelServiceApplicationTests {

  @MockBean private ExcelFileWriter excelFileWriter;

  @MockBean private ExcelSyncService excelSyncService;

  @MockBean private CheckpointStore checkpointStore;

  @Test
  void contextLoads() {}
}
