package org.project.excelservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.excelservice.client.ServiceClient;
import org.project.excelservice.config.RabbitConfig;
import org.project.excelservice.dto.EmailRequestDto;
import org.project.excelservice.dto.ReportingDataDto;
import org.project.excelservice.service.AiAnalysisService; // <--- Import
import org.project.excelservice.service.ExcelGenerationService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExcelScheduler {

    private final ExcelGenerationService excelService;
    private final RabbitTemplate rabbitTemplate;
    private final ServiceClient serviceClient;
    private final AiAnalysisService aiService;

    @Value("${report.recipient:admin@localhost}")
    private String recipientEmail;

    // Run every Friday at 5:00 PM
    @Scheduled(cron = "0 0 17 * * FRI", zone = "Africa/Casablanca")
    public void scheduleWeeklyMetricsReport() {
        log.info("⏰ Triggered scheduled Excel + AI Report...");

        LocalDate today = LocalDate.now();
        LocalDate start = today.with(DayOfWeek.MONDAY);
        LocalDate end = today.with(DayOfWeek.SUNDAY);
        String startStr = start.format(DateTimeFormatter.ISO_DATE);
        String endStr = end.format(DateTimeFormatter.ISO_DATE);

        try {
            // 1. Fetch Data (Needed for both AI and Excel)
            Map<String, ReportingDataDto> reportData = serviceClient.getWeeklyReport(startStr, endStr);

            // 2. Generate AI Summary
            String aiSummary = "";
            if (!reportData.isEmpty()) {
                // Grab the first week available to analyze
                ReportingDataDto weekData = reportData.values().iterator().next();
                aiSummary = aiService.generateExecutiveSummary(weekData);
            }

            // 3. Generate Excel File
            byte[] fileContent = excelService.generateMetricsReport(startStr, endStr);
            String filename = "Weekly_Metrics_" + startStr + ".xlsx";

            // 4. Build Email Body (HTML)
            String emailBody = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Weekly Performance Report</h2>
                    <p><b>Period:</b> %s to %s</p>
                    <hr style="border: 0; border-top: 1px solid #eee;"/>
                    
                    <h3 style="color: #2c3e50;">AI Executive Analysis</h3>
                    <div style="background-color: #f9f9f9; padding: 15px; border-left: 4px solid #007bff; white-space: pre-wrap; font-family: inherit;">%s</div>
                    
                    <hr style="border: 0; border-top: 1px solid #eee;"/>
                    <p>Please find the detailed Excel metrics attached.</p>
                </body>
                </html>
                """, startStr, endStr, aiSummary);

            // 5. Send to Queue
            EmailRequestDto emailRequest = EmailRequestDto.builder()
                    .to(recipientEmail)
                    .subject("Weekly AI Report (" + startStr + ")")
                    .body(emailBody)
                    .attachmentName(filename)
                    .attachmentData(fileContent)
                    .build();

            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, emailRequest);

            log.info("✅ AI Enhanced Report sent to Queue.");

        } catch (Exception e) {
            log.error("❌ Failed to execute scheduled job", e);
        }
    }
}