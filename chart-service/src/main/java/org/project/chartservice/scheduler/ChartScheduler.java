package org.project.chartservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.chartservice.client.ReportingClient;
import org.project.chartservice.config.RabbitConfig;
import org.project.chartservice.dto.EmailRequestDto;
import org.project.chartservice.dto.WeeklySummaryDto;
import org.project.chartservice.service.ChartGenerationService;
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
public class ChartScheduler {

    private final ReportingClient reportingClient;
    private final ChartGenerationService chartGenService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${report.recipient:admin@localhost}") // Default if env var missing
    private String recipientEmail;

    // CRON: Second Minute Hour DayOfMonth Month DayOfWeek(Fri)
    // Zone: Africa/Casablanca
    @Scheduled(cron = "0 0 17 * * FRI", zone = "Africa/Casablanca")
    public void scheduleWeeklyChart() {
        log.info("⏰ Triggered scheduled Weekly Chart generation for {}", recipientEmail);

        // 1. Calculate Dynamic Dates (Current Week: Mon -> Fri)
        LocalDate today = LocalDate.now();
        LocalDate start = today.with(DayOfWeek.MONDAY);
        LocalDate end = today.with(DayOfWeek.SUNDAY);

        String startDateStr = start.format(DateTimeFormatter.ISO_DATE);
        String endDateStr = end.format(DateTimeFormatter.ISO_DATE);

        try {
            // 2. Fetch Data
            Map<String, WeeklySummaryDto> data = reportingClient.getWeeklyBreakdown(startDateStr, endDateStr);

            // 3. Generate Image
            byte[] chartImage = chartGenService.generateWeeklyTrendChart(data);

            // 4. Create Email DTO
            EmailRequestDto emailRequest = EmailRequestDto.builder()
                    .to(recipientEmail)
                    .subject("Weekly Automated Trend Chart (" + startDateStr + " - " + endDateStr + ")")
                    .body("Here is the automated weekly velocity trend for the team.")
                    .attachmentName("weekly_trend_" + startDateStr + ".png")
                    .attachmentData(chartImage)
                    .build();

            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, emailRequest);

            log.info("✅ Scheduled Chart sent to Queue.");

        } catch (Exception e) {
            log.error("❌ Failed to execute scheduled chart job", e);
        }
    }
}