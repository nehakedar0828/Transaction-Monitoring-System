package com.nehakedar.backend.dto;

import com.nehakedar.backend.constant.ModelVersion;
import com.nehakedar.backend.constant.ProcessingStatus;
import com.nehakedar.backend.constant.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResponseDto {

    private Long id;

    private Long transactionId;

    private Double riskScore;

    private Severity severity;

    private Boolean isAnomaly;

    private ModelVersion modelVersion;

    private LocalDateTime predictedAt;

    private ProcessingStatus processingStatus;
}
