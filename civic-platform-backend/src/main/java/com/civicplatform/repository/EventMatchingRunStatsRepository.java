package com.civicplatform.repository;

import com.civicplatform.entity.EventMatchingRunStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventMatchingRunStatsRepository extends JpaRepository<EventMatchingRunStats, Long> {

    Page<EventMatchingRunStats> findByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    Optional<EventMatchingRunStats> findTopByEventIdOrderByCreatedAtDesc(Long eventId);
}
