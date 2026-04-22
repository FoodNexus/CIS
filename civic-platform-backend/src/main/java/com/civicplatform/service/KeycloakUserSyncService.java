package com.civicplatform.service;

import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserSyncService {

    private static final int PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakSyncProperties properties;
    private final RestClient restClient = RestClient.builder().build();
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Transactional
    public SyncResult syncUsers() {
        if (!properties.isEnabled()) {
            return SyncResult.disabled();
        }
        if (!running.compareAndSet(false, true)) {
            return SyncResult.busy();
        }
        try {
            String token = fetchAdminAccessToken();
            if (token == null || token.isBlank()) {
                return SyncResult.skipped("token_unavailable");
            }

            SyncResult result = new SyncResult();
            int first = 0;
            while (true) {
                List<Map<String, Object>> remoteUsers = fetchUsersPage(token, first, PAGE_SIZE);
                if (remoteUsers == null || remoteUsers.isEmpty()) {
                    break;
                }
                for (Map<String, Object> raw : remoteUsers) {
                    processOneUser(raw, result);
                }
                if (remoteUsers.size() < PAGE_SIZE) {
                    break;
                }
                first += remoteUsers.size();
            }
            log.info("Keycloak user sync complete: {}", result);
            return result;
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.warn("Keycloak user sync skipped: invalid admin client credentials.");
            return SyncResult.skipped("invalid_credentials");
        } catch (HttpClientErrorException ex) {
            log.warn("Keycloak user sync skipped: Keycloak API error status={}", ex.getStatusCode());
            return SyncResult.skipped("keycloak_http_" + ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Keycloak user sync failed: {}", ex.getMessage());
            return SyncResult.failed(ex.getMessage());
        } finally {
            running.set(false);
        }
    }

    private String fetchAdminAccessToken() {
        String tokenUrl = properties.tokenRealmBaseUrl() + "/protocol/openid-connect/token";
        if (!isBlank(properties.getClientId()) && !isBlank(properties.getClientSecret())) {
            try {
                MultiValueMap<String, String> clientForm = new LinkedMultiValueMap<>();
                clientForm.add("grant_type", "client_credentials");
                clientForm.add("client_id", properties.getClientId());
                clientForm.add("client_secret", properties.getClientSecret());
                String token = extractToken(requestToken(tokenUrl, clientForm));
                if (!isBlank(token)) {
                    return token;
                }
            } catch (Exception ex) {
                log.warn("Client credentials token request failed: {}", ex.getMessage());
            }
        }

        // Dev fallback: use admin username/password when client credentials are unavailable.
        if (!isBlank(properties.getUsername()) && !isBlank(properties.getPassword())) {
            try {
                MultiValueMap<String, String> passwordForm = new LinkedMultiValueMap<>();
                passwordForm.add("grant_type", "password");
                passwordForm.add("client_id", firstPresent(properties.getPasswordClientId(), "admin-cli"));
                passwordForm.add("username", properties.getUsername());
                passwordForm.add("password", properties.getPassword());
                String token = extractToken(requestToken(tokenUrl, passwordForm));
                if (!isBlank(token)) {
                    return token;
                }
            } catch (Exception ex) {
                log.warn("Password grant token request failed: {}", ex.getMessage());
            }
        }
        return null;
    }

    private Map<String, Object> requestToken(String tokenUrl, MultiValueMap<String, String> form) {
        return restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private static String extractToken(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object accessToken = body.get("access_token");
        return accessToken == null ? null : String.valueOf(accessToken);
    }

    private List<Map<String, Object>> fetchUsersPage(String accessToken, int first, int max) {
        String usersUrl = properties.adminUsersUrl() + "?first=" + first + "&max=" + max;
        return restClient.get()
                .uri(usersUrl)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private void processOneUser(Map<String, Object> raw, SyncResult result) {
        result.scanned++;
        String keycloakId = stringValue(raw.get("id"));
        String username = stringValue(raw.get("username"));
        String email = normalizeEmail(stringValue(raw.get("email")));
        boolean enabled = Boolean.TRUE.equals(raw.get("enabled"));
        String firstName = stringValue(raw.get("firstName"));
        String lastName = stringValue(raw.get("lastName"));

        if (isBlank(email) || isServiceAccount(username)) {
            result.skipped++;
            return;
        }

        Optional<User> existingByKeycloakId = isBlank(keycloakId)
                ? Optional.empty()
                : userRepository.findByKeycloakId(keycloakId);
        Optional<User> existingByEmail = userRepository.findByEmail(email);
        User user = existingByKeycloakId.or(() -> existingByEmail).orElse(null);

        if (user == null) {
            User created = User.builder()
                    .userName(generateUniqueUsername(firstPresent(username, email.split("@")[0])))
                    .email(email)
                    .password(passwordEncoder.encode("EXTERNAL_AUTH_" + UUID.randomUUID()))
                    .keycloakId(keycloakId)
                    .firstName(firstName)
                    .lastName(lastName)
                    .admin(false)
                    .actif(enabled)
                    .deletionRequested(false)
                    .userType(UserType.CITIZEN)
                    .badge(Badge.NONE)
                    .points(0)
                    .build();
            userRepository.save(created);
            result.created++;
            return;
        }

        boolean changed = false;
        if (!isBlank(keycloakId) && !keycloakId.equals(user.getKeycloakId())) {
            user.setKeycloakId(keycloakId);
            changed = true;
        }
        if (!email.equalsIgnoreCase(user.getEmail())) {
            user.setEmail(email);
            changed = true;
        }
        if (!isBlank(firstName) && !firstName.equals(user.getFirstName())) {
            user.setFirstName(firstName);
            changed = true;
        }
        if (!isBlank(lastName) && !lastName.equals(user.getLastName())) {
            user.setLastName(lastName);
            changed = true;
        }
        if (user.isActif() != enabled) {
            user.setActif(enabled);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
            result.updated++;
        } else {
            result.skipped++;
        }
    }

    private String generateUniqueUsername(String baseRaw) {
        String base = sanitizeUsername(baseRaw);
        if (isBlank(base)) {
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
        return isBlank(first) ? fallback : first;
    }

    private static boolean isServiceAccount(String username) {
        return username != null && username.startsWith("service-account-");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @Getter
    public static class SyncResult {
        private int scanned;
        private int created;
        private int updated;
        private int skipped;
        private boolean enabled = true;
        private boolean inProgress;
        private String status = "ok";
        private String details;

        public static SyncResult disabled() {
            SyncResult r = new SyncResult();
            r.enabled = false;
            r.status = "disabled";
            return r;
        }

        public static SyncResult busy() {
            SyncResult r = new SyncResult();
            r.inProgress = true;
            r.status = "busy";
            return r;
        }

        public static SyncResult skipped(String details) {
            SyncResult r = new SyncResult();
            r.status = "skipped";
            r.details = details;
            return r;
        }

        public static SyncResult failed(String details) {
            SyncResult r = new SyncResult();
            r.status = "failed";
            r.details = details;
            return r;
        }

        @Override
        public String toString() {
            return "status=" + status +
                    ", scanned=" + scanned +
                    ", created=" + created +
                    ", updated=" + updated +
                    ", skipped=" + skipped +
                    (details == null ? "" : ", details=" + details);
        }
    }
}
