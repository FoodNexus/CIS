package com.civicplatform.repository;

import com.civicplatform.entity.EventCitizenInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventCitizenInvitationRepository
        extends JpaRepository<EventCitizenInvitation, Long>, JpaSpecificationExecutor<EventCitizenInvitation> {

    boolean existsByEvent_Id(Long eventId);

    List<EventCitizenInvitation> findByEventId(Long eventId);

    List<EventCitizenInvitation> findByCitizenId(Long citizenId);

    Optional<EventCitizenInvitation> findByInvitationToken(String token);

    boolean existsByEvent_IdAndCitizen_Id(Long eventId, Long citizenId);

    @Query("SELECT eci FROM EventCitizenInvitation eci "
            + "WHERE eci.event.id = :eventId ORDER BY eci.matchScore DESC")
    List<EventCitizenInvitation> findByEventIdOrderByScoreDesc(@Param("eventId") Long eventId);

    @Query("SELECT eci FROM EventCitizenInvitation eci "
            + "WHERE eci.citizen.id = :citizenId ORDER BY eci.invitedAt DESC")
    List<EventCitizenInvitation> findByCitizenIdOrderByInvitedAtDesc(@Param("citizenId") Long citizenId);

    @Modifying
    @Query("DELETE FROM EventCitizenInvitation e WHERE e.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);

    /**
     * True when this citizen appears on the invitation list for any event organized by the given user.
     * Used so organizers can open invitee profiles without full admin access.
     */
    @Query("SELECT COUNT(eci) > 0 FROM EventCitizenInvitation eci "
            + "WHERE eci.citizen.id = :citizenId AND eci.event.organizerId = :organizerId")
    boolean existsByCitizenIdAndEventOrganizerId(
            @Param("citizenId") Long citizenId, @Param("organizerId") Long organizerId);
}
