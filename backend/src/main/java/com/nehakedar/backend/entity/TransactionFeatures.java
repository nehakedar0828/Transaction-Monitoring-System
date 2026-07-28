package com.nehakedar.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFeatures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Column(nullable = false)
    private Double v1;

    @Column(nullable = false)
    private Double v2;

    @Column(nullable = false)
    private Double v3;

    @Column(nullable = false)
    private Double v4;

    @Column(nullable = false)
    private Double v5;

    @Column(nullable = false)
    private Double v6;

    @Column(nullable = false)
    private Double v7;

    @Column(nullable = false)
    private Double v8;

    @Column(nullable = false)
    private Double v9;

    @Column(nullable = false)
    private Double v10;

    @Column(nullable = false)
    private Double v11;

    @Column(nullable = false)
    private Double v12;

    @Column(nullable = false)
    private Double v13;

    @Column(nullable = false)
    private Double v14;

    @Column(nullable = false)
    private Double v15;

    @Column(nullable = false)
    private Double v16;

    @Column(nullable = false)
    private Double v17;

    @Column(nullable = false)
    private Double v18;

    @Column(nullable = false)
    private Double v19;

    @Column(nullable = false)
    private Double v20;

    @Column(nullable = false)
    private Double v21;

    @Column(nullable = false)
    private Double v22;

    @Column(nullable = false)
    private Double v23;

    @Column(nullable = false)
    private Double v24;

    @Column(nullable = false)
    private Double v25;

    @Column(nullable = false)
    private Double v26;

    @Column(nullable = false)
    private Double v27;

    @Column(nullable = false)
    private Double v28;
}
