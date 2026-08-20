package com.nehakedar.backend.controller;

import com.nehakedar.backend.dto.FraudPredictionRequest;
import com.nehakedar.backend.dto.FraudPredictionResponse;
import com.nehakedar.backend.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    @PostMapping("/predict")
    public FraudPredictionResponse predict(
            @RequestBody FraudPredictionRequest request
    ) {

        return fraudDetectionService.predict(request);
    }
}