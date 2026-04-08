package com.civicplatform.service.impl;

import com.civicplatform.dto.request.ProfileUpdateRequest;
import com.civicplatform.dto.request.UserRequest;
import com.civicplatform.dto.response.UserResponse;
import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.enums.Role;
import com.civicplatform.enums.UserType;
import com.civicplatform.mapper.UserMapper;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.EmailService;
import com.civicplatform.service.UserResponseAssembler;
import com.civicplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserResponseAssembler userResponseAssembler;
    private final EventParticipantRepository eventParticipantRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        User user = userMapper.toEntity(userRequest);
        // Always hardcode these fields on registration - ignore any values in request
        user.setRole(Role.USER);
        user.setBadge(null);
        user.setPoints(0);
        user = userRepository.save(user);
        return userResponseAssembler.toUserResponse(user);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return userResponseAssembler.toUserResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return userResponseAssembler.toUserResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(userResponseAssembler::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getUsersByType(UserType userType) {
        List<User> users = userRepository.findByUserType(userType);
        return users.stream()
                .map(userResponseAssembler::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest userRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (userRequest.getEmail() != null && !userRequest.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(userRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRequest.getUserName() != null && !userRequest.getUserName().equalsIgnoreCase(user.getUserName())
                && userRepository.existsByUserName(userRequest.getUserName())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRequest.getUserName() != null) user.setUserName(userRequest.getUserName());
        if (userRequest.getEmail() != null) user.setEmail(userRequest.getEmail());
        if (userRequest.getFirstName() != null) user.setFirstName(userRequest.getFirstName());
        if (userRequest.getLastName() != null) user.setLastName(userRequest.getLastName());
        if (userRequest.getPhone() != null) user.setPhone(userRequest.getPhone());
        if (userRequest.getAddress() != null) user.setAddress(userRequest.getAddress());
        if (userRequest.getCompanyName() != null) user.setCompanyName(userRequest.getCompanyName());
        if (userRequest.getAssociationName() != null) user.setAssociationName(userRequest.getAssociationName());
        if (userRequest.getContactName() != null) user.setContactName(userRequest.getContactName());
        if (userRequest.getContactEmail() != null) user.setContactEmail(userRequest.getContactEmail());
        if (userRequest.getBirthDate() != null && !userRequest.getBirthDate().isBlank()) {
            user.setBirthDate(LocalDate.parse(userRequest.getBirthDate()));
        }

        user = userRepository.save(user);
        return userResponseAssembler.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long id, ProfileUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Only update profile fields - never touch userName, email, role, userType, badge, points
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getCompanyName() != null) user.setCompanyName(request.getCompanyName());
        if (request.getAssociationName() != null) user.setAssociationName(request.getAssociationName());
        if (request.getContactName() != null) user.setContactName(request.getContactName());
        if (request.getContactEmail() != null) user.setContactEmail(request.getContactEmail());
        if (request.getBirthDate() != null && !request.getBirthDate().isBlank()) {
            user.setBirthDate(LocalDate.parse(request.getBirthDate()));
        }

        user = userRepository.save(user);
        return userResponseAssembler.toUserResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void promoteToAmbassador(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getUserType() != UserType.CITIZEN) {
            throw new RuntimeException("Only CITIZEN users can be promoted to AMBASSADOR");
        }

        long completedEventsCount = eventParticipantRepository.countAttendedCompletedEventsByUser(userId);
        if (completedEventsCount < 5) {
            throw new RuntimeException("User must have completed at least 5 events to be promoted to AMBASSADOR");
        }

        user.setUserType(UserType.AMBASSADOR);
        user.setBadge(Badge.PLATINUM);
        user.setAwardedDate(LocalDate.now());

        userRepository.save(user);

        emailService.sendAmbassadorPromotionEmail(user.getEmail(), user.getUserName());
        log.info("User {} has been promoted to AMBASSADOR with badge PLATINUM", user.getEmail());
    }

    @Override
    public long countUsersByType(UserType userType) {
        return userRepository.countByUserType(userType);
    }
}
