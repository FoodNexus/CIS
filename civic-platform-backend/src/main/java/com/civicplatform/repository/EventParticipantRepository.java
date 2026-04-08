package com.civicplatform.repository;

import com.civicplatform.entity.EventParticipant;
import com.civicplatform.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {
    
    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);
    
    List<EventParticipant> findByEventIdOrderByRegisteredAtAsc(Long eventId);
    
    List<EventParticipant> findByUserId(Long userId);

    @Query("SELECT ep FROM EventParticipant ep JOIN FETCH ep.event JOIN FETCH ep.user WHERE ep.user.id = :userId")
    List<EventParticipant> findByUserIdWithEventAndUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.event.id = :eventId AND ep.status IN ('REGISTERED', 'CHECKED_IN')")
    long countActiveParticipants(Long eventId);
    
    /**
     * Attended events: participation COMPLETED and parent event COMPLETED (lifecycle finished).
     */
    @Query("SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.user.id = :userId"
            + " AND ep.status = 'COMPLETED' AND ep.event.status = 'COMPLETED'")
    long countAttendedCompletedEventsByUser(@Param("userId") Long userId);
    
    void deleteByEventIdAndUserId(Long eventId, Long userId);
}
