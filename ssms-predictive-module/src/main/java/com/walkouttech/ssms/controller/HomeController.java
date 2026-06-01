package com.walkouttech.ssms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "SSMS Predictive Analysis Standalone API",
                "status", "RUNNING",
                "frontend", "http://localhost:5173",
                "charts", "http://localhost:9091/api/predictive/charts/risk-distribution",
                "imageAnalysis", "POST http://localhost:9091/api/predictive/analyze-image"
        );
    }
}
