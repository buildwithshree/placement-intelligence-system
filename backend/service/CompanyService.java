package com.pplis.backend.service;

import com.pplis.backend.model.*;
import com.pplis.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyRiskProfileRepository riskProfileRepository;
    private final JobPostingRepository jobPostingRepository;

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Company getCompanyById(Integer id) {
        return companyRepository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Company not found: " + id));
    }

    public List<Company> getActiveRecruiters() {
        return companyRepository.findAllActiveRecruiters();
    }

    public Map<String, Object> getCompanyDetails(Integer companyId) {
        Company company = getCompanyById(companyId);

        CompanyRiskProfile risk = riskProfileRepository
            .findByCompanyCompanyId(companyId)
            .orElse(null);

        List<JobPosting> jobs = jobPostingRepository
            .findByCompanyCompanyId(companyId);

        Map<String, Object> details = new HashMap<>();
        details.put("company",      company);
        details.put("riskProfile",  risk);
        details.put("jobPostings",  jobs);
        details.put("totalJobs",    jobs.size());

        if (risk != null) {
            details.put("riskIndex",    risk.getRiskIndex());
            details.put("riskLevel",    risk.getRiskLevel());
            details.put("hiringTrend",  risk.getHiringTrend());
        }

        return details;
    }
}
