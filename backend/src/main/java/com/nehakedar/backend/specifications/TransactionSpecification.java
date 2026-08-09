package com.nehakedar.backend.specifications;

import com.nehakedar.backend.entity.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class TransactionSpecification {

    /**
     * Creates an amount filter depending on which
     * amount parameters were provided.
     */
    public static Specification<Transaction> hasAmountBetween(
            BigDecimal minAmount,
            BigDecimal maxAmount
    ) {

        // No amount filter required.
        if (minAmount == null && maxAmount == null) {
            return null;
        }

        // Both limits provided.
        if (minAmount != null && maxAmount != null) {

            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.between(
                            root.get("amount"),
                            minAmount,
                            maxAmount
                    );
        }

        // Only minimum amount provided.
        if (minAmount != null) {

            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(
                            root.get("amount"),
                            minAmount
                    );
        }

        // Only maximum amount provided.
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.get("amount"),
                        maxAmount
                );
    }

    /**
     * Creates a filter based on whether the transaction
     * is fraudulent or genuine.
     */
    public static Specification<Transaction> hasFraudStatus(Boolean fraud) {

        // If fraud wasn't provided, don't apply this filter.
        if (fraud == null) {
            return null;
        }

        return (root, query, criteriaBuilder) ->

                // Generates:
                // actual_fraud = true/false
                criteriaBuilder.equal(
                        root.get("actualFraud"),
                        fraud
                );
    }
}