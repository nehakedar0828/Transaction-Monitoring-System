package com.nehakedar.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudPredictionResponse {

    private Boolean fraud;

    private Double probability;

    private Double threshold;
}