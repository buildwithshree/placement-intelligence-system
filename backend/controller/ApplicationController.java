package com.pplis.backend.controller;

import com.pplis.backend.model.Application;
import com.pplis.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // POST /api/applications/apply/{studentId}/{jobId}
    @PostMapping("/apply/{studentId}/{jobId}")
    public ResponseEntity<Map<String, Object>> applyToJob(
            @PathVariable Integer studentId,
            @PathVariable Integer jobId) {
        try {
            return ResponseEntity.ok(
                applicationService.applyToJob(studentId, jobId));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/applications/student/{studentId}
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Application>> getStudentApplications(
            @PathVariable Integer studentId) {
        return ResponseEntity.ok(
            applicationService.getStudentApplications(studentId));
    }

    // PUT /api/applications/{applicationId}/status
    @PutMapping("/{applicationId}/status")
    public ResponseEntity<Application> updateStatus(
            @PathVariable Integer applicationId,
            @RequestParam String status) {
        try {
            return ResponseEntity.ok(
                applicationService.updateApplicationStatus(
                    applicationId, status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
