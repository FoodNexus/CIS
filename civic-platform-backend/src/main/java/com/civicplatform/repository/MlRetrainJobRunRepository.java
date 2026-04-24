package com.civicplatform.repository;

import com.civicplatform.entity.MlRetrainJobRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MlRetrainJobRunRepository extends JpaRepository<MlRetrainJobRun, Long> {
    Page<MlRetrainJobRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
