package com.nehakedar.backend.repository;

import com.nehakedar.backend.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}