package com.pplis.backend.repository;

import com.pplis.backend.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository 
    extends JpaRepository<Application, Integer> {

    List<Application> findByStudentStudentId(Integer studentId);
    List<Application> findByJobPostingJobId(Integer jobId);
    List<Application> findByStatus(String status);

    Optional<Application> findByStudentStudentIdAndJobPostingJobId(
        Integer studentId, Integer jobId
    );

    @Query("SELECT a FROM Application a " +
           "WHERE a.student.studentId = :studentId " +
           "ORDER BY a.appliedAt DESC")
    List<Application> findStudentApplicationHistory(
        @Param("studentId") Integer studentId
    );

    boolean existsByStudentStudentIdAndJobPostingJobId(
        Integer studentId, Integer jobId
    );
}
