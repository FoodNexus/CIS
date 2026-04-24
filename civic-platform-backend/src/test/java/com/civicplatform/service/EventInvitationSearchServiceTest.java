package com.civicplatform.service;

import com.civicplatform.entity.EventCitizenInvitation;
import com.civicplatform.repository.EventCitizenInvitationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventInvitationSearchServiceTest {

    @Mock
    private EventCitizenInvitationRepository repository;

    @Test
    void searchEventInvitations_throwsBadRequest_forInvalidJqlClause() {
        EventInvitationSearchService service = new EventInvitationSearchService(repository);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.searchEventInvitations(12L, "status === INVITED", PageRequest.of(0, 20))
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void searchEventInvitations_executesRepository_forValidJql() {
        EventInvitationSearchService service = new EventInvitationSearchService(repository);
        Page<EventCitizenInvitation> page = Page.empty();
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(PageRequest.of(0, 20))))
                .thenReturn(page);

        Page<EventCitizenInvitation> result = service.searchEventInvitations(
                12L,
                "status = INVITED AND compositeRate >= 55",
                PageRequest.of(0, 20)
        );

        assertEquals(0, result.getTotalElements());
        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(PageRequest.of(0, 20)));
    }
}
