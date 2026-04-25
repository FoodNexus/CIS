package com.civicplatform.repository;

import com.civicplatform.entity.EventParticipant;
import com.civicplatform.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.event.organizerId = :organizerId AND ep.status <> 'CANCELLED'")
    long countNonCancelledByOrganizerId(@Param("organizerId") Long organizerId);
    
    /**
     * Attended events: participation COMPLETED and parent event COMPLETED (lifecycle finished).
     */
    @Query("SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.user.id = :userId"
            + " AND ep.status = 'COMPLETED' AND ep.event.status = 'COMPLETED'")
    long countAttendedCompletedEventsByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.user.id = :userId "
            + "AND ep.status = 'COMPLETED' AND ep.event.status = 'COMPLETED'")
    int countCompletedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(ep) > 0 FROM EventParticipant ep WHERE ep.user.id = :userId "
            + "AND ep.event.organizerId = :organizerId")
    boolean hasAttendedEventByOrganizer(
            @Param("userId") Long userId,
            @Param("organizerId") Long organizerId);

    @Query("SELECT MAX(ep.registeredAt) FROM EventParticipant ep WHERE ep.user.id = :userId")
    Optional<LocalDateTime> findMaxRegisteredAtByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(ep.completedAt) FROM EventParticipant ep WHERE ep.user.id = :userId")
    Optional<LocalDateTime> findMaxCompletedAtByUserId(@Param("userId") Long userId);

    /**
     * Completed participations where the event lifecycle is finished (participant + event both completed).
     */
    @Query("SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.user.id = :userId AND ep.status = 'COMPLETED' "
            + "AND ep.event.status = 'COMPLETED' AND ep.event.type = :type")
    long countCompletedByUserIdAndEventType(@Param("userId") Long userId, @Param("type") EventType type);

    @Query("SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.user.id = :userId AND ep.status != 'CANCELLED' "
            + "AND ep.registeredAt IS NOT NULL")
    long countRegisteredNonCancelledByUserId(@Param("userId") Long userId);

    /**
     * Showed up: check-in recorded or terminal attended status (excludes NO_SHOW and plain REGISTERED).
     */
    @Query("SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.user.id = :userId AND ep.status != 'CANCELLED' "
            + "AND ep.registeredAt IS NOT NULL AND (ep.checkedInAt IS NOT NULL OR ep.status IN ('CHECKED_IN','COMPLETED'))")
    long countReliableAttendanceByUserId(@Param("userId") Long userId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);
}
