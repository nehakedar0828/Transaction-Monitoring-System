package com.nehakedar.backend.service;

import com.nehakedar.backend.dto.TransactionResponseDto;
import com.nehakedar.backend.entity.Transaction;
import com.nehakedar.backend.exception.TransactionNotFoundException;
import com.nehakedar.backend.repository.TransactionRepository;
import com.nehakedar.backend.specifications.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
        return transactions.map(this::mapToDto);
    }

    // Fetch a single transaction by its database ID.
    public TransactionResponseDto getTransactionById(Long id) {

        // Search for the transaction in the database.
        // JpaRepository returns an Optional because the ID may not exist.
        Optional<Transaction> optionalTransaction =
                transactionRepository.findById(id);

        // If the transaction doesn't exist, return null.
        // We'll replace this with a custom exception in the next phase.
        // Throw a custom exception when the transaction doesn't exist.
        if (optionalTransaction.isEmpty()) {

            throw new TransactionNotFoundException(
                    "Transaction with ID " + id + " not found"
            );
        }

        // Extract the Transaction object from the Optional.
        Transaction transaction = optionalTransaction.get();

        // Convert the entity into a DTO before returning it.
        return mapToDto(transaction);
    }

    // Returns all transactions whose amount is
    // between the given minimum and maximum values.
    // Search transactions using dynamic filters.
    public Page<TransactionResponseDto> searchTransactions(
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Boolean fraud,
            int page,
            int size,
            String sortBy,
            String direction
    ) {

        // Start with an empty specification.
        Specification<Transaction> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();

        // Decide sorting direction.
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        // Create pagination information.
        Pageable pageable = PageRequest.of(
                page,
                size,
                sort
        );

        // Add amount filter if provided.
        if (minAmount != null || maxAmount != null) {

            specification = specification.and(
                    TransactionSpecification.hasAmountBetween(
                            minAmount,
                            maxAmount
                    )
            );
        }

        // Add fraud filter if provided.
        if (fraud != null) {

            specification = specification.and(
                    TransactionSpecification.hasFraudStatus(
                            fraud
                    )
            );
        }

        // Execute query.
        Page<Transaction> transactions =
                transactionRepository.findAll(
                        specification,
                        pageable
                );

        // Convert to DTO.
        return transactions.map(this::mapToDto);
    }

    private TransactionResponseDto mapToDto(Transaction transaction) {

        // Convert a Transaction entity into a DTO.
        // The frontend should receive DTOs instead of database entities.
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
}