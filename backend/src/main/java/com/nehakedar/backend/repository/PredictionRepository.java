package com.nehakedar.backend.repository;

import com.nehakedar.backend.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findByTransactionId(Long transactionId);

    boolean existsByTransactionId(Long transactionId);
}