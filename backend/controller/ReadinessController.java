package com.pplis.backend.controller;

import com.pplis.backend.service.ReadinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/readiness")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReadinessController {

    private final ReadinessService readinessService;

    // GET /api/readiness/calculate/{studentId}/{jobId}
    // Calculate readiness score for one student-job pair
    @GetMapping("/calculate/{studentId}/{jobId}")
    public ResponseEntity<Map<String, Object>> calculateReadiness(
            @PathVariable Integer studentId,
            @PathVariable Integer jobId) {
        try {
            Map<String, Object> result =
                readinessService.calculateReadinessJava(
                    studentId, jobId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Readiness calculation failed: {}",
                e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/readiness/student/{studentId}
    // Get full readiness report for a student
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Map<String, Object>>> getStudentReport(
            @PathVariable Integer studentId) {
        try {
            List<Map<String, Object>> report =
                readinessService.getStudentReadinessReport(studentId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // GET /api/readiness/all-jobs/{studentId}
    // Calculate readiness for student against ALL active jobs
    @GetMapping("/all-jobs/{studentId}")
    public ResponseEntity<List<Map<String, Object>>> calculateForAllJobs(
            @PathVariable Integer studentId) {
        try {
            List<Map<String, Object>> results =
                readinessService.calculateReadinessForAllJobs(studentId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
