package com.nehakedar.backend.controller;

import com.nehakedar.backend.dto.TransactionResponseDto;
import com.nehakedar.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}