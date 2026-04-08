package com.civicplatform.service.impl;

import com.civicplatform.entity.User;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.BadgeService;
import com.civicplatform.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final UserRepository userRepository;
    private final BadgeService badgeService;

    @Override
    @Transactional
    public void processEventAttendance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        badgeService.applyBadgeForUser(user);
        userRepository.save(user);
        log.debug("Badge/points refreshed for user {} after attendance confirmation", user.getEmail());
    }
}
