package com.civicplatform.service;

import com.civicplatform.entity.UserInteraction;
import com.civicplatform.enums.InteractionAction;
import com.civicplatform.enums.InteractionEntityType;
import com.civicplatform.repository.UserInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserInteractionService {

    private final UserInteractionRepository userInteractionRepository;

    @Transactional
    public void record(Long userId, InteractionEntityType entityType, Long entityId, InteractionAction action) {
        UserInteraction row = UserInteraction.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .createdAt(LocalDateTime.now())
                .build();
        userInteractionRepository.save(row);
    }
}
