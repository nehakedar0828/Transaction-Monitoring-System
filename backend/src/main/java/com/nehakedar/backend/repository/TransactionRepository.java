package com.nehakedar.backend.repository;

import com.nehakedar.backend.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> , JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findByActualFraud(Boolean actualFraud, Pageable pageable);

    // Finds all transactions whose amount lies between
    // the minimum and maximum values provided.
    // Spring Data JPA automatically generates:
    // SELECT * FROM transactions
    // WHERE amount BETWEEN ? AND ?
    List<Transaction> findByAmountBetween(
            BigDecimal minAmount,
            BigDecimal maxAmount
    );

    // Counts fraudulent or genuine transactions.
    long countByActualFraud(Boolean actualFraud);

    // Calculates the total amount of all transactions.
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t")
    BigDecimal findTotalAmount();

    // Calculates the total amount of fraudulent transactions.
    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM Transaction t
       WHERE t.actualFraud = true
       """)
    BigDecimal findFraudAmount();

    // Calculates the total amount of genuine transactions.
    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM Transaction t
       WHERE t.actualFraud = false
       """)
    BigDecimal findGenuineAmount();

    // Finds fraudulent transactions ordered from newest to oldest.
    List<Transaction> findTop10ByActualFraudTrueOrderByDatasetTimeDesc();

    // Counts transactions grouped by their processing status.
    @Query("""
       SELECT t.processingStatus, COUNT(t)
       FROM Transaction t
       GROUP BY t.processingStatus
       """)
    List<Object[]> countTransactionsByProcessingStatus();
}