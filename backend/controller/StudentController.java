package com.pplis.backend.controller;

import com.pplis.backend.model.Student;
import com.pplis.backend.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // GET /api/students
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    // GET /api/students/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getStudentProfile(
            @PathVariable Integer id) {
        try {
            return ResponseEntity.ok(
                studentService.getStudentProfile(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/students
    @PostMapping
    public ResponseEntity<Student> createStudent(
            @RequestBody Student student) {
        try {
            return ResponseEntity.ok(
                studentService.createStudent(student));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/students/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(
            @PathVariable Integer id,
            @RequestBody Student student) {
        try {
            return ResponseEntity.ok(
                studentService.updateStudent(id, student));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // GET /api/students/unplaced
    @GetMapping("/unplaced")
    public ResponseEntity<List<Student>> getUnplacedStudents() {
        return ResponseEntity.ok(studentService.getUnplacedStudents());
    }

    // GET /api/students/eligible?minGpa=7.0&maxBacklogs=0
    @GetMapping("/eligible")
    public ResponseEntity<List<Student>> getEligibleStudents(
            @RequestParam BigDecimal minGpa,
            @RequestParam Integer maxBacklogs) {
        return ResponseEntity.ok(
            studentService.getEligibleStudents(minGpa, maxBacklogs));
    }

    // POST /api/students/{studentId}/skills/{skillId}
    @PostMapping("/{studentId}/skills/{skillId}")
    public ResponseEntity<Map<String, Object>> addSkill(
            @PathVariable Integer studentId,
            @PathVariable Integer skillId,
            @RequestParam String proficiencyLevel) {
        try {
            return ResponseEntity.ok(
                studentService.addSkillToStudent(
                    studentId, skillId, proficiencyLevel));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
