package com.pplis.backend.repository;

import com.pplis.backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository 
    extends JpaRepository<Student, Integer> {

    Optional<Student> findByEmail(String email);
    List<Student> findByGraduationYear(Integer year);
    List<Student> findByIsPlacedFalse();
    List<Student> findByIsPlacedTrue();
    List<Student> findByDepartmentDepartmentId(Integer departmentId);

    @Query("SELECT s FROM Student s WHERE s.gpa >= :minGpa AND s.backlogs <= :maxBacklogs")
    List<Student> findEligibleStudents(
        @Param("minGpa") BigDecimal minGpa,
        @Param("maxBacklogs") Integer maxBacklogs
    );

    @Query("SELECT s FROM Student s WHERE s.isPlaced = false ORDER BY s.gpa DESC")
    List<Student> findUnplacedStudentsOrderByGpa();

    @Query("SELECT COUNT(s) FROM Student s WHERE s.isPlaced = true")
    Long countPlacedStudents();
}
