package com.nehakedar.backend.dto;

import com.nehakedar.backend.constant.ProcessingStatus;
import com.nehakedar.backend.constant.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDto {

    private Long id;

    private Long datasetTime;

    private BigDecimal amount;

    private Boolean actualFraud;

    private LocalDateTime createdAt;

    private ProcessingStatus processingStatus;

    private Double riskScore;

    private Severity severity;

    private Boolean isAnomaly;

    public TransactionResponseDto(Long id, Long datasetTime, BigDecimal amount, Boolean actualFraud, LocalDateTime createdAt) {
        this.id = id;
        this.datasetTime = datasetTime;
        this.amount = amount;
        this.actualFraud = actualFraud;
        this.createdAt = createdAt;
    }

    public TransactionResponseDto(Long id, Long datasetTime, BigDecimal amount, Boolean actualFraud, LocalDateTime createdAt,
                                  ProcessingStatus processingStatus, Double riskScore, Severity severity) {
        this.id = id;
        this.datasetTime = datasetTime;
        this.amount = amount;
        this.actualFraud = actualFraud;
        this.createdAt = createdAt;
        this.processingStatus = processingStatus;
        this.riskScore = riskScore;
        this.severity = severity;
    }
}
