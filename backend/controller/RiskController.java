package com.pplis.backend.controller;

import com.pplis.backend.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/risk")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    // GET /api/risk/company/{companyId}
    // Full risk report for one company
    @GetMapping("/company/{companyId}")
    public ResponseEntity<Map<String, Object>> getCompanyRisk(
            @PathVariable Integer companyId) {
        try {
            Map<String, Object> report =
                riskService.getCompanyRiskReport(companyId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/risk/all
    // All companies sorted safest first
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllRisks() {
        return ResponseEntity.ok(
            riskService.getAllCompanyRiskReports());
    }

    // GET /api/risk/safe
    // Only safe companies
    @GetMapping("/safe")
    public ResponseEntity<List<Map<String, Object>>> getSafeCompanies() {
        return ResponseEntity.ok(riskService.getSafeCompanies());
    }

    // GET /api/risk/high
    // Only high risk companies
    @GetMapping("/high")
    public ResponseEntity<List<Map<String, Object>>> getHighRisk() {
        return ResponseEntity.ok(
            riskService.getHighRiskCompanies());
    }

    // GET /api/risk/dashboard
    // Summary dashboard data
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(riskService.getRiskDashboard());
    }

    // POST /api/risk/signal/{companyId}
    // Add new risk signal for a company
    @PostMapping("/signal/{companyId}")
    public ResponseEntity<Map<String, Object>> addRiskSignal(
            @PathVariable Integer companyId,
            @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> result = riskService.addRiskSignal(
                companyId,
                (String) body.get("signalType"),
                (String) body.get("headline"),
                new BigDecimal(body.get("severityScore").toString()),
                (String) body.get("source")
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
