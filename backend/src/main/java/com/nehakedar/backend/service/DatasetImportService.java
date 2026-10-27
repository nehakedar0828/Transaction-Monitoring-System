package com.nehakedar.backend.service;

import com.nehakedar.backend.entity.Transaction;
import com.nehakedar.backend.entity.TransactionFeatures;
import com.nehakedar.backend.repository.TransactionFeaturesRepository;
import com.nehakedar.backend.repository.TransactionRepository;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DatasetImportService {

    public static final int CHUNK_SIZE = 1000;

    private final ResourceLoader resourceLoader;
    private final TransactionRepository transactionRepository;
    private final TransactionFeaturesRepository transactionFeaturesRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public DatasetImportService(ResourceLoader resourceLoader,
                                TransactionRepository transactionRepository,
                                TransactionFeaturesRepository transactionFeaturesRepository,
                                PlatformTransactionManager transactionManager) {
        this.resourceLoader = resourceLoader;
        this.transactionRepository = transactionRepository;
        this.transactionFeaturesRepository = transactionFeaturesRepository;
        if (transactionManager != null) {
            this.transactionTemplate = new TransactionTemplate(transactionManager);
            this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        } else {
            this.transactionTemplate = null;
        }
    }

    public DatasetImportService(ResourceLoader resourceLoader,
                                TransactionRepository transactionRepository,
                                TransactionFeaturesRepository transactionFeaturesRepository) {
        this(resourceLoader, transactionRepository, transactionFeaturesRepository, null);
    }

    public Map<String, Object> importDataset() {
        // Step 1: Check existing dataset state and verify consistency
        long existingTransactions = transactionRepository.count();
        long existingFeatures = transactionFeaturesRepository.count();

        // Safe guard against orphaned features with zero parent transactions
        if (existingTransactions == 0 && existingFeatures > 0) {
            log.error("Dataset import blocked: database contains 0 transactions but {} orphaned transaction_features.",
                    existingFeatures);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "INCONSISTENT_STATE");
            result.put("message", "Dataset import skipped: database contains orphaned transaction_features ("
                    + existingFeatures + " records) with 0 transactions. Manual database cleanup required.");
            result.put("existingTransactions", 0L);
            result.put("existingFeatures", existingFeatures);
            result.put("importedRecords", 0);
            return result;
        }

        // Safe guard if database is already populated
        if (existingTransactions > 0) {
            if (existingFeatures > 0 && existingFeatures != existingTransactions) {
                log.warn("Dataset import skipped: inconsistent existing records detected ({} transactions vs {} features).",
                        existingTransactions, existingFeatures);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "INCONSISTENT_STATE");
                result.put("message", "Dataset import skipped: inconsistent database state detected ("
                        + existingTransactions + " transactions vs " + existingFeatures + " features).");
                result.put("existingTransactions", existingTransactions);
                result.put("existingFeatures", existingFeatures);
                result.put("importedRecords", 0);
                return result;
            }

            log.warn("Dataset import skipped: database already contains {} records.", existingTransactions);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "SKIPPED");
            result.put("message", "Dataset import skipped because records already exist in the database.");
            result.put("existingRecords", existingTransactions);
            result.put("importedRecords", 0);
            return result;
        }

        // Step 2: Stream and ingest CSV in transactional chunks
        Resource resource = resourceLoader.getResource("classpath:dataset/creditcard.csv");
        int totalImported = 0;
        int chunkNumber = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
             CSVReader csvReader = new CSVReader(reader)) {

            // Skip CSV header
            String[] header = csvReader.readNext();
            if (header == null) {
                log.warn("Dataset CSV is empty.");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "SUCCESS");
                result.put("message", "Dataset CSV is empty. 0 records imported.");
                result.put("importedRecords", 0);
                return result;
            }

            List<IngestionPair> currentChunk = new ArrayList<>(CHUNK_SIZE);
            String[] row;

            while ((row = csvReader.readNext()) != null) {
                Transaction transaction = createTransaction(row);
                TransactionFeatures features = createTransactionFeatures(row, transaction);
                currentChunk.add(new IngestionPair(transaction, features));

                if (currentChunk.size() >= CHUNK_SIZE) {
                    chunkNumber++;
                    saveChunkWithTransaction(currentChunk);
                    totalImported += currentChunk.size();
                    currentChunk.clear();

                    if (totalImported % 10000 == 0) {
                        log.info("Imported {} records so far ({} chunks)...", totalImported, chunkNumber);
                    }
                }
            }

            // Ingest any remaining records in the last chunk
            if (!currentChunk.isEmpty()) {
                chunkNumber++;
                saveChunkWithTransaction(currentChunk);
                totalImported += currentChunk.size();
                currentChunk.clear();
            }

            log.info("Dataset import completed successfully. Total records imported: {} across {} chunks.",
                    totalImported, chunkNumber);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "SUCCESS");
            result.put("message", "Dataset imported successfully.");
            result.put("importedRecords", totalImported);
            result.put("totalChunks", chunkNumber);
            return result;

        } catch (Exception e) {
            log.error("Dataset import failed after persisting {} records. Error: {}", totalImported, e.getMessage(), e);
            throw new RuntimeException("Dataset import failed after persisting " + totalImported + " records: " + e.getMessage(), e);
        }
    }

    private void saveChunkWithTransaction(List<IngestionPair> chunk) {
        if (transactionTemplate != null) {
            transactionTemplate.executeWithoutResult(status -> saveChunk(chunk));
        } else {
            saveChunk(chunk);
        }
    }

    private void saveChunk(List<IngestionPair> chunk) {
        // 1. Extract and save parent transactions.
        // With GenerationType.IDENTITY, Hibernate executes the SQL INSERT immediately
        // for each Transaction, retrieving and populating its generated database ID.
        List<Transaction> transactions = new ArrayList<>(chunk.size());
        for (IngestionPair pair : chunk) {
            transactions.add(pair.transaction());
        }
        List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);

        // 2. Link each saved parent transaction (with its generated ID) to its corresponding feature record.
        List<TransactionFeatures> featuresList = new ArrayList<>(chunk.size());
        for (int i = 0; i < chunk.size(); i++) {
            TransactionFeatures features = chunk.get(i).features();
            features.setTransaction(savedTransactions.get(i));
            featuresList.add(features);
        }

        // 3. Save all corresponding feature records.
        // If an exception occurs here, the enclosing chunk transaction marks rollback and
        // both parent transactions and features for this chunk are rolled back atomically.
        transactionFeaturesRepository.saveAll(featuresList);
    }

    private Transaction createTransaction(String[] row) {
        Transaction transaction = new Transaction();
        transaction.setDatasetTime((long) Double.parseDouble(row[0]));
        transaction.setAmount(new BigDecimal(row[29]));
        transaction.setActualFraud(Integer.parseInt(row[30]) == 1);
        return transaction;
    }

    private TransactionFeatures createTransactionFeatures(String[] row,
                                                          Transaction transaction) {
        TransactionFeatures features = new TransactionFeatures();
        features.setTransaction(transaction);
        features.setV1(Double.parseDouble(row[1]));
        features.setV2(Double.parseDouble(row[2]));
        features.setV3(Double.parseDouble(row[3]));
        features.setV4(Double.parseDouble(row[4]));
        features.setV5(Double.parseDouble(row[5]));
        features.setV6(Double.parseDouble(row[6]));
        features.setV7(Double.parseDouble(row[7]));
        features.setV8(Double.parseDouble(row[8]));
        features.setV9(Double.parseDouble(row[9]));
        features.setV10(Double.parseDouble(row[10]));
        features.setV11(Double.parseDouble(row[11]));
        features.setV12(Double.parseDouble(row[12]));
        features.setV13(Double.parseDouble(row[13]));
        features.setV14(Double.parseDouble(row[14]));
        features.setV15(Double.parseDouble(row[15]));
        features.setV16(Double.parseDouble(row[16]));
        features.setV17(Double.parseDouble(row[17]));
        features.setV18(Double.parseDouble(row[18]));
        features.setV19(Double.parseDouble(row[19]));
        features.setV20(Double.parseDouble(row[20]));
        features.setV21(Double.parseDouble(row[21]));
        features.setV22(Double.parseDouble(row[22]));
        features.setV23(Double.parseDouble(row[23]));
        features.setV24(Double.parseDouble(row[24]));
        features.setV25(Double.parseDouble(row[25]));
        features.setV26(Double.parseDouble(row[26]));
        features.setV27(Double.parseDouble(row[27]));
        features.setV28(Double.parseDouble(row[28]));
        return features;
    }

    private record IngestionPair(Transaction transaction, TransactionFeatures features) {}
}