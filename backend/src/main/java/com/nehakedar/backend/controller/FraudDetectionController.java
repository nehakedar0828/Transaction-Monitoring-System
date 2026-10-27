package com.nehakedar.backend.controller;

import com.nehakedar.backend.dto.FraudPredictionRequest;
import com.nehakedar.backend.dto.FraudPredictionResponse;
import com.nehakedar.backend.dto.PredictionResponseDto;
import com.nehakedar.backend.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    // Existing endpoint: raw payload prediction without database persistence
    @PostMapping("/predict")
    public FraudPredictionResponse predict(
            @RequestBody FraudPredictionRequest request
    ) {
        return fraudDetectionService.predict(request);
    }

    // New endpoint: evaluates a persisted transaction by ID, saves prediction, and updates status
    @PostMapping("/predict/{transactionId}")
    public ResponseEntity<PredictionResponseDto> processTransaction(
            @PathVariable Long transactionId
    ) {
        PredictionResponseDto response = fraudDetectionService.processTransactionById(transactionId);
        return ResponseEntity.ok(response);
    }
}