package com.nehakedar.backend.controller;

import com.nehakedar.backend.service.DatasetImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DatasetImportController {

    private final DatasetImportService datasetImportService;

    @PostMapping("/import")
    public String importDataset(){

        datasetImportService.importDataset();

        return "Dataset imported successfully";
    }
}
