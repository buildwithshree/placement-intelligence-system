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
public class ReadinessService {

    private final StudentRepository studentRepository;
    private final JobPostingRepository jobPostingRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final ReadinessScoreRepository readinessScoreRepository;
    private final CompanyRiskProfileRepository riskProfileRepository;
    private final EntityManager entityManager;

    // =================================================================
    // MAIN METHOD: Calculate readiness using PostgreSQL stored procedure
    // This calls the calculate_readiness() function we wrote in SQL
    // =================================================================
    @Transactional
    public Map<String, Object> calculateReadiness(
            Integer studentId, Integer jobId) {

        log.info("Calculating readiness for student {} and job {}", 
                studentId, jobId);

        // Call the PostgreSQL stored procedure directly
        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("calculate_readiness")
            .registerStoredProcedureParameter(
                "p_student_id", Integer.class, ParameterMode.IN)
            .registerStoredProcedureParameter(
                "p_job_id", Integer.class, ParameterMode.IN)
            .setParameter("p_student_id", studentId)
            .setParameter("p_job_id", jobId);

        query.execute();

        // Get result from procedure
        List<?> results = query.getResultList();

        Map<String, Object> response = new HashMap<>();

        if (!results.isEmpty()) {
            Object[] row = (Object[]) results.get(0);
            response.put("finalScore",       row[0]);
            response.put("skillMatch",       row[1]);
            response.put("gpaComponent",     row[2]);
            response.put("projectComponent", row[3]);
            response.put("riskPenalty",      row[4]);
            response.put("missingSkills",    row[5]);
            response.put("recommendation",   row[6]);
        }

        // Add extra context
        response.put("studentId", studentId);
        response.put("jobId",     jobId);

        return response;
    }

    // =================================================================
    // JAVA-LEVEL readiness calculation (backup if procedure fails)
    // This implements the same formula directly in Java
    // Formula: (0.5 x skillMatch) + (0.2 x gpa) + 
    //          (0.2 x project) - (0.1 x companyRisk)
    // =================================================================
    @Transactional
    public Map<String, Object> calculateReadinessJava(
            Integer studentId, Integer jobId) {

        log.info("Java-level readiness calculation for student {} job {}", 
                studentId, jobId);

        // Fetch student
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> 
                new RuntimeException("Student not found: " + studentId));

        // Fetch job
        JobPosting job = jobPostingRepository.findById(jobId)
            .orElseThrow(() -> 
                new RuntimeException("Job not found: " + jobId));

        // Fetch company risk
        BigDecimal companyRisk = riskProfileRepository
            .findByCompanyCompanyId(job.getCompany().getCompanyId())
            .map(CompanyRiskProfile::getRiskIndex)
            .orElse(new BigDecimal("50.0"));

        // Calculate skill match score
        BigDecimal skillMatchScore = calculateSkillMatch(student, job);

        // Calculate GPA weight (normalize 0-10 GPA to 0-100)
        BigDecimal gpaWeight = student.getGpa()
            .divide(new BigDecimal("10.0"), 2, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));

        // Project score already 0-100
        BigDecimal projectScore = student.getProjectScore() != null
            ? student.getProjectScore()
            : BigDecimal.ZERO;

        // READINESS FORMULA:
        // (0.5 x skillMatch) + (0.2 x gpa) + 
        // (0.2 x project) - (0.1 x risk)
        BigDecimal finalReadiness = 
            skillMatchScore.multiply(new BigDecimal("0.5"))
            .add(gpaWeight.multiply(new BigDecimal("0.2")))
            .add(projectScore.multiply(new BigDecimal("0.2")))
            .subtract(companyRisk.multiply(new BigDecimal("0.1")));

        // Clamp between 0 and 100
        finalReadiness = finalReadiness
            .max(BigDecimal.ZERO)
            .min(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);

        // Find missing skills
        String missingSkills = findMissingSkills(student, job);

        // Generate recommendation
        String recommendation = generateRecommendation(
            finalReadiness, companyRisk);

        // Determine readiness level
        String readinessLevel = determineReadinessLevel(finalReadiness);

        // Save to database
        saveReadinessScore(student, job, skillMatchScore, gpaWeight,
            projectScore, companyRisk, finalReadiness,
            missingSkills, recommendation);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("studentId",       studentId);
        response.put("studentName",     student.getFullName());
        response.put("jobId",           jobId);
        response.put("jobTitle",        job.getJobTitle());
        response.put("companyName",     job.getCompany().getCompanyName());
        response.put("finalScore",      finalReadiness);
        response.put("readinessLevel",  readinessLevel);
        response.put("skillMatch",      skillMatchScore);
        response.put("gpaComponent",    gpaWeight);
        response.put("projectComponent",projectScore);
        response.put("riskPenalty",     companyRisk);
        response.put("missingSkills",   missingSkills);
        response.put("recommendation",  recommendation);

        log.info("Readiness calculated: {} for student {} → job {}",
            finalReadiness, studentId, jobId);

        return response;
    }

    // =================================================================
    // Get all readiness scores for a student
    // Returns best matches first
    // =================================================================
    public List<Map<String, Object>> getStudentReadinessReport(
            Integer studentId) {

        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> 
                new RuntimeException("Student not found: " + studentId));

        List<ReadinessScore> scores = readinessScoreRepository
            .findBestMatchesForStudent(studentId);

        List<Map<String, Object>> report = new ArrayList<>();

        for (ReadinessScore score : scores) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("jobTitle",       
                score.getJobPosting().getJobTitle());
            entry.put("companyName",    
                score.getJobPosting().getCompany().getCompanyName());
            entry.put("finalReadiness", score.getFinalReadiness());
            entry.put("readinessLevel", score.getReadinessLevel());
            entry.put("missingSkills",  score.getMissingSkills());
            entry.put("recommendation", score.getRecommendation());
            entry.put("salaryMax",      
                score.getJobPosting().getSalaryMax());
            report.add(entry);
        }

        return report;
    }

    // =================================================================
    // Calculate readiness for student against ALL active jobs
    // =================================================================
    @Transactional
    public List<Map<String, Object>> calculateReadinessForAllJobs(
            Integer studentId) {

        List<JobPosting> activeJobs = 
            jobPostingRepository.findByIsActiveTrue();

        List<Map<String, Object>> results = new ArrayList<>();

        for (JobPosting job : activeJobs) {
            try {
                Map<String, Object> score = 
                    calculateReadinessJava(studentId, job.getJobId());
                results.add(score);
            } catch (Exception e) {
                log.error("Failed readiness for job {}: {}", 
                    job.getJobId(), e.getMessage());
            }
        }

        // Sort by final score descending
        results.sort((a, b) -> {
            BigDecimal scoreA = (BigDecimal) a.get("finalScore");
            BigDecimal scoreB = (BigDecimal) b.get("finalScore");
            return scoreB.compareTo(scoreA);
        });

        return results;
    }

    // =================================================================
    // PRIVATE HELPER: TF-IDF style skill matching
    // Compares student skills against job required skills
    // =================================================================
    private BigDecimal calculateSkillMatch(
            Student student, JobPosting job) {

        // Get student's skills
        List<StudentSkill> studentSkills = studentSkillRepository
            .findByStudentStudentId(student.getStudentId());

        if (studentSkills.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Parse required skills from job description
        String requiredSkillsText = job.getRequiredSkills()
            .toLowerCase();
        String[] requiredArray = requiredSkillsText.split(",");
        int totalRequired = requiredArray.length;

        if (totalRequired == 0) return BigDecimal.ZERO;

        // Count matches with proficiency weighting
        double matchScore = 0.0;

        for (StudentSkill ss : studentSkills) {
            String skillName = ss.getSkill()
                .getSkillName().toLowerCase();

            for (String required : requiredArray) {
                if (required.trim().contains(skillName) ||
                    skillName.contains(required.trim())) {

                    // Weight by proficiency level
                    double weight = switch (ss.getProficiencyLevel()) {
                        case "expert"       -> 1.0;
                        case "advanced"     -> 0.85;
                        case "intermediate" -> 0.65;
                        case "beginner"     -> 0.4;
                        default             -> 0.5;
                    };
                    matchScore += weight;
                    break;
                }
            }
        }

        // Calculate percentage
        double percentage = (matchScore / totalRequired) * 100;
        percentage = Math.min(100.0, percentage);

        return new BigDecimal(percentage)
            .setScale(2, RoundingMode.HALF_UP);
    }

    // =================================================================
    // PRIVATE HELPER: Find missing skills
    // =================================================================
    private String findMissingSkills(Student student, JobPosting job) {

        List<StudentSkill> studentSkills = studentSkillRepository
            .findByStudentStudentId(student.getStudentId());

        Set<String> studentSkillNames = new HashSet<>();
        for (StudentSkill ss : studentSkills) {
            studentSkillNames.add(
                ss.getSkill().getSkillName().toLowerCase());
        }

        String[] requiredSkills = job.getRequiredSkills().split(",");
        List<String> missing = new ArrayList<>();

        for (String required : requiredSkills) {
            String req = required.trim().toLowerCase();
            boolean found = false;
            for (String studentSkill : studentSkillNames) {
                if (studentSkill.contains(req) || 
                    req.contains(studentSkill)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(required.trim());
            }
        }

        return missing.isEmpty() ? null : String.join(", ", missing);
    }

    // =================================================================
    // PRIVATE HELPER: Generate recommendation text
    // =================================================================
    private String generateRecommendation(
            BigDecimal finalReadiness, BigDecimal companyRisk) {

        String riskWarning = "";
        if (companyRisk.compareTo(new BigDecimal("60")) >= 0) {
            riskWarning = " WARNING: This company has HIGH layoff risk (" 
                + companyRisk + "/100). Have backup options ready.";
        }

        if (finalReadiness.compareTo(new BigDecimal("80")) >= 0) {
            return "Highly recommended to apply. Strong match." 
                + riskWarning;
        } else if (finalReadiness.compareTo(new BigDecimal("60")) >= 0) {
            return "Good candidate. Focus on missing skills before applying." 
                + riskWarning;
        } else if (finalReadiness.compareTo(new BigDecimal("40")) >= 0) {
            return "Moderate readiness. Significant skill gaps need attention." 
                + riskWarning;
        } else {
            return "Not ready yet. Upskill significantly before applying." 
                + riskWarning;
        }
    }

    // =================================================================
    // PRIVATE HELPER: Determine readiness level label
    // =================================================================
    private String determineReadinessLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("80")) >= 0) return "excellent";
        if (score.compareTo(new BigDecimal("60")) >= 0) return "good";
        if (score.compareTo(new BigDecimal("40")) >= 0) return "moderate";
        return "low";
    }

    // =================================================================
    // PRIVATE HELPER: Save readiness score to database
    // =================================================================
    @Transactional
    private void saveReadinessScore(
            Student student, JobPosting job,
            BigDecimal skillMatch, BigDecimal gpaWeight,
            BigDecimal projectScore, BigDecimal companyRisk,
            BigDecimal finalReadiness, String missingSkills,
            String recommendation) {

        ReadinessScore score = readinessScoreRepository
            .findByStudentStudentIdAndJobPostingJobId(
                student.getStudentId(), job.getJobId())
            .orElse(new ReadinessScore());

        score.setStudent(student);
        score.setJobPosting(job);
        score.setSkillMatchScore(skillMatch);
        score.setGpaWeight(gpaWeight);
        score.setProjectScore(projectScore);
        score.setCompanyRiskScore(companyRisk);
        score.setFinalReadiness(finalReadiness);
        score.setMissingSkills(missingSkills);
        score.setRecommendation(recommendation);
        score.setReadinessLevel(
            determineReadinessLevel(finalReadiness));

        readinessScoreRepository.save(score);
    }
}