package com.walkouttech.ssms.service.predictive;

/**
 * Abstraction for communicating with an external AI API.
 * Default implementation uses OpenAI-compatible endpoints.
 * Replace the implementation to switch AI providers (Gemini, Claude, etc.).
 */
public interface AiClientService {

    /**
     * Sends a structured prompt to the AI and returns the raw response body.
     *
     * @param prompt the fully constructed prompt text
     * @return raw JSON response string from the AI provider
     */
    String sendPredictionRequest(String prompt);

    /**
     * Sends a Base64-encoded image to the AI Vision API (gpt-4o or similar)
     * and returns extracted report card data as a JSON string.
     *
     * @param base64Image Base64-encoded image string (without data URI prefix)
     * @return raw JSON response string from the AI provider
     */
    String sendVisionRequest(String base64Image);

    /**
     * Sends a Base64-encoded exam answer sheet image to the AI Vision API.
     * The AI will read handwritten answers and provide a detailed conceptual
     * analysis including strengths, gaps, and handwriting quality assessment.
     *
     * @param base64Image Base64-encoded image string (without data URI prefix)
     * @return raw JSON response string with exam analysis data
     */
    String sendExamPaperVisionRequest(String base64Image);
}
