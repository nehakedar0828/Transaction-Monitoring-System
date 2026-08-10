package com.nehakedar.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionStatusSummaryDto {

    // Stores the number of transactions for each processing status.
    private Map<String, Long> statusCounts;
}