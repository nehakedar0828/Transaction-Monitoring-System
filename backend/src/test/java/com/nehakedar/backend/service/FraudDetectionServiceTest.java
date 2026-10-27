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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fraudDetectionService, "mlServiceUrl", "http://127.0.0.1:8000");
    }

    private Transaction createTestTransaction(Long id) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setDatasetTime(100L);
        transaction.setAmount(new BigDecimal("150.00"));
        transaction.setActualFraud(false);
        transaction.setProcessingStatus(ProcessingStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());

        TransactionFeatures features = new TransactionFeatures();
        features.setId(id);
        features.setTransaction(transaction);
        features.setV1(1.0);
        features.setV2(2.0);
        features.setV3(3.0);
        features.setV4(4.0);
        features.setV5(5.0);
        features.setV6(6.0);
        features.setV7(7.0);
        features.setV8(8.0);
        features.setV9(9.0);
        features.setV10(10.0);
        features.setV11(11.0);
        features.setV12(12.0);
        features.setV13(13.0);
        features.setV14(14.0);
        features.setV15(15.0);
        features.setV16(16.0);
        features.setV17(17.0);
        features.setV18(18.0);
        features.setV19(19.0);
        features.setV20(20.0);
        features.setV21(21.0);
        features.setV22(22.0);
        features.setV23(23.0);
        features.setV24(24.0);
        features.setV25(25.0);
        features.setV26(26.0);
        features.setV27(27.0);
        features.setV28(28.0);

        transaction.setTransactionFeatures(features);
        return transaction;
    }

    private void mockRestClientSuccess(FraudPredictionResponse response) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://127.0.0.1:8000/predict")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(FraudPredictionRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(FraudPredictionResponse.class)).thenReturn(response);
    }

    @Test
    @DisplayName("Should successfully process transaction, persist prediction and update status to COMPLETED")
    void shouldSuccessfullyProcessTransactionAndPersistPrediction() {
        Long txId = 1L;
        Transaction transaction = createTestTransaction(txId);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(transaction));
        when(predictionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        FraudPredictionResponse mlResponse = new FraudPredictionResponse(true, 0.98, 0.95);
        mockRestClientSuccess(mlResponse);

        when(predictionRepository.save(any(Prediction.class))).thenAnswer(invocation -> {
            Prediction p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        PredictionResponseDto result = fraudDetectionService.processTransactionById(txId);

        assertNotNull(result);
        assertEquals(txId, result.getTransactionId());
        assertEquals(0.98, result.getRiskScore());
        assertTrue(result.getIsAnomaly());
        assertEquals(Severity.HIGH, result.getSeverity());
        assertEquals(ModelVersion.V1, result.getModelVersion());
        assertEquals(ProcessingStatus.COMPLETED, result.getProcessingStatus());

        // Verify prediction saved
        ArgumentCaptor<Prediction> predictionCaptor = ArgumentCaptor.forClass(Prediction.class);
        verify(predictionRepository).save(predictionCaptor.capture());
        Prediction savedPrediction = predictionCaptor.getValue();
        assertEquals(0.98, savedPrediction.getRiskScore());
        assertTrue(savedPrediction.getIsAnomaly());
        assertEquals(Severity.HIGH, savedPrediction.getSeverity());

        // Verify transaction updated
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(ProcessingStatus.COMPLETED, savedTx.getProcessingStatus());
        assertNotNull(savedTx.getProcessedAt());
    }

    @Test
    @DisplayName("Should return existing prediction and skip inference when already processed")
    void shouldReturnExistingPredictionWhenAlreadyProcessed() {
        Long txId = 2L;
        Transaction transaction = createTestTransaction(txId);
        transaction.setProcessingStatus(ProcessingStatus.COMPLETED);

        Prediction existing = Prediction.builder()
                .id(20L)
                .transaction(transaction)
                .riskScore(0.15)
                .severity(Severity.LOW)
                .isAnomaly(false)
                .modelVersion(ModelVersion.V1)
                .predictedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(transaction));
        when(predictionRepository.findByTransactionId(txId)).thenReturn(Optional.of(existing));

        PredictionResponseDto result = fraudDetectionService.processTransactionById(txId);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals(txId, result.getTransactionId());
        assertEquals(0.15, result.getRiskScore());
        assertFalse(result.getIsAnomaly());
        assertEquals(Severity.LOW, result.getSeverity());

        // Verify no ML call was made
        verifyNoInteractions(restClient);
        verify(predictionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException when transaction does not exist")
    void shouldThrowTransactionNotFoundWhenIdDoesNotExist() {
        Long txId = 999L;
        when(transactionRepository.findById(txId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> fraudDetectionService.processTransactionById(txId));
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("Should update status to FAILED and throw IllegalStateException when features are missing")
    void shouldFailTransactionAndThrowWhenFeaturesMissing() {
        Long txId = 3L;
        Transaction transaction = createTestTransaction(txId);
        transaction.setTransactionFeatures(null); // Missing features

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(transaction));
        when(predictionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> fraudDetectionService.processTransactionById(txId));
        assertTrue(exception.getMessage().contains("Transaction features are missing"));

        // Verify status marked as FAILED
        verify(transactionRepository).save(argThat(tx -> tx.getProcessingStatus() == ProcessingStatus.FAILED));
        verifyNoInteractions(restClient);
        verify(predictionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update status to FAILED and throw InferenceServiceException when ML service call fails")
    void shouldFailTransactionAndThrowWhenInferenceFails() {
        Long txId = 4L;
        Transaction transaction = createTestTransaction(txId);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(transaction));
        when(predictionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://127.0.0.1:8000/predict")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(FraudPredictionRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RuntimeException("Connection refused: 127.0.0.1:8000"));

        InferenceServiceException exception = assertThrows(InferenceServiceException.class,
                () -> fraudDetectionService.processTransactionById(txId));
        assertTrue(exception.getMessage().contains("ML inference service communication failed"));

        // Verify status marked as FAILED
        verify(transactionRepository).save(argThat(tx -> tx.getProcessingStatus() == ProcessingStatus.FAILED));
        verify(predictionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate correct severity categories")
    void shouldCorrectlyDetermineSeverityLevels() {
        assertEquals(Severity.HIGH, fraudDetectionService.determineSeverity(0.90, false));
        assertEquals(Severity.HIGH, fraudDetectionService.determineSeverity(0.40, true));
        assertEquals(Severity.MEDIUM, fraudDetectionService.determineSeverity(0.50, false));
        assertEquals(Severity.LOW, fraudDetectionService.determineSeverity(0.10, false));
        assertEquals(Severity.LOW, fraudDetectionService.determineSeverity(null, null));
    }
}
