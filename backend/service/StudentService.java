package com.pplis.backend.service;

import com.pplis.backend.model.*;
import com.pplis.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final SkillRepository skillRepository;
    private final StudentSkillRepository studentSkillRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student getStudentById(Integer id) {
        return studentRepository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Student not found: " + id));
    }

    public Student getStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
            .orElseThrow(() ->
                new RuntimeException("Student not found: " + email));
    }

    @Transactional
    public Student createStudent(Student student) {
        if (studentRepository.findByEmail(
                student.getEmail()).isPresent()) {
            throw new RuntimeException(
                "Email already registered: " + student.getEmail());
        }
        log.info("Creating student: {}", student.getFullName());
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(Integer id, Student updated) {
        Student existing = getStudentById(id);
        existing.setFullName(updated.getFullName());
        existing.setPhone(updated.getPhone());
        existing.setGpa(updated.getGpa());
        existing.setProjectScore(updated.getProjectScore());
        existing.setResumeUrl(updated.getResumeUrl());
        return studentRepository.save(existing);
    }

    public List<Student> getUnplacedStudents() {
        return studentRepository.findUnplacedStudentsOrderByGpa();
    }

    public List<Student> getEligibleStudents(
            BigDecimal minGpa, Integer maxBacklogs) {
        return studentRepository
            .findEligibleStudents(minGpa, maxBacklogs);
    }

    @Transactional
    public Map<String, Object> addSkillToStudent(
            Integer studentId, Integer skillId,
            String proficiencyLevel) {

        Student student = getStudentById(studentId);
        Skill skill = skillRepository.findById(skillId)
            .orElseThrow(() ->
                new RuntimeException("Skill not found: " + skillId));

        if (studentSkillRepository
                .existsByStudentStudentIdAndSkillSkillId(
                    studentId, skillId)) {
            throw new RuntimeException("Skill already added");
        }

        StudentSkill ss = new StudentSkill();
        ss.setStudent(student);
        ss.setSkill(skill);
        ss.setProficiencyLevel(proficiencyLevel);
        studentSkillRepository.save(ss);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Skill added successfully");
        response.put("studentName", student.getFullName());
        response.put("skillName", skill.getSkillName());
        response.put("proficiency", proficiencyLevel);
        return response;
    }

    public List<StudentSkill> getStudentSkills(Integer studentId) {
        return studentSkillRepository
            .findByStudentStudentId(studentId);
    }

    public Map<String, Object> getStudentProfile(Integer studentId) {
        Student student = getStudentById(studentId);
        List<StudentSkill> skills = getStudentSkills(studentId);

        Map<String, Object> profile = new HashMap<>();
        profile.put("studentId",       student.getStudentId());
        profile.put("fullName",        student.getFullName());
        profile.put("email",           student.getEmail());
        profile.put("gpa",             student.getGpa());
        profile.put("department",
            student.getDepartment().getDeptName());
        profile.put("graduationYear",  student.getGraduationYear());
        profile.put("backlogs",        student.getBacklogs());
        profile.put("projectScore",    student.getProjectScore());
        profile.put("isPlaced",        student.getIsPlaced());
        profile.put("totalSkills",     skills.size());
        profile.put("skills",          skills);
        return profile;
    }
}