package com.nehakedar.backend.repository;

import com.nehakedar.backend.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByActualFraud(Boolean actualFraud, Pageable pageable);
}