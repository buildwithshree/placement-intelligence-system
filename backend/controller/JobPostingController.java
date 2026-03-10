package com.pplis.backend.controller;

import com.pplis.backend.model.JobPosting;
import com.pplis.backend.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;

    // GET /api/jobs
    @GetMapping
    public ResponseEntity<List<JobPosting>> getAllJobs() {
        return ResponseEntity.ok(
            jobPostingService.getAllActiveJobs());
    }

    // GET /api/jobs/{id}
    @GetMapping("/{id}")
    public ResponseEntity<JobPosting> getJobById(
            @PathVariable Integer id) {
        try {
            return ResponseEntity.ok(
                jobPostingService.getJobById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/jobs/eligible?gpa=7.5&backlogs=0
    @GetMapping("/eligible")
    public ResponseEntity<List<JobPosting>> getEligibleJobs(
            @RequestParam BigDecimal gpa,
            @RequestParam Integer backlogs) {
        return ResponseEntity.ok(
            jobPostingService.getEligibleJobs(gpa, backlogs));
    }

    // GET /api/jobs/company/{companyId}
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<JobPosting>> getJobsByCompany(
            @PathVariable Integer companyId) {
        return ResponseEntity.ok(
            jobPostingService.getJobsByCompany(companyId));
    }
}
