package com.walkouttech.ssms.request.predictive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisionApiRequestDTO {

    private String model;
    private List<MessageDTO> messages;
    @JsonProperty("max_tokens")
    private int maxTokens;
    private double temperature;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageDTO {
        private String role;
        private Object content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentDTO {
        private String type;
        private String text;
        @JsonProperty("image_url")
        private ImageUrlDTO imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageUrlDTO {
        private String url;
    }
}
