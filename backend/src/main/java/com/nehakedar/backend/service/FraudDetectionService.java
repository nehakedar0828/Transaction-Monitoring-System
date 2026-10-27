package com.nehakedar.backend.service;

import com.nehakedar.backend.constant.ModelVersion;
import com.nehakedar.backend.constant.ProcessingStatus;
import com.nehakedar.backend.constant.Severity;
import com.nehakedar.backend.dto.FraudPredictionRequest;
import com.nehakedar.backend.dto.FraudPredictionResponse;
import com.nehakedar.backend.dto.PredictionResponseDto;
import com.nehakedar.backend.entity.Prediction;
import com.nehakedar.backend.entity.Transaction;
import com.nehakedar.backend.entity.TransactionFeatures;
import com.nehakedar.backend.exception.InferenceServiceException;
import com.nehakedar.backend.exception.TransactionNotFoundException;
import com.nehakedar.backend.repository.PredictionRepository;
import com.nehakedar.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final RestClient restClient;
    private final TransactionRepository transactionRepository;
    private final PredictionRepository predictionRepository;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    /**
     * Direct raw ML prediction call using RestClient.
     */
    public FraudPredictionResponse predict(FraudPredictionRequest request) {
        try {
            return restClient
                    .post()
                    .uri(mlServiceUrl + "/predict")
                    .body(request)
                    .retrieve()
                    .body(FraudPredictionResponse.class);
        } catch (Exception e) {
            log.error("Failed to connect to ML inference service at {}/predict: {}", mlServiceUrl, e.getMessage(), e);
            throw new InferenceServiceException("ML inference service communication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Direct prediction on an in-memory Transaction entity without database persistence.
     */
    public FraudPredictionResponse predictTransaction(Transaction transaction) {
        TransactionFeatures features = transaction.getTransactionFeatures();
        if (features == null) {
            throw new IllegalStateException("Transaction features are missing for transaction ID: " + transaction.getId());
        }
        FraudPredictionRequest request = buildPredictionRequest(transaction, features);
        return predict(request);
    }

    /**
     * End-to-end processing of a transaction by its ID:
     * 1. Loads transaction and features.
     * 2. Checks for existing prediction to avoid duplicates.
     * 3. Calls external Python ML inference service outside of a database transaction.
     * 4. Persists the Prediction entity and updates the Transaction status to COMPLETED atomically.
     * 5. If inference fails, marks the transaction as FAILED and propagates the exception.
     */
    public PredictionResponseDto processTransactionById(Long transactionId) {
        // 1. Fetch transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction with ID " + transactionId + " not found"));

        // 2. Prevent duplicate prediction
        Optional<Prediction> existingPrediction = predictionRepository.findByTransactionId(transactionId);
        if (existingPrediction.isPresent()) {
            log.info("Transaction ID {} already has a persisted prediction. Returning existing record.", transactionId);
            return mapToDto(existingPrediction.get(), transaction.getProcessingStatus());
        }

        // 3. Verify features exist
        TransactionFeatures features = transaction.getTransactionFeatures();
        if (features == null) {
            log.error("Transaction ID {} has no associated TransactionFeatures.", transactionId);
            markTransactionFailed(transactionId, "Missing transaction features");
            throw new IllegalStateException("Transaction features are missing for transaction ID: " + transactionId);
        }

        // 4. Call Python ML service (outside database transaction to avoid holding DB connections)
        FraudPredictionResponse inferenceResponse;
        try {
            FraudPredictionRequest request = buildPredictionRequest(transaction, features);
            inferenceResponse = predict(request);

            if (inferenceResponse == null || inferenceResponse.getProbability() == null) {
                throw new InferenceServiceException("Inference service returned an empty or invalid response");
            }
        } catch (Exception e) {
            log.error("Inference execution failed for transaction ID {}: {}", transactionId, e.getMessage());
            markTransactionFailed(transactionId, e.getMessage());
            if (e instanceof InferenceServiceException ise) {
                throw ise;
            }
            throw new InferenceServiceException("Inference execution failed for transaction ID " + transactionId + ": " + e.getMessage(), e);
        }

        // 5. Persist prediction and advance status atomically
        return persistPredictionAndUpdateStatus(transactionId, inferenceResponse);
    }

    /**
     * Atomic database update: persists Prediction and updates Transaction status to COMPLETED.
     */
    @Transactional
    public PredictionResponseDto persistPredictionAndUpdateStatus(Long transactionId, FraudPredictionResponse response) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction with ID " + transactionId + " not found"));

        // Concurrency guard against duplicate insertion
        Optional<Prediction> existing = predictionRepository.findByTransactionId(transactionId);
        if (existing.isPresent()) {
            return mapToDto(existing.get(), transaction.getProcessingStatus());
        }

        Severity severity = determineSeverity(response.getProbability(), response.getFraud());

        Prediction prediction = Prediction.builder()
                .transaction(transaction)
                .riskScore(response.getProbability())
                .isAnomaly(Boolean.TRUE.equals(response.getFraud()))
                .severity(severity)
                .modelVersion(ModelVersion.V1)
                .predictedAt(LocalDateTime.now())
                .build();

        Prediction savedPrediction = predictionRepository.save(prediction);

        transaction.setProcessingStatus(ProcessingStatus.COMPLETED);
        transaction.setProcessedAt(LocalDateTime.now());
        transaction.setPrediction(savedPrediction);
        transactionRepository.save(transaction);

        log.info("Persisted prediction for transaction ID {}: riskScore={}, severity={}, isAnomaly={}, status=COMPLETED",
                transactionId, savedPrediction.getRiskScore(), savedPrediction.getSeverity(), savedPrediction.getIsAnomaly());

        return mapToDto(savedPrediction, transaction.getProcessingStatus());
    }

    /**
     * Marks a transaction as FAILED when inference cannot be completed.
     */
    @Transactional
    public void markTransactionFailed(Long transactionId, String reason) {
        try {
            transactionRepository.findById(transactionId).ifPresent(tx -> {
                tx.setProcessingStatus(ProcessingStatus.FAILED);
                tx.setProcessedAt(LocalDateTime.now());
                transactionRepository.save(tx);
                log.warn("Updated transaction ID {} processing status to FAILED. Reason: {}", transactionId, reason);
            });
        } catch (Exception ex) {
            log.error("Failed to update status to FAILED for transaction ID {}: {}", transactionId, ex.getMessage(), ex);
        }
    }

    /**
     * Maps risk probability and anomaly flag to the Severity enum.
     */
    public Severity determineSeverity(Double riskScore, Boolean isAnomaly) {
        if (Boolean.TRUE.equals(isAnomaly) || (riskScore != null && riskScore >= 0.70)) {
            return Severity.HIGH;
        } else if (riskScore != null && riskScore >= 0.30) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }

    private FraudPredictionRequest buildPredictionRequest(Transaction transaction, TransactionFeatures features) {
        FraudPredictionRequest request = new FraudPredictionRequest();
        request.setTime(transaction.getDatasetTime().doubleValue());
        request.setAmount(transaction.getAmount().doubleValue());
        request.setV1(features.getV1());
        request.setV2(features.getV2());
        request.setV3(features.getV3());
        request.setV4(features.getV4());
        request.setV5(features.getV5());
        request.setV6(features.getV6());
        request.setV7(features.getV7());
        request.setV8(features.getV8());
        request.setV9(features.getV9());
        request.setV10(features.getV10());
        request.setV11(features.getV11());
        request.setV12(features.getV12());
        request.setV13(features.getV13());
        request.setV14(features.getV14());
        request.setV15(features.getV15());
        request.setV16(features.getV16());
        request.setV17(features.getV17());
        request.setV18(features.getV18());
        request.setV19(features.getV19());
        request.setV20(features.getV20());
        request.setV21(features.getV21());
        request.setV22(features.getV22());
        request.setV23(features.getV23());
        request.setV24(features.getV24());
        request.setV25(features.getV25());
        request.setV26(features.getV26());
        request.setV27(features.getV27());
        request.setV28(features.getV28());
        return request;
    }

    private PredictionResponseDto mapToDto(Prediction prediction, ProcessingStatus status) {
        return PredictionResponseDto.builder()
                .id(prediction.getId())
                .transactionId(prediction.getTransaction() != null ? prediction.getTransaction().getId() : null)
                .riskScore(prediction.getRiskScore())
                .severity(prediction.getSeverity())
                .isAnomaly(prediction.getIsAnomaly())
                .modelVersion(prediction.getModelVersion())
                .predictedAt(prediction.getPredictedAt())
                .processingStatus(status)
                .build();
    }
}