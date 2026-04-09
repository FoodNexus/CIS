package com.civicplatform.controller;

import com.civicplatform.dto.request.ProfileUpdateRequest;
import com.civicplatform.dto.request.UserRequest;
import com.civicplatform.dto.response.UserResponse;
import com.civicplatform.enums.UserType;
import com.civicplatform.entity.User;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.security.RegularAccountPolicy;
import com.civicplatform.service.QrCodeService;
import com.civicplatform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;

    @Operation(summary = "Create a new user")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        UserResponse response = userService.createUser(userRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get current authenticated regular user profile (not for platform admins)")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("User not resolved"));
        if (user.isAdmin()) {
            throw new AccessDeniedException("Platform admin accounts do not have a participant profile. Use admin tools.");
        }
        return ResponseEntity.ok(userService.getUserByEmail(user.getEmail()));
    }

    @Operation(summary = "Download PNG QR code with user identity (plain-text CivicIdentity payload)")
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> getUserQrCode(@PathVariable Long id, Authentication authentication) {
        User authUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("User not resolved"));
        RegularAccountPolicy.requireRegularUser(authUser);
        if (!authUser.getId().equals(id)) {
            throw new AccessDeniedException("You can only access your own QR code");
        }
        byte[] qrCode = qrCodeService.generateQrCode(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qrcode-user-" + id + ".png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    @Operation(summary = "Get user by email")
    @GetMapping("/email/{email:.+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all regular users")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get users by type")
    @GetMapping("/type/{userType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsersByType(@PathVariable UserType userType) {
        List<UserResponse> response = userService.getUsersByType(userType);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user by ID (self or platform admin)")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id, Authentication authentication) {
        User authUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("User not resolved"));
        if (!authUser.isAdmin() && !authUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest userRequest) {
        UserResponse response = userService.updateUser(id, userRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user profile")
    @PutMapping("/{id}/profile")
    public ResponseEntity<UserResponse> updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        User authUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("User not resolved"));
        if (authUser.isAdmin()) {
            throw new AccessDeniedException("Admins do not have participant profiles.");
        }
        UserResponse current = userService.getUserById(id);
        if (!authentication.getName().equalsIgnoreCase(current.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserResponse response = userService.updateProfile(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Promote user to ambassador")
    @PostMapping("/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> promoteToAmbassador(@PathVariable Long id) {
        userService.promoteToAmbassador(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get user count by type")
    @GetMapping("/count/{userType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countUsersByType(@PathVariable UserType userType) {
        Long count = userService.countUsersByType(userType);
        return ResponseEntity.ok(count);
    }
}
