package com.pplis.backend.repository;

import com.pplis.backend.model.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface JobPostingRepository 
    extends JpaRepository<JobPosting, Integer> {

    List<JobPosting> findByCompanyCompanyId(Integer companyId);
    List<JobPosting> findByIsActiveTrue();
    List<JobPosting> findByJobType(String jobType);

    @Query("SELECT jp FROM JobPosting jp " +
           "WHERE jp.isActive = true " +
           "AND jp.requiredGpa <= :studentGpa " +
           "AND jp.maxBacklogs >= :studentBacklogs " +
           "ORDER BY jp.salaryMax DESC")
    List<JobPosting> findEligibleJobs(
        @Param("studentGpa") BigDecimal studentGpa,
        @Param("studentBacklogs") Integer studentBacklogs
    );

    @Query("SELECT jp FROM JobPosting jp " +
           "WHERE jp.isActive = true " +
           "ORDER BY jp.salaryMax DESC")
    List<JobPosting> findAllActiveOrderBySalary();
}