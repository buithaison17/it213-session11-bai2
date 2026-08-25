package com.example.bai2.model;

import com.example.bai2.util.LlmCostCalculator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraceMetadata(
        String department,
        String environment,
        String userId,
        String sessionId,
        String model,
        BigDecimal estimatedCost,
        String formattedCost
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static TraceMetadata of(String department,
                                   String environment,
                                   String userId,
                                   String sessionId,
                                   String model,
                                   long inputTokens,
                                   long outputTokens) {
        BigDecimal cost = LlmCostCalculator.calculateCost(inputTokens, outputTokens, model);
        String formatted = LlmCostCalculator.formatCost(cost);
        return new TraceMetadata(department, environment, userId, sessionId, model, cost, formatted);
    }

    /**
     * Chuyển đổi sang Map để nhúng trực tiếp vào Langfuse metadata.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("department", department);
        map.put("environment", environment);
        map.put("userId", userId);
        map.put("sessionId", sessionId);
        map.put("model", model);
        map.put("estimatedCost", estimatedCost != null ? estimatedCost.toPlainString() : "0.00000000");
        map.put("formattedCost", formattedCost);
        return map;
    }

    /**
     * Chuyển đổi sang JSON String phục vụ truyền tải hoặc ghi log.
     */
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize TraceMetadata to JSON", e);
        }
    }
}