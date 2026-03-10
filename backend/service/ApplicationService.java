package com.pplis.backend.service;

import com.pplis.backend.model.*;
import com.pplis.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CompanyRiskProfileRepository riskProfileRepository;

    @Transactional
    public Map<String, Object> applyToJob(
            Integer studentId, Integer jobId) {

        // Check if already applied
        if (applicationRepository
                .existsByStudentStudentIdAndJobPostingJobId(
                    studentId, jobId)) {
            throw new RuntimeException("Already applied to this job");
        }

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() ->
                new RuntimeException("Student not found"));

        JobPosting job = jobPostingRepository.findById(jobId)
            .orElseThrow(() ->
                new RuntimeException("Job not found"));

        // Check eligibility
        if (student.getGpa().compareTo(job.getRequiredGpa()) < 0) {
            throw new RuntimeException(
                "GPA below requirement: " + job.getRequiredGpa());
        }

        if (student.getBacklogs() > job.getMaxBacklogs()) {
            throw new RuntimeException(
                "Backlogs exceed limit: " + job.getMaxBacklogs());
        }

        // Check company risk and add warning
        String riskWarning = null;
        Optional<CompanyRiskProfile> riskProfile =
            riskProfileRepository.findByCompanyCompanyId(
                job.getCompany().getCompanyId());

        if (riskProfile.isPresent()) {
            String riskLevel = riskProfile.get().getRiskLevel();
            if (riskLevel.equals("high") ||
                riskLevel.equals("critical")) {
                riskWarning = "WARNING: " +
                    job.getCompany().getCompanyName() +
                    " has " + riskLevel.toUpperCase() +
                    " layoff risk. Proceed with caution.";
            }
        }

        Application application = new Application();
        application.setStudent(student);
        application.setJobPosting(job);
        application.setStatus("applied");
        applicationRepository.save(application);

        Map<String, Object> response = new HashMap<>();
        response.put("message",     "Application submitted successfully");
        response.put("studentName", student.getFullName());
        response.put("jobTitle",    job.getJobTitle());
        response.put("company",     job.getCompany().getCompanyName());
        response.put("status",      "applied");
        if (riskWarning != null) {
            response.put("riskWarning", riskWarning);
        }
        return response;
    }

    public List<Application> getStudentApplications(Integer studentId) {
        return applicationRepository
            .findStudentApplicationHistory(studentId);
    }

    @Transactional
    public Application updateApplicationStatus(
            Integer applicationId, String newStatus) {

        Application app = applicationRepository.findById(applicationId)
            .orElseThrow(() ->
                new RuntimeException("Application not found"));

        app.setStatus(newStatus);

        if (newStatus.equals("selected")) {
            app.getStudent().setIsPlaced(true);
            studentRepository.save(app.getStudent());
        }

        return applicationRepository.save(app);
    }
}
