package com.iquenobot.ai.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIResponseDto {

    private String content;
    private String intent;
    private Float confidence;
    private String sentiment;
    private Float sentimentScore;
    private Integer tokensUsed;
    private String model;
    private Map<String, Object> metadata;
}