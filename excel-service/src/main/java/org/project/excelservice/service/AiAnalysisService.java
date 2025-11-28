package org.project.excelservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.excelservice.dto.ReportingDataDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiAnalysisService {

    private final ChatClient.Builder chatClientBuilder;

    public String generateExecutiveSummary(ReportingDataDto data) {
        log.info("🧠 Asking AI (Groq) to analyze the weekly report...");

        // 1. Construct the Prompt
        // We convert the data object to a string so the AI can read it.
        String prompt = """
                You are a Senior Agile Project Manager.
                Analyze the following weekly performance data for my software team.
                
                DATA:
                %s
                
                INSTRUCTIONS:
                1. Write a short "Executive Summary".
                2. Identify the "MVP" (Top Performer).
                3. Highlight one risk.
                
                FORMATTING RULES:
                - Do NOT use HTML tags (no <h3>, no <b>).
                - Do NOT use Markdown (no **bold**, no # headers).
                - Use standard bullet points (-) and newlines for structure.
                - Keep the tone professional and concise.
                """.formatted(data.toString());

        try {
            // 2. Call Groq (via OpenAI Client)
            String response = chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("✅ AI Analysis complete.");
            return response;

        } catch (Exception e) {
            log.error("❌ AI Generation failed: {}", e.getMessage());
            return "<p><em>AI Analysis unavailable at this time.</em></p>";
        }
    }
}