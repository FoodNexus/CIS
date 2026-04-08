package com.civicplatform.repository;

import com.civicplatform.entity.ProjectVote;
import com.civicplatform.entity.ProjectVoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectVoteRepository extends JpaRepository<ProjectVote, ProjectVoteId> {

    @Query("SELECT v FROM ProjectVote v WHERE v.id.userId = :userId AND v.id.projectId = :projectId")
    Optional<ProjectVote> findByUserIdAndProjectId(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId);
}
