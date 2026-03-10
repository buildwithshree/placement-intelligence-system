package com.pplis.backend.controller;

import com.pplis.backend.model.Company;
import com.pplis.backend.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // GET /api/companies
    @GetMapping
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    // GET /api/companies/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCompanyDetails(
            @PathVariable Integer id) {
        try {
            return ResponseEntity.ok(
                companyService.getCompanyDetails(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/companies/active
    @GetMapping("/active")
    public ResponseEntity<List<Company>> getActiveRecruiters() {
        return ResponseEntity.ok(
            companyService.getActiveRecruiters());
    }
}
