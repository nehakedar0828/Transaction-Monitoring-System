package com.nehakedar.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto {

    private Long id;

    private Long datasetTime;

    private BigDecimal amount;

    private Boolean actualFraud;

    private LocalDateTime createdAt;
}
