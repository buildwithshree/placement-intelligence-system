package com.pplis.backend.service;

import com.pplis.backend.model.*;
import com.pplis.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final CompanyRepository companyRepository;
    private final CompanyRiskProfileRepository riskProfileRepository;
    private final RiskSignalRepository riskSignalRepository;
    private final EntityManager entityManager;

    // =================================================================
    // Get full risk report for a company
    // Calls PostgreSQL stored procedure calculate_layoff_risk()
    // =================================================================
    @Transactional
    public Map<String, Object> getCompanyRiskReport(Integer companyId) {

        log.info("Calculating layoff risk for company {}", companyId);

        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("calculate_layoff_risk")
            .registerStoredProcedureParameter(
                "p_company_id", Integer.class, ParameterMode.IN)
            .setParameter("p_company_id", companyId);

        query.execute();

        List<?> results = query.getResultList();
        Map<String, Object> response = new HashMap<>();

        if (!results.isEmpty()) {
            Object[] row = (Object[]) results.get(0);
            response.put("companyName",     row[0]);
            response.put("riskIndex",       row[1]);
            response.put("riskLevel",       row[2]);
            response.put("stabilityScore",  row[3]);
            response.put("totalSignals",    row[4]);
            response.put("recommendation",  row[5]);
        }

        // Add latest signals
        response.put("recentSignals",
            getRecentSignals(companyId));

        return response;
    }

    // =================================================================
    // Get risk reports for ALL companies
    // Returns sorted safest first
    // =================================================================
    public List<Map<String, Object>> getAllCompanyRiskReports() {

        List<CompanyRiskProfile> profiles = 
            riskProfileRepository.findAllOrderBySafest();

        List<Map<String, Object>> reports = new ArrayList<>();

        for (CompanyRiskProfile profile : profiles) {
            Map<String, Object> report = new HashMap<>();
            report.put("companyId",
                profile.getCompany().getCompanyId());
            report.put("companyName",
                profile.getCompany().getCompanyName());
            report.put("sector",
                profile.getCompany().getSector());
            report.put("riskIndex",
                profile.getRiskIndex());
            report.put("riskLevel",
                profile.getRiskLevel());
            report.put("stabilityScore",
                profile.getStabilityScore());
            report.put("hiringTrend",
                profile.getHiringTrend());
            report.put("automationImpact",
                profile.getAutomationImpact());
            report.put("lastLayoffDate",
                profile.getLastLayoffDate());
            report.put("layoffCount2024",
                profile.getLayoffCount2024());
            report.put("layoffCount2025",
                profile.getLayoffCount2025());
            reports.add(report);
        }

        return reports;
    }

    // =================================================================
    // Get only safe companies for student recommendation
    // =================================================================
    public List<Map<String, Object>> getSafeCompanies() {

        List<CompanyRiskProfile> safeProfiles = 
            riskProfileRepository.findSafeCompanies();

        List<Map<String, Object>> result = new ArrayList<>();

        for (CompanyRiskProfile profile : safeProfiles) {
            Map<String, Object> company = new HashMap<>();
            company.put("companyId",
                profile.getCompany().getCompanyId());
            company.put("companyName",
                profile.getCompany().getCompanyName());
            company.put("sector",
                profile.getCompany().getSector());
            company.put("riskIndex",
                profile.getRiskIndex());
            company.put("stabilityScore",
                profile.getStabilityScore());
            company.put("hiringTrend",
                profile.getHiringTrend());
            result.add(company);
        }

        return result;
    }

    // =================================================================
    // Get high risk companies - warn students
    // =================================================================
    public List<Map<String, Object>> getHighRiskCompanies() {

        List<CompanyRiskProfile> highRisk = 
            riskProfileRepository.findHighRiskCompanies();

        List<Map<String, Object>> result = new ArrayList<>();

        for (CompanyRiskProfile profile : highRisk) {
            Map<String, Object> company = new HashMap<>();
            company.put("companyId",
                profile.getCompany().getCompanyId());
            company.put("companyName",
                profile.getCompany().getCompanyName());
            company.put("riskIndex",
                profile.getRiskIndex());
            company.put("riskLevel",
                profile.getRiskLevel());
            company.put("layoffCount2024",
                profile.getLayoffCount2024());
            company.put("warning",
                "This company has " + profile.getRiskLevel() + 
                " layoff risk. Students should apply with caution.");
            result.add(company);
        }

        return result;
    }

    // =================================================================
    // Add new risk signal for a company
    // Trigger will automatically update risk_index
    // =================================================================
    @Transactional
    public Map<String, Object> addRiskSignal(
            Integer companyId,
            String signalType,
            String headline,
            BigDecimal severityScore,
            String source) {

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() ->
                new RuntimeException("Company not found: " + companyId));

        RiskSignal signal = new RiskSignal();
        signal.setCompany(company);
        signal.setSignalType(signalType);
        signal.setHeadline(headline);
        signal.setSeverityScore(severityScore);
        signal.setSignalSource(source);
        signal.setSignalDate(java.time.LocalDate.now());
        signal.setIsVerified(false);

        riskSignalRepository.save(signal);

        log.info("Risk signal added for company {}. " +
            "Trigger will update risk index.", companyId);

        // Return updated risk profile
        return getCompanyRiskReport(companyId);
    }

    // =================================================================
    // Get risk dashboard summary
    // =================================================================
    public Map<String, Object> getRiskDashboard() {

        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("safeCompanies",
            riskProfileRepository.findSafeCompanies().size());
        dashboard.put("highRiskCompanies",
            riskProfileRepository.findHighRiskCompanies().size());
        dashboard.put("totalSignals",
            riskSignalRepository.count());

        // Get top 3 safest
        List<CompanyRiskProfile> safest = 
            riskProfileRepository.findAllOrderBySafest();
        List<String> top3Sa
