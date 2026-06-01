import re

with open("c:/Users/sagac/Downloads/NF_SSMS-API/ssms-predictive-module/src/main/java/com/walkouttech/ssms/serviceImpl/predictive/AiClientServiceImpl.java", "r", encoding="utf-8") as f:
    code = f.read()

# Replace sendPredictionRequest
new_prediction = """
    @Override
    public String sendPredictionRequest(String prompt) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-api-key-here")) {
            log.warn("AI API key is not configured. Returning mock response.");
            return getMockResponse();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            boolean isGemini = apiUrl.contains("generativelanguage.googleapis.com");
            String targetUrl = apiUrl;
            Map<String, Object> requestBody;
            
            if (isGemini) {
                targetUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
                requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "systemInstruction", Map.of("parts", List.of(Map.of("text", "You are an educational data analyst AI. Analyse student data and return predictions in JSON format.")))
                );
            } else {
                headers.setBearerAuth(apiKey);
                requestBody = Map.of(
                        "model", model,
                        "max_tokens", maxTokens,
                        "temperature", temperature,
                        "messages", List.of(
                                Map.of("role", "system", "content", "You are an educational data analyst AI. Analyse student data and return predictions in JSON format."),
                                Map.of("role", "user", "content", prompt)
                        )
                );
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) throw new ApiException("Empty response from AI API", HttpStatus.BAD_GATEWAY);

            if (isGemini) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    return (String) parts.get(0).get("text");
                }
            } else {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return response.getBody().toString();

        } catch (ApiException e) { throw e; } catch (Exception e) {
            log.error("AI API call failed: {}", e.getMessage(), e);
            throw new ApiException("AI API call failed: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }
"""

# Replace sendVisionRequest
new_vision = """
    @Override
    public String sendVisionRequest(String base64Image) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-api-key-here")) {
            log.warn("AI API key is not configured. Returning mock vision response.");
            return getMockVisionResponse();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            boolean isGemini = apiUrl.contains("generativelanguage.googleapis.com");
            String targetUrl = apiUrl;
            Object requestBody;
            
            String sysPrompt = "You are an AI that extracts student report card data from images. Analyze the provided report card image and return a JSON object with: studentName (string), className (string), rollNumber (string), overallPercentage (number), overallGrade (string), subjects (array of {name, score, grade, totalMarks}), attendance (object with totalDays, presentDays, percentage), overallRiskLevel (LOW|MODERATE|HIGH|CRITICAL), confidence (0.0-1.0), performanceSummary (string), recommendations (string). Return ONLY valid JSON.";
            String userPrompt = "Extract the student report card data from this image. Return ONLY valid JSON.";

            if (isGemini) {
                targetUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
                requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(
                        Map.of("text", userPrompt),
                        Map.of("inline_data", Map.of("mime_type", "image/jpeg", "data", base64Image))
                    ))),
                    "systemInstruction", Map.of("parts", List.of(Map.of("text", sysPrompt)))
                );
            } else {
                headers.setBearerAuth(apiKey);
                VisionApiRequestDTO request = new VisionApiRequestDTO();
                request.setModel(model);
                request.setMaxTokens(maxTokens);
                request.setTemperature(temperature);
                VisionApiRequestDTO.MessageDTO systemMessage = new VisionApiRequestDTO.MessageDTO();
                systemMessage.setRole("system");
                systemMessage.setContent(sysPrompt);
                VisionApiRequestDTO.MessageDTO userMessage = new VisionApiRequestDTO.MessageDTO();
                userMessage.setRole("user");
                VisionApiRequestDTO.ContentDTO textContent = new VisionApiRequestDTO.ContentDTO();
                textContent.setType("text");
                textContent.setText(userPrompt);
                VisionApiRequestDTO.ContentDTO imageContent = new VisionApiRequestDTO.ContentDTO();
                imageContent.setType("image_url");
                VisionApiRequestDTO.ImageUrlDTO imageUrl = new VisionApiRequestDTO.ImageUrlDTO();
                imageUrl.setUrl("data:image/jpeg;base64," + base64Image);
                imageContent.setImageUrl(imageUrl);
                userMessage.setContent(List.of(textContent, imageContent));
                request.setMessages(List.of(systemMessage, userMessage));
                requestBody = request;
            }

            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) throw new ApiException("Empty response from AI Vision API", HttpStatus.BAD_GATEWAY);

            if (isGemini) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    return (String) parts.get(0).get("text");
                }
            } else {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return response.getBody().toString();

        } catch (ApiException e) { throw e; } catch (Exception e) {
            log.error("AI Vision API call failed: {}", e.getMessage(), e);
            throw new ApiException("AI Vision API call failed: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }
"""

p1 = re.compile(r'@Override\s+public String sendPredictionRequest\(String prompt\) \{.*?(?=@Override\s+public String sendVisionRequest)', re.DOTALL)
p2 = re.compile(r'@Override\s+public String sendVisionRequest\(String base64Image\) \{.*?(?=\/\*\*|\Z)', re.DOTALL)

code = p1.sub(new_prediction.strip() + "\n\n    ", code)
code = p2.sub(new_vision.strip() + "\n\n    ", code)

with open("c:/Users/sagac/Downloads/NF_SSMS-API/ssms-predictive-module/src/main/java/com/walkouttech/ssms/serviceImpl/predictive/AiClientServiceImpl.java", "w", encoding="utf-8") as f:
    f.write(code)

print("Java file patched successfully.")
