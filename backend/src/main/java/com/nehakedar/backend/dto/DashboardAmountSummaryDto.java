package com.nehakedar.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardAmountSummaryDto {

    // Total amount across all transactions.
    private BigDecimal totalAmount;

    // Total amount involved in fraudulent transactions.
    private BigDecimal fraudAmount;

    // Total amount involved in genuine transactions.
    private BigDecimal genuineAmount;
}