package com.nehakedar.backend.service;

import com.nehakedar.backend.entity.Transaction;
import com.nehakedar.backend.entity.TransactionFeatures;
import com.nehakedar.backend.repository.TransactionFeaturesRepository;
import com.nehakedar.backend.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetImportServiceTest {

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionFeaturesRepository transactionFeaturesRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private Resource mockResource;

    @InjectMocks
    private DatasetImportService datasetImportService;

    @Test
    @DisplayName("Should skip import when database already contains populated transaction records")
    void shouldSkipImportWhenRecordsAlreadyExist() {
        // Arrange
        when(transactionRepository.count()).thenReturn(284807L);
        when(transactionFeaturesRepository.count()).thenReturn(284807L);

        // Act
        Map<String, Object> result = datasetImportService.importDataset();

        // Assert
        assertEquals("SKIPPED", result.get("status"));
        assertEquals("Dataset import skipped because records already exist in the database.", result.get("message"));
        assertEquals(284807L, result.get("existingRecords"));
        assertEquals(0, result.get("importedRecords"));

        // Verify CSV resource was never accessed
        verifyNoInteractions(resourceLoader);

        // Verify no save operations were performed
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(transactionRepository, never()).saveAll(anyList());
        verify(transactionFeaturesRepository, never()).save(any(TransactionFeatures.class));
        verify(transactionFeaturesRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should detect inconsistent state when transaction count and feature count mismatch")
    void shouldDetectInconsistentStateWhenCountsMismatch() {
        // Arrange: 1000 transactions but only 500 features (aborted previous import)
        when(transactionRepository.count()).thenReturn(1000L);
        when(transactionFeaturesRepository.count()).thenReturn(500L);

        // Act
        Map<String, Object> result = datasetImportService.importDataset();

        // Assert
        assertEquals("INCONSISTENT_STATE", result.get("status"));
        assertTrue(((String) result.get("message")).contains("inconsistent database state"));
        assertEquals(1000L, result.get("existingTransactions"));
        assertEquals(500L, result.get("existingFeatures"));
        assertEquals(0, result.get("importedRecords"));

        verifyNoInteractions(resourceLoader);
        verify(transactionRepository, never()).saveAll(anyList());
        verify(transactionFeaturesRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should detect inconsistent state when orphaned features exist with zero transactions")
    void shouldDetectInconsistentStateWhenOnlyFeaturesExist() {
        // Arrange
        when(transactionRepository.count()).thenReturn(0L);
        when(transactionFeaturesRepository.count()).thenReturn(350L);

        // Act
        Map<String, Object> result = datasetImportService.importDataset();

        // Assert
        assertEquals("INCONSISTENT_STATE", result.get("status"));
        assertTrue(((String) result.get("message")).contains("orphaned transaction_features"));
        assertEquals(0L, result.get("existingTransactions"));
        assertEquals(350L, result.get("existingFeatures"));

        verifyNoInteractions(resourceLoader);
    }

    @Test
    @DisplayName("Should import records in chunks when database is empty")
    void shouldImportRecordsInChunksWhenDatabaseIsEmpty() throws Exception {
        // Arrange: Database has 0 records
        when(transactionRepository.count()).thenReturn(0L);
        when(transactionFeaturesRepository.count()).thenReturn(0L);

        // CSV Header + 2 rows
        String csvContent = "Time,V1,V2,V3,V4,V5,V6,V7,V8,V9,V10,V11,V12,V13,V14,V15,V16,V17,V18,V19,V20,V21,V22,V23,V24,V25,V26,V27,V28,Amount,Class\n"
                + "0,-1.3598,-0.0727,2.5363,1.3781,-0.3383,0.4623,0.2395,0.0986,0.3637,0.0907,-0.5515,-0.6178,-0.9913,-0.3111,1.4681,-0.4704,0.2079,0.0257,0.4039,0.2514,-0.0183,0.2778,-0.1104,0.0669,0.1285,-0.1891,0.1335,-0.0210,149.62,0\n"
                + "1,1.1918,0.2661,0.1664,0.4481,0.0600,-0.0823,-0.0788,0.0851,-0.2554,-0.1669,1.6127,1.0652,0.4890,-0.1437,0.6355,0.4639,-0.1148,-0.1833,-0.1457,-0.0690,-0.2257,-0.6386,0.1012,-0.3398,0.1671,0.1258,-0.0089,0.0147,2.69,0\n";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        when(mockResource.getInputStream()).thenReturn(inputStream);
        when(resourceLoader.getResource("classpath:dataset/creditcard.csv")).thenReturn(mockResource);

        // Mock repository saves: assigning simulated ID to transactions
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Transaction> transactions = invocation.getArgument(0);
            long idCounter = 1L;
            for (Transaction t : transactions) {
                t.setId(idCounter++);
            }
            return transactions;
        });

        // Act
        Map<String, Object> result = datasetImportService.importDataset();

        // Assert
        assertEquals("SUCCESS", result.get("status"));
        assertEquals("Dataset imported successfully.", result.get("message"));
        assertEquals(2, result.get("importedRecords"));
        assertEquals(1, result.get("totalChunks"));

        // Verify batch saveAll was called for transactions and features
        verify(transactionRepository).saveAll(anyList());
        verify(transactionFeaturesRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should propagate exception and not report success when save fails")
    void shouldPropagateExceptionWhenSaveFails() throws Exception {
        // Arrange
        when(transactionRepository.count()).thenReturn(0L);
        when(transactionFeaturesRepository.count()).thenReturn(0L);

        String csvContent = "Time,V1,V2,V3,V4,V5,V6,V7,V8,V9,V10,V11,V12,V13,V14,V15,V16,V17,V18,V19,V20,V21,V22,V23,V24,V25,V26,V27,V28,Amount,Class\n"
                + "0,-1.3598,-0.0727,2.5363,1.3781,-0.3383,0.4623,0.2395,0.0986,0.3637,0.0907,-0.5515,-0.6178,-0.9913,-0.3111,1.4681,-0.4704,0.2079,0.0257,0.4039,0.2514,-0.0183,0.2778,-0.1104,0.0669,0.1285,-0.1891,0.1335,-0.0210,149.62,0\n";

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        when(mockResource.getInputStream()).thenReturn(inputStream);
        when(resourceLoader.getResource("classpath:dataset/creditcard.csv")).thenReturn(mockResource);

        when(transactionRepository.saveAll(anyList())).thenThrow(new RuntimeException("Database disk full"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> datasetImportService.importDataset());
        assertTrue(exception.getMessage().contains("Dataset import failed"));
    }
}
