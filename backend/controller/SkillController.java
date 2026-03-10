package com.pplis.backend.controller;

import com.pplis.backend.model.Skill;
import com.pplis.backend.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/skills")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    // GET /api/skills
    @GetMapping
    public ResponseEntity<List<Skill>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    // GET /api/skills/critical
    @GetMapping("/critical")
    public ResponseEntity<List<Skill>> getCriticalSkills() {
        return ResponseEntity.ok(skillService.getCriticalSkills());
    }

    // GET /api/skills/category/{category}
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Skill>> getByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(
            skillService.getSkillsByCategory(category));
    }
}
