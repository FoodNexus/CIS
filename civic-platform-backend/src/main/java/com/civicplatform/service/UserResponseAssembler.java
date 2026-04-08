package com.civicplatform.service;

import com.civicplatform.dto.response.AuthResponse;
import com.civicplatform.dto.response.UserResponse;
import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserResponseAssembler {

    private final UserMapper userMapper;
    private final BadgeService badgeService;

    public UserResponse toUserResponse(User user) {
        UserResponse r = userMapper.toResponse(user);
        r.setBadgeProgress(computeProgress(user));
        return r;
    }

    public AuthResponse toAuthResponse(User user, String accessToken, String refreshToken) {
        Badge b = user.getBadge() != null ? user.getBadge() : Badge.NONE;
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .userType(user.getUserType())
                .role(user.getRole())
                .badge(user.getBadge())
                .points(user.getPoints())
                .awardedDate(user.getAwardedDate())
                .createdAt(user.getCreatedAt())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .companyName(user.getCompanyName())
                .associationName(user.getAssociationName())
                .contactName(user.getContactName())
                .contactEmail(user.getContactEmail())
                .birthDate(user.getBirthDate())
                .badgeProgress(computeProgress(user))
                .build();
    }

    private com.civicplatform.dto.response.BadgeProgressInfo computeProgress(User user) {
        Badge stored = user.getBadge() != null ? user.getBadge() : Badge.NONE;
        long attended = badgeService.countAttendedCompletedEvents(user.getId());
        return badgeService.computeBadgeProgress(stored, attended);
    }
}
