package com.example.bai2.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class LlmCostCalculator {

    public static final String MODEL_GEMINI_2_5_FLASH = "gemini-2.5-flash";
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    public static final int CALCULATION_SCALE = 12;
    public static final int DISPLAY_SCALE = 8;

    // gemini-2.5-flash: Input $0.075 / 1M tokens, Output $0.300 / 1M tokens
    private static final BigDecimal GEMINI_INPUT_RATE_PER_M = new BigDecimal("0.075");
    private static final BigDecimal GEMINI_OUTPUT_RATE_PER_M = new BigDecimal("0.300");

    private LlmCostCalculator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Tính toán tổng chi phí tiêu thụ token theo mô hình LLM với độ chính xác tuyệt đối.
     */
    public static BigDecimal calculateCost(long inputTokens, long outputTokens, String model) {
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("Token count cannot be negative");
        }

        BigDecimal inputRatePerMillion;
        BigDecimal outputRatePerMillion;

        if (MODEL_GEMINI_2_5_FLASH.equalsIgnoreCase(model)) {
            inputRatePerMillion = GEMINI_INPUT_RATE_PER_M;
            outputRatePerMillion = GEMINI_OUTPUT_RATE_PER_M;
        } else {
            throw new IllegalArgumentException("Unsupported LLM model: " + model);
        }

        BigDecimal inputCount = BigDecimal.valueOf(inputTokens);
        BigDecimal outputCount = BigDecimal.valueOf(outputTokens);

        // Input Cost = (inputTokens / 1,000,000) * Rate
        BigDecimal inputCost = inputCount
                .multiply(inputRatePerMillion)
                .divide(ONE_MILLION, CALCULATION_SCALE, RoundingMode.HALF_UP);

        // Output Cost = (outputTokens / 1,000,000) * Rate
        BigDecimal outputCost = outputCount
                .multiply(outputRatePerMillion)
                .divide(ONE_MILLION, CALCULATION_SCALE, RoundingMode.HALF_UP);

        return inputCost.add(outputCost).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Định dạng chi phí thành chuỗi hiển thị dạng $0.00000000 phục vụ ghi log kiểm toán.
     */
    public static String formatCost(BigDecimal cost) {
        if (cost == null) {
            return "$0.00000000";
        }
        BigDecimal scaledCost = cost.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("$0.00000000", symbols);
        return df.format(scaledCost);
    }
}