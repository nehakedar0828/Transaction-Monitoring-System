package com.nehakedar.backend.controller;

import com.nehakedar.backend.dto.DashboardAmountSummaryDto;
import com.nehakedar.backend.dto.DashboardSummaryDto;
import com.nehakedar.backend.dto.TransactionResponseDto;
import com.nehakedar.backend.dto.TransactionStatusSummaryDto;
import com.nehakedar.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    // Service responsible for dashboard calculations.
    private final DashboardService dashboardService;

    // Returns overall transaction statistics.
    @GetMapping("/summary")
    public DashboardSummaryDto getSummary() {

        // Delegate calculation to the service layer.
        return dashboardService.getSummary();
    }

    // Returns total, fraudulent and genuine transaction amounts.
    @GetMapping("/amount-summary")
    public DashboardAmountSummaryDto getAmountSummary() {

        // Delegate amount calculations to the service layer.
        return dashboardService.getAmountSummary();
    }

    // Returns the 10 most recent fraudulent transactions.
    @GetMapping("/recent-frauds")
    public List<TransactionResponseDto> getRecentFrauds() {

        // Delegate the request to the dashboard service.
        return dashboardService.getRecentFrauds();
    }

    // Returns the number of transactions for each processing status.
    @GetMapping("/status")
    public TransactionStatusSummaryDto getStatusSummary() {

        // Delegate status calculation to the service layer.
        return dashboardService.getStatusSummary();
    }
}