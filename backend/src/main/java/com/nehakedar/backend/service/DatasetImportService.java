package com.nehakedar.backend.service;

import com.nehakedar.backend.entity.Transaction;
import com.nehakedar.backend.entity.TransactionFeatures;
import com.nehakedar.backend.repository.TransactionFeaturesRepository;
import com.nehakedar.backend.repository.TransactionRepository;
import com.opencsv.CSVReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Arrays;

@Service
public class DatasetImportService {

    private final ResourceLoader resourceLoader;
    private final TransactionRepository transactionRepository;
    private final TransactionFeaturesRepository transactionFeaturesRepository;

    public DatasetImportService(ResourceLoader resourceLoader,
                                TransactionRepository transactionRepository,
                                TransactionFeaturesRepository transactionFeaturesRepository) {
        this.resourceLoader = resourceLoader;
        this.transactionRepository = transactionRepository;
        this.transactionFeaturesRepository = transactionFeaturesRepository;
    }

    public void importDataset() {

        try {

            Resource resource =
                    resourceLoader.getResource("classpath:dataset/creditcard.csv");

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(resource.getInputStream()));

            CSVReader csvReader = new CSVReader(reader);

            // Skip header
            csvReader.readNext();

            String[] row;
            int count = 0;

            while ((row = csvReader.readNext()) != null) {

                Transaction transaction = createTransaction(row);
                transactionRepository.save(transaction);

                TransactionFeatures features =
                        createTransactionFeatures(row, transaction);
                transactionFeaturesRepository.save(features);

                count++;

                if (count % 10000 == 0) {
                    System.out.println("Imported " + count + " records");
                }
            }

            System.out.println("Import completed. Total records: " + count);

            csvReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
}