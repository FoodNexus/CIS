package com.civicplatform.repository;

import com.civicplatform.entity.UserInteraction;
import com.civicplatform.enums.InteractionAction;
import com.civicplatform.enums.InteractionEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    @Query("SELECT MAX(ui.createdAt) FROM UserInteraction ui WHERE ui.userId = :userId")
    Optional<LocalDateTime> findLastInteractionDate(@Param("userId") Long userId);

    long countByUserIdAndEntityType(Long userId, InteractionEntityType entityType);

    long countByUserIdAndEntityTypeAndAction(Long userId, InteractionEntityType entityType, InteractionAction action);

    @Query("SELECT COUNT(ui) FROM UserInteraction ui WHERE ui.userId = :userId AND ui.entityType = :type "
            + "AND ui.entityId IN :ids")
    long countByUserIdAndEntityTypeAndEntityIdIn(
            @Param("userId") Long userId,
            @Param("type") InteractionEntityType type,
            @Param("ids") Collection<Long> ids);
}
