package com.pplis.backend.service;

import com.pplis.backend.model.JobPosting;
import com.pplis.backend.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;

    public List<JobPosting> getAllActiveJobs() {
        return jobPostingRepository.findAllActiveOrderBySalary();
    }

    public JobPosting getJobById(Integer id) {
        return jobPostingRepository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Job not found: " + id));
    }

    public List<JobPosting> getEligibleJobs(
            BigDecimal studentGpa, Integer studentBacklogs) {
        return jobPostingRepository
            .findEligibleJobs(studentGpa, studentBacklogs);
    }

    public List<JobPosting> getJobsByCompany(Integer companyId) {
        return jobPostingRepository
            .findByCompanyCompanyId(companyId);
    }
}
