package com.pplis.backend.service;

import com.pplis.backend.model.Skill;
import com.pplis.backend.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    public List<Skill> getSkillsByCategory(String category) {
        return skillRepository.findByCategory(category);
    }

    public List<Skill> getCriticalSkills() {
        return skillRepository.findByDemandLevel("critical");
    }

    public Skill getSkillById(Integer id) {
        return skillRepository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Skill not found: " + id));
    }
}