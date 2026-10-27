package com.nehakedar.backend.service;

import com.nehakedar.backend.dto.DashboardAmountSummaryDto;
import com.nehakedar.backend.dto.DashboardSummaryDto;
import com.nehakedar.backend.dto.TransactionResponseDto;
import com.nehakedar.backend.dto.TransactionStatusSummaryDto;
import com.nehakedar.backend.entity.Transaction;
import com.nehakedar.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    // Repository used to fetch transaction statistics.
    private final TransactionRepository transactionRepository;

    // Creates the summary statistics for the dashboard.
    public DashboardSummaryDto getSummary() {

        // Count all transactions in the database.
        long totalTransactions = transactionRepository.count();

        // Count only fraudulent transactions.
        long fraudTransactions =
                transactionRepository.countByActualFraud(true);

        // Genuine transactions = total - fraudulent.
        long genuineTransactions =
                totalTransactions - fraudTransactions;

        // Calculate what percentage of all transactions are fraudulent.
        double fraudPercentage = totalTransactions == 0
                ? 0
                : (fraudTransactions * 100.0) / totalTransactions;

        // Return all calculated statistics as a DTO.
        return new DashboardSummaryDto(
                totalTransactions,
                fraudTransactions,
                genuineTransactions,
                fraudPercentage
        );
    }

    // Creates the amount statistics for the dashboard.
    public DashboardAmountSummaryDto getAmountSummary() {

        // Calculate the total amount of all transactions.
        BigDecimal totalAmount =
                transactionRepository.findTotalAmount();

        // Calculate the amount belonging to fraudulent transactions.
        BigDecimal fraudAmount =
                transactionRepository.findFraudAmount();

        // Calculate the amount belonging to genuine transactions.
        BigDecimal genuineAmount =
                transactionRepository.findGenuineAmount();

        // Return all amount statistics as a DTO.
        return new DashboardAmountSummaryDto(
                totalAmount,
                fraudAmount,
                genuineAmount
        );
    }

    // Fetches the 10 most recent fraudulent transactions.
    public List<TransactionResponseDto> getRecentFrauds() {

        // Fetch the latest fraudulent transactions from the database.
        List<Transaction> transactions =
                transactionRepository
                        .findTop10ByActualFraudTrueOrderByDatasetTimeDesc();

        // Convert each Transaction entity into a response DTO.
        return transactions.stream()
                .map(this::convertToDto)
                .toList();
    }

    // Converts a Transaction entity into the DTO used by the API.
    private TransactionResponseDto convertToDto(Transaction transaction) {

        var prediction = transaction.getPrediction();
        return new TransactionResponseDto(
                transaction.getId(),
                transaction.getDatasetTime(),
                transaction.getAmount(),
                transaction.getActualFraud(),
                transaction.getCreatedAt(),
                transaction.getProcessingStatus(),
                prediction != null ? prediction.getRiskScore() : null,
                prediction != null ? prediction.getSeverity() : null,
                prediction != null ? prediction.getIsAnomaly() : null
        );
    }

    // Creates the transaction processing-status statistics.
    public TransactionStatusSummaryDto getStatusSummary() {

        // Fetch grouped status counts from the database.
        List<Object[]> results =
                transactionRepository.countTransactionsByProcessingStatus();

        // Create a map to store status -> count.
        Map<String, Long> statusCounts = new HashMap<>();

        // Process every database result.
        for (Object[] result : results) {

            // First value is the processing status.
            String status = result[0].toString();

            // Second value is the number of transactions.
            Long count = ((Number) result[1]).longValue();

            // Add the status and count to the map.
            statusCounts.put(status, count);
        }

        // Return the result as a DTO.
        return new TransactionStatusSummaryDto(statusCounts);
    }

}