package com.nehakedar.backend.repository;

import com.nehakedar.backend.entity.TransactionFeatures;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionFeaturesRepository extends JpaRepository<TransactionFeatures, Long> {
}