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
public class UserResponse {
    
    private Long id;
    private String userName;
    private String email;
    private UserType userType;
    private Role role;
    private LocalDateTime createdAt;
    
    // AMBASSADOR fields
    private Badge badge;
    private LocalDate awardedDate;
    
    // DONOR fields
    private String companyName;
    private String associationName;
    private String contactName;
    private String contactEmail;
    private String address;
    
    // CITIZEN fields
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;
    
    // PARTICIPANT fields
    private Integer points;

    /** Progress toward next badge (see BadgeService). */
    private BadgeProgressInfo badgeProgress;
}
