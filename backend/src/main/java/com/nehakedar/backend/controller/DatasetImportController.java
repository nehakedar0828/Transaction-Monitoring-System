package com.nehakedar.backend.controller;

import com.nehakedar.backend.service.DatasetImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DatasetImportController {

    private final DatasetImportService datasetImportService;

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importDataset() {
        Map<String, Object> response = datasetImportService.importDataset();
        return ResponseEntity.ok(response);
    }
}
