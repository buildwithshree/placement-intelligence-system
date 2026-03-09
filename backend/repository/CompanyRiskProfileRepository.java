package com.pplis.backend.repository;

import com.pplis.backend.model.CompanyRiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRiskProfileRepository 
    extends JpaRepository<CompanyRiskProfile, Integer> {

    Optional<CompanyRiskProfile> findByCompanyCompanyId(Integer companyId);
    List<CompanyRiskProfile> findByRiskLevel(String riskLevel);

    @Query("SELECT crp FROM CompanyRiskProfile crp " +
           "ORDER BY crp.riskIndex ASC")
    List<CompanyRiskProfile> findAllOrderBySafest();

    @Query("SELECT crp FROM CompanyRiskProfile crp " +
           "WHERE crp.riskLevel IN ('low') " +
           "ORDER BY crp.stabilityScore DESC")
    List<CompanyRiskProfile> findSafeCompanies();

    @Query("SELECT crp FROM CompanyRiskProfile crp " +
           "WHERE crp.riskLevel IN ('high', 'critical') " +
           "ORDER BY crp.riskIndex DESC")
    List<CompanyRiskProfile> findHighRiskCompanies();
}
