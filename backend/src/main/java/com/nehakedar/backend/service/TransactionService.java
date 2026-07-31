package com.nehakedar.backend.service;

import com.nehakedar.backend.dto.TransactionResponseDto;
import com.nehakedar.backend.entity.Transaction;
import com.nehakedar.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

//    Simpler method
//    public Page<TransactionResponseDto> getTransactions(int page, int size) {
//
//        Pageable pageable = PageRequest.of(page, size);
//
//        return transactionRepository
//                .findAll(pageable)
//                .map(this::convertToDto);
//    }

    // Method to fetch transactions with pagination and sorting.
    // Fetch transactions with pagination, sorting and optional fraud filtering.
    public Page<TransactionResponseDto> getTransactions(
            int page,
            int size,
            String sortBy,
            String direction,
            Boolean fraud) {

        // Decide whether sorting should be ascending or descending.
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        // Create pagination + sorting information.
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Transaction> transactions;

        // If the client did not provide the fraud parameter,
        // return every transaction.
        if (fraud == null) {

            transactions = transactionRepository.findAll(pageable);

        } else {

            // Otherwise return only fraud or only non-fraud transactions.
            transactions = transactionRepository.findByActualFraud(
                    fraud,
                    pageable
            );
        }

        // Convert every Transaction entity into DTO.
        return transactions.map(this::convertToDto);
    }

    private TransactionResponseDto convertToDto(Transaction transaction) {

        return new TransactionResponseDto(
                transaction.getId(),
                transaction.getDatasetTime(),
                transaction.getAmount(),
                transaction.getActualFraud(),
                transaction.getCreatedAt()
        );
    }
}