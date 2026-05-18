package com.resumeai.ai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final RestTemplate restTemplate;

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.url}")
    private String apiUrl;

    @Value("${ai.gemini.model}")
    private String model;

    public String call(String prompt) {
        String endpoint = String.format("%s/models/%s:generateContent?key=%s", apiUrl, model, apiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. Build the specific text part for the prompt
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        // 2. Wrap it inside a contents array as required by the API
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        // 3. Assemble the final JSON payload structure
        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        try {
            Map response = restTemplate.postForObject(endpoint, entity, Map.class);
            return extractContent(response);
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map response) {
        if (response == null) return "";
        List<Map> candidates = (List<Map>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "";
        
        Map content = (Map) candidates.get(0).get("content");
        if (content == null) return "";
        
        List<Map> parts = (List<Map>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "";
        
        return (String) parts.get(0).get("text");
    }
}
