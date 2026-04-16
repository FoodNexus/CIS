package com.civicplatform.repository;

import com.civicplatform.entity.User;
import com.civicplatform.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);

    Optional<User> findByUserName(String userName);
    
    boolean existsByEmail(String email);
    
    boolean existsByUserName(String userName);
    
    List<User> findByUserType(UserType userType);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.userType = :userType")
    long countByUserType(UserType userType);
    
    @Query("SELECT u FROM User u WHERE u.userType = 'CITIZEN' AND " +
           "(SELECT COUNT(ep) FROM EventParticipant ep WHERE ep.user.id = u.id AND ep.status = 'COMPLETED') >= 3")
    List<User> findCitizensEligibleForAmbassadorPromotion();

    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :from AND :to" +
           " AND (:type IS NULL OR u.userType = :type)")
    List<User> findByDateRangeAndType(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("type") UserType type);

    @Query("SELECT u FROM User u WHERE u.userType IN ('CITIZEN','AMBASSADOR','PARTICIPANT') AND u.admin = false")
    List<User> findEligibleCitizensForInvitations();
}
