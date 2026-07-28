package com.nehakedar.backend.entity;

import com.nehakedar.backend.constant.ModelVersion;
import com.nehakedar.backend.constant.Severity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //prediction to one transaction
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    //risk score generated
    @Column(nullable = false)
    private Double riskScore;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;


    @Column(nullable = false)
    private Boolean isAnomaly;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModelVersion modelVersion;


    @Column(nullable = false)
    private LocalDateTime predictedAt;

    @PrePersist
    public void onCreate() {
        this.predictedAt = LocalDateTime.now();
    }
}