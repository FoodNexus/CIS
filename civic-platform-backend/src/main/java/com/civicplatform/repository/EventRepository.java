package com.civicplatform.repository;

import com.civicplatform.entity.Event;
import com.civicplatform.enums.EventStatus;
import com.civicplatform.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    
    List<Event> findByStatus(EventStatus status);
    
    Page<Event> findByStatus(EventStatus status, Pageable pageable);
    
    List<Event> findByType(EventType type);
    
    Page<Event> findByType(EventType type, Pageable pageable);
    
    List<Event> findByOrganizerId(Long organizerId);
    
    @Query("SELECT e FROM Event e WHERE e.date >= :now ORDER BY e.date ASC")
    List<Event> findUpcomingEvents(LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.date >= :now AND e.status = 'UPCOMING' AND e.id <> :excludeId ORDER BY e.date ASC")
    List<Event> findUpcomingExcluding(@Param("now") LocalDateTime now, @Param("excludeId") Long excludeId);
    
    @Query("SELECT e FROM Event e WHERE e.date <= :now AND e.status = 'UPCOMING'")
    List<Event> findEventsThatShouldStart(LocalDateTime now);

    List<Event> findByOrganizerIdAndDateBetween(Long organizerId, LocalDateTime from, LocalDateTime to);
    
    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status")
    long countByStatus(EventStatus status);
}
