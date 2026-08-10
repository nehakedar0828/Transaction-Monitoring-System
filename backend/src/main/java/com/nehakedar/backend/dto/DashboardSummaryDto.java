package com.nehakedar.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryDto {

    // Total number of transactions.
    private long totalTransactions;

    // Number of fraudulent transactions.
    private long fraudTransactions;

    // Number of genuine transactions.
    private long genuineTransactions;

    // Percentage of fraud transactions.
    private double fraudPercentage;
}
