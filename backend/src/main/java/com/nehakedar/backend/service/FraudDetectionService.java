package com.nehakedar.backend.service;

import com.nehakedar.backend.dto.FraudPredictionRequest;
import com.nehakedar.backend.dto.FraudPredictionResponse;
import com.nehakedar.backend.entity.Transaction;
import com.nehakedar.backend.entity.TransactionFeatures;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final RestClient restClient;

    @Value("${ml.service.url}")
    private String mlServiceUrl;


    public FraudPredictionResponse predict(
            FraudPredictionRequest request
    ) {

        return restClient
                .post()
                .uri(mlServiceUrl + "/predict")
                .body(request)
                .retrieve()
                .body(FraudPredictionResponse.class);
    }


    public FraudPredictionResponse predictTransaction(
            Transaction transaction
    ) {

        TransactionFeatures features =
                transaction.getTransactionFeatures();

        if (features == null) {
            throw new IllegalStateException(
                    "Transaction features are missing"
            );
        }


        FraudPredictionRequest request =
                new FraudPredictionRequest();

        request.setTime(
                transaction.getDatasetTime().doubleValue()
        );

        request.setAmount(
                transaction.getAmount().doubleValue()
        );


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


        return predict(request);
    }
}