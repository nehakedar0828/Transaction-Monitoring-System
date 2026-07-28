package com.nehakedar.backend.entity;

import com.nehakedar.backend.constant.ProcessingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Time value from the dataset
    @Column(nullable = false)
    private Long datasetTime;

    //Transaction amount
    @Column(nullable = false,precision = 12,scale = 2)
    private BigDecimal amount;

    //fraud or not from dataset
    @Column(nullable = false)
    private Boolean actualFraud;

    //processing status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus processingStatus;

    //when record was created
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.processingStatus = ProcessingStatus.PENDING;
    }

    @OneToOne(mappedBy = "transaction",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private TransactionFeatures transactionFeatures;

    @OneToOne(mappedBy = "transaction",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private Prediction prediction;
}
