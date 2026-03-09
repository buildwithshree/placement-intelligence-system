package com.pplis.backend.repository;

import com.pplis.backend.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository 
    extends JpaRepository<Company, Integer> {
    
    Optional<Company> findByCompanyName(String companyName);
    List<Company> findBySector(String sector);
    List<Company> findByCompanyType(String companyType);
    List<Company> findByIsActiveRecruiterTrue();

    @Query("SELECT c FROM Company c WHERE c.isActiveRecruiter = true ORDER BY c.companyName")
    List<Company> findAllActiveRecruiters();
}
