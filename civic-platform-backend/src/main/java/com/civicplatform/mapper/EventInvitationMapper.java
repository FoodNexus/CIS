package com.civicplatform.mapper;

import com.civicplatform.dto.response.EventInvitationResponse;
import com.civicplatform.entity.EventCitizenInvitation;
import com.civicplatform.entity.User;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.repository.UserRepository;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class EventInvitationMapper {

    @Autowired
    protected EventParticipantRepository eventParticipantRepository;

    @Autowired
    protected UserRepository userRepository;

    @Mapping(target = "invitationId", source = "id")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "citizenId", source = "citizen.id")
    @Mapping(target = "citizenFullName", expression = "java(fullName(invitation.getCitizen()))")
    @Mapping(target = "citizenUserType", expression = "java(userTypeString(invitation.getCitizen()))")
    @Mapping(target = "citizenBadge", expression = "java(badgeString(invitation.getCitizen()))")
    @Mapping(target = "citizenEventsAttended", ignore = true)
    @Mapping(target = "matchScorePercent", ignore = true)
    @Mapping(target = "invitationTier", expression = "java(tierString(invitation))")
    @Mapping(target = "status", expression = "java(invitation.getStatus() != null ? invitation.getStatus().name() : null)")
    @Mapping(target = "donorAssociationName", expression = "java(donorAssociation(invitation))")
    public abstract EventInvitationResponse toResponse(EventCitizenInvitation invitation);

    public EventInvitationResponse toResponseForOrganizer(EventCitizenInvitation invitation) {
        EventInvitationResponse r = toResponse(invitation);
        r.setInvitationToken(null);
        return r;
    }

    protected String fullName(User u) {
        if (u == null) {
            return "";
        }
        String fn = u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u.getLastName() != null ? u.getLastName() : "";
        String t = (fn + " " + ln).trim();
        return t.isEmpty() ? u.getUserName() : t;
    }

    protected String userTypeString(User u) {
        return u != null && u.getUserType() != null ? u.getUserType().name() : "";
    }

    protected String badgeString(User u) {
        return u != null && u.getBadge() != null ? u.getBadge().name() : "NONE";
    }

    protected String tierString(EventCitizenInvitation invitation) {
        return invitation.getInvitationTier() != null ? invitation.getInvitationTier().name() : null;
    }

    protected String donorAssociation(EventCitizenInvitation invitation) {
        if (invitation.getEvent() == null || invitation.getEvent().getOrganizerId() == null) {
            return null;
        }
        return userRepository.findById(invitation.getEvent().getOrganizerId())
                .map(donor -> {
                    if (donor.getAssociationName() != null && !donor.getAssociationName().isBlank()) {
                        return donor.getAssociationName();
                    }
                    return donor.getContactName() != null ? donor.getContactName() : donor.getUserName();
                })
                .orElse(null);
    }

    @AfterMapping
    protected void fillDerived(EventCitizenInvitation invitation, @MappingTarget EventInvitationResponse r) {
        if (invitation.getCompositeRate() != null) {
            r.setMatchScorePercent(Math.round(invitation.getCompositeRate() * 10.0) / 10.0);
        } else {
            double pct = (r.getMatchScore() / 125.0) * 100.0;
            r.setMatchScorePercent(Math.round(pct * 10.0) / 10.0);
        }
        if (invitation.getCitizen() != null) {
            r.setCitizenEventsAttended(eventParticipantRepository.countCompletedByUserId(invitation.getCitizen().getId()));
        }
    }
}
