package com.civicplatform.dto.response;

import com.civicplatform.enums.Badge;
import com.civicplatform.enums.Role;
import com.civicplatform.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    private String refreshToken;
    private Long userId;
    private String userName;
    private String email;
    private UserType userType;
    private Role role;
    private Badge badge;
    private Integer points;
    private LocalDate awardedDate;
    private LocalDateTime createdAt;
    
    // Additional user fields
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String companyName;
    private String associationName;
    private String contactName;
    private String contactEmail;
    private LocalDate birthDate;

    private BadgeProgressInfo badgeProgress;
}
