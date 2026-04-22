package com.civicplatform.security;

import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentUserResolver {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User resolveRequired(Authentication authentication) {
        return resolveInternal(authentication, false);
    }

    @Transactional
    public User resolveOrCreate(Authentication authentication) {
        return resolveInternal(authentication, true);
    }

    private User resolveInternal(Authentication authentication, boolean createIfMissing) {
        if (authentication == null) {
            throw new AccessDeniedException("Missing authentication");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return resolveFromJwt(jwt, createIfMissing);
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    private User resolveFromJwt(Jwt jwt, boolean createIfMissing) {
        String keycloakId = jwt.getSubject();
        String email = normalizeEmail(jwt.getClaimAsString("email"));
        boolean tokenAdmin = hasAdminRole(jwt);

        Optional<User> byKeycloakId = isPresent(keycloakId)
                ? userRepository.findByKeycloakId(keycloakId)
                : Optional.empty();
        Optional<User> byEmail = isPresent(email)
                ? userRepository.findByEmail(email)
                : Optional.empty();

        User resolved = byKeycloakId.or(() -> byEmail).orElse(null);
        if (resolved != null) {
            boolean changed = false;
            if (isPresent(keycloakId) && !keycloakId.equals(resolved.getKeycloakId())) {
                resolved.setKeycloakId(keycloakId);
                changed = true;
            }
            // Keep local platform-admin flag aligned with Keycloak realm role.
            if (resolved.isAdmin() != tokenAdmin) {
                resolved.setAdmin(tokenAdmin);
                // Admin accounts don't use regular participant lifecycle.
                if (tokenAdmin) {
                    resolved.setUserType(null);
                } else if (resolved.getUserType() == null) {
                    resolved.setUserType(UserType.CITIZEN);
                }
                changed = true;
            }
            if (changed) {
                resolved = userRepository.save(resolved);
            }
            return resolved;
        }

        if (!createIfMissing) {
            throw new AccessDeniedException("User not found for token subject/email");
        }
        if (!isPresent(email)) {
            throw new AccessDeniedException("Token must include email to initialize local profile");
        }

        String preferredUsername = firstPresent(jwt.getClaimAsString("preferred_username"), email.split("@")[0]);
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        User user = User.builder()
                .userName(generateUniqueUsername(preferredUsername))
                .email(email)
                .password(passwordEncoder.encode("EXTERNAL_AUTH_" + UUID.randomUUID()))
                .admin(tokenAdmin)
                .userType(tokenAdmin ? null : UserType.CITIZEN)
                .badge(Badge.NONE)
                .points(0)
                .firstName(givenName)
                .lastName(familyName)
                .keycloakId(keycloakId)
                .actif(true)
                .deletionRequested(false)
                .build();

        User created = userRepository.save(user);
        log.info("Created local profile for keycloak subject={} email={}", keycloakId, email);
        return created;
    }

    private boolean hasAdminRole(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmMap)) {
            return false;
        }
        Object roles = realmMap.get("roles");
        if (!(roles instanceof Collection<?> collection)) {
            return false;
        }
        return collection.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(String.valueOf(r)));
    }

    private String generateUniqueUsername(String baseRaw) {
        String base = sanitizeUsername(baseRaw);
        if (!isPresent(base)) {
            base = "user";
        }
        String candidate = base;
        int i = 1;
        while (userRepository.existsByUserName(candidate)) {
            candidate = base + i++;
        }
        return candidate;
    }

    private static String sanitizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (normalized.length() > 50) {
            return normalized.substring(0, 50);
        }
        return normalized;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String firstPresent(String first, String fallback) {
        return isPresent(first) ? first : fallback;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
