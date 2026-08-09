package com.nehakedar.backend.controller;

import com.nehakedar.backend.dto.TransactionResponseDto;
import com.nehakedar.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public Page<TransactionResponseDto> getTransactions(

            // Page number (default = 0)
            @RequestParam(defaultValue = "0") int page,

            // Number of records per page (default = 10)
            @RequestParam(defaultValue = "10") int size,

            // Column name to sort by.
            // Example: id, amount, datasetTime, createdAt
            @RequestParam(defaultValue = "id") String sortBy,

            // Sorting direction.
            // Allowed values: asc or desc
            @RequestParam(defaultValue = "asc") String direction,

            // Optional parameter.
            // If omitted -> returns all transactions.
            // true -> only fraudulent transactions.
            // false -> only genuine transactions.
            @RequestParam(required = false) Boolean fraud
    ) {

        // Delegate the request to the service layer.
        return transactionService.getTransactions(
                page,
                size,
                sortBy,
                direction,
                fraud
        );
    }

    // Endpoint to fetch a single transaction by its ID.
// Example: GET /api/transactions/100
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(

            // Extract the 'id' value from the URL.
            // Example: /api/transactions/100 -> id = 100
            @PathVariable Long id
    ) {

        // Ask the service layer to fetch the transaction.
        TransactionResponseDto transaction =
                transactionService.getTransactionById(id);

        // If no transaction exists with the given ID,
        // return HTTP 404 (Not Found).
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        // If found, return HTTP 200 (OK)
        // along with the transaction DTO.
        return ResponseEntity.ok(transaction);
    }

    // Search transactions whose amount falls
    // between the given minimum and maximum values.
    // Search transactions using optional filters.
    @GetMapping("/search")
    public Page<TransactionResponseDto> searchTransactions(

            @RequestParam(required = false)
            BigDecimal minAmount,

            @RequestParam(required = false)
            BigDecimal maxAmount,

            @RequestParam(required = false)
            Boolean fraud,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String direction
    ) {

        return transactionService.searchTransactions(
                minAmount,
                maxAmount,
                fraud,
                page,
                size,
                sortBy,
                direction
        );
    }
}