package com.example.bai2;

import com.example.bai2.model.TraceMetadata;
import com.example.bai2.util.LlmCostCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LlmCostCalculatorTest {
    @Test
    @DisplayName("Test Case 1: input=15000, output=1200 cho gemini-2.5-flash")
    void testCalculateCost_Case1() {
        long inputTokens = 15_000L;
        long outputTokens = 1_200L;
        String model = LlmCostCalculator.MODEL_GEMINI_2_5_FLASH;

        // Input Cost: 15,000 * 0.075 / 1,000,000 = 0.00112500
        // Output Cost: 1,200 * 0.300 / 1,000,000 = 0.00036000
        // Total Cost: 0.00148500
        BigDecimal expectedCost = new BigDecimal("0.00148500");

        BigDecimal actualCost = LlmCostCalculator.calculateCost(inputTokens, outputTokens, model);
        String formattedCost = LlmCostCalculator.formatCost(actualCost);

        assertEquals(expectedCost, actualCost);
        assertEquals("$0.00148500", formattedCost);
    }

    @Test
    @DisplayName("Test Case 2: input=250000, output=45000 cho gemini-2.5-flash")
    void testCalculateCost_Case2() {
        long inputTokens = 250_000L;
        long outputTokens = 45_000L;
        String model = LlmCostCalculator.MODEL_GEMINI_2_5_FLASH;

        // Input Cost: 250,000 * 0.075 / 1,000,000 = 0.01875000
        // Output Cost: 45,000 * 0.300 / 1,000,000 = 0.01350000
        // Total Cost: 0.03225000
        BigDecimal expectedCost = new BigDecimal("0.03225000");

        BigDecimal actualCost = LlmCostCalculator.calculateCost(inputTokens, outputTokens, model);
        String formattedCost = LlmCostCalculator.formatCost(actualCost);

        assertEquals(expectedCost, actualCost);
        assertEquals("$0.03225000", formattedCost);
    }

    @Test
    @DisplayName("Kiểm tra serialize TraceMetadata sang Map và JSON String")
    void testTraceMetadataSerialization() {
        TraceMetadata metadata = TraceMetadata.of(
                "Risk-Management",
                "prod",
                "usr_finance_99",
                "sess_8831920",
                LlmCostCalculator.MODEL_GEMINI_2_5_FLASH,
                15_000L,
                1_200L
        );

        Map<String, Object> map = metadata.toMap();
        assertEquals("Risk-Management", map.get("department"));
        assertEquals("prod", map.get("environment"));
        assertEquals("0.00148500", map.get("estimatedCost"));
        assertEquals("$0.00148500", map.get("formattedCost"));

        String json = metadata.toJson();
        assertTrue(json.contains("\"department\":\"Risk-Management\""));
        assertTrue(json.contains("\"formattedCost\":\"$0.00148500\""));
    }
}
