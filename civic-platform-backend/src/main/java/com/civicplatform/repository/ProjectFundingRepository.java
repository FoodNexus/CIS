package com.civicplatform.repository;

import com.civicplatform.entity.ProjectFunding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProjectFundingRepository extends JpaRepository<ProjectFunding, Long> {
    
    List<ProjectFunding> findByProjectIdOrderByFundDateDesc(Long projectId);
    
    List<ProjectFunding> findByUserId(Long userId);

    @Query("SELECT pf FROM ProjectFunding pf JOIN FETCH pf.project JOIN FETCH pf.user WHERE pf.user.id = :userId ORDER BY pf.fundDate DESC")
    List<ProjectFunding> findByUserIdWithDetails(@Param("userId") Long userId);
    
    @Query("SELECT SUM(pf.amount) FROM ProjectFunding pf WHERE pf.project.id = :projectId")
    BigDecimal sumFundingByProject(Long projectId);
    
    @Query("SELECT COUNT(pf) FROM ProjectFunding pf WHERE pf.user.id = :userId")
    long countFundingsByUser(Long userId);
}
