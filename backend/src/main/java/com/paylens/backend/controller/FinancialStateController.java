package com.paylens.backend.controller;

import com.paylens.backend.dto.DashboardResponse;
import com.paylens.backend.dto.FinancialStateResponse;
import com.paylens.backend.service.FinancialStateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FinancialStateController {
    private final FinancialStateService financialStateService;

    public FinancialStateController(FinancialStateService financialStateService) {
        this.financialStateService = financialStateService;
    }

    @GetMapping("/financial-state")
    public FinancialStateResponse financialState() {
        return financialStateService.getFinancialState();
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return financialStateService.getDashboard();
    }
}
