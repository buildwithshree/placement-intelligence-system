package com.pplis.backend.repository;

import com.pplis.backend.model.ReadinessScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadinessScoreRepository 
    extends JpaRepository<ReadinessScore, Integer> {

    Optional<ReadinessScore> findByStudentStudentIdAndJobPostingJobId(
        Integer studentId, Integer jobId
    );

    List<ReadinessScore> findByStudentStudentId(Integer studentId);

    @Query("SELECT rs FROM ReadinessScore rs " +
           "WHERE rs.student.studentId = :studentId " +
           "ORDER BY rs.finalReadiness DESC")
    List<ReadinessScore> findBestMatchesForStudent(
        @Param("studentId") Integer studentId
    );

    @Query("SELECT rs FROM ReadinessScore rs " +
           "WHERE rs.jobPosting.jobId = :jobId " +
           "ORDER BY rs.finalReadiness DESC")
    List<ReadinessScore> findTopCandidatesForJob(
        @Param("jobId") Integer jobId
    );

    @Query("SELECT rs FROM ReadinessScore rs " +
           "WHERE rs.readinessLevel = 'excellent' " +
           "ORDER BY rs.finalReadiness DESC")
    List<ReadinessScore> findExcellentCandidates();
}
