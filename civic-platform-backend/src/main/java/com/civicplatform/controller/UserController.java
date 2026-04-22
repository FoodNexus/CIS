package com.civicplatform.controller;

import com.civicplatform.dto.request.ProfileUpdateRequest;
import com.civicplatform.dto.request.UserRequest;
import com.civicplatform.dto.response.UserResponse;
import com.civicplatform.enums.UserType;
import com.civicplatform.entity.User;
import com.civicplatform.repository.EventCitizenInvitationRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.security.CurrentUserResolver;
import com.civicplatform.security.RegularAccountPolicy;
import com.civicplatform.service.ProfilePictureStorageService;
import com.civicplatform.service.QrCodeService;
import com.civicplatform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final CurrentUserResolver currentUserResolver;
    private final EventCitizenInvitationRepository eventCitizenInvitationRepository;
    private final QrCodeService qrCodeService;
    private final ProfilePictureStorageService profilePictureStorageService;

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
        User user = currentUserResolver.resolveOrCreate(authentication);
        if (user.isAdmin()) {
            throw new AccessDeniedException("Platform admin accounts do not have a participant profile. Use admin tools.");
        }
        return ResponseEntity.ok(userService.getUserById(user.getId()));
    }

    @Operation(summary = "Get profile picture image (public URL for img tags)")
    @GetMapping("/{id}/profile-picture")
    public ResponseEntity<Resource> getProfilePicture(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        if (user.isAdmin()) {
            return ResponseEntity.notFound().build();
        }
        String ext = user.getProfilePictureExtension();
        if (ext == null || ext.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Resource resource = profilePictureStorageService.loadAsResource(user.getId(), ext);
            if (resource == null || !resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(mediaTypeForExtension(ext))
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static MediaType mediaTypeForExtension(String ext) {
        String e = ext.toLowerCase();
        return switch (e) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    @Operation(summary = "Upload profile picture (JPEG, PNG, or WebP)")
    @PostMapping("/{id}/profile-picture")
    public ResponseEntity<UserResponse> uploadProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        User authUser = requireRegularAuthenticatedUser(authentication);
        if (!canAccessOwnProfile(authUser, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserResponse response = userService.uploadProfilePicture(id, file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Upload current user profile picture (JPEG, PNG, or WebP)")
    @PostMapping("/me/profile-picture")
    public ResponseEntity<UserResponse> uploadMyProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        User authUser = requireRegularAuthenticatedUser(authentication);
        UserResponse response = userService.uploadProfilePicture(authUser.getId(), file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Download PNG QR code with user identity (plain-text CivicIdentity payload)")
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> getUserQrCode(@PathVariable Long id, Authentication authentication) {
        User authUser = currentUserResolver.resolveRequired(authentication);
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

    @Operation(summary = "Get user by ID (self, platform admin, or event organizer who invited this citizen)")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id, Authentication authentication) {
        User authUser = currentUserResolver.resolveRequired(authentication);
        if (!authUser.isAdmin() && !authUser.getId().equals(id)) {
            boolean invitedByOrganizer = eventCitizenInvitationRepository.existsByCitizenIdAndEventOrganizerId(
                    id, authUser.getId());
            if (!invitedByOrganizer) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
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
        User authUser = requireRegularAuthenticatedUser(authentication);
        if (!canAccessOwnProfile(authUser, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserResponse response = userService.updateProfile(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update current authenticated regular user profile")
    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        User authUser = requireRegularAuthenticatedUser(authentication);
        UserResponse response = userService.updateProfile(authUser.getId(), request);
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

    private User requireRegularAuthenticatedUser(Authentication authentication) {
        User authUser = currentUserResolver.resolveRequired(authentication);
        if (authUser.isAdmin()) {
            throw new AccessDeniedException("Admins do not have participant profiles.");
        }
        return authUser;
    }

    private boolean canAccessOwnProfile(User authUser, Long requestedUserId) {
        UserResponse current = userService.getUserById(requestedUserId);
        return authUser.getEmail().equalsIgnoreCase(current.getEmail());
    }
}
