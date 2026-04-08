package com.civicplatform.service.impl;

import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;

    @Override
    @Transactional
    public void processEventAttendance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        long eventsAttended = eventParticipantRepository.countCompletedEventsByUser(userId);

        user.setPoints((int) eventsAttended);

        // Update badge based on attendance
        Badge newBadge = calculateBadge(eventsAttended);
        if (newBadge != user.getBadge()) {
            user.setBadge(newBadge);
            user.setAwardedDate(LocalDate.now());
            log.info("User {} promoted to badge: {}", user.getEmail(), newBadge);
        }

        // Update user_type based on attendance and current type
        UserType newUserType = calculateUserType(user.getUserType(), eventsAttended);
        if (newUserType != user.getUserType()) {
            user.setUserType(newUserType);
            log.info("User {} promoted to userType: {}", user.getEmail(), newUserType);
        }

        userRepository.save(user);
    }

    private Badge calculateBadge(long eventsAttended) {
        if (eventsAttended >= 8) {
            return Badge.PLATINUM;
        } else if (eventsAttended >= 5) {
            return Badge.GOLD;
        } else if (eventsAttended >= 3) {
            return Badge.SILVER;
        } else if (eventsAttended >= 1) {
            return Badge.BRONZE;
        }
        return Badge.NONE;
    }

    private UserType calculateUserType(UserType currentType, long eventsAttended) {
        switch (currentType) {
            case CITIZEN:
                if (eventsAttended >= 1) {
                    return UserType.PARTICIPANT;
                }
                break;
            case PARTICIPANT:
                if (eventsAttended >= 5) {
                    return UserType.AMBASSADOR;
                }
                break;
            case DONOR:
                if (eventsAttended >= 5) {
                    return UserType.AMBASSADOR;
                }
                break;
            default:
                // AMBASSADOR stays AMBASSADOR
                break;
        }
        return currentType;
    }
}
