package com.civicplatform.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.keycloak.sync")
@Getter
@Setter
public class KeycloakSyncProperties {

    /**
     * Enable automatic periodic sync from Keycloak to local user table.
     */
    private boolean enabled = false;

    /**
     * Trigger one sync run when the application is ready.
     */
    private boolean runOnStartup = true;

    /**
     * Scheduler fixed-delay interval in milliseconds.
     */
    private long intervalMs = 300000;

    /**
     * Keycloak base URL used by the backend container (host-reachable).
     */
    private String baseUrl = "http://host.docker.internal:8180";

    /**
     * Target Keycloak realm name.
     */
    private String realm = "foodnexus";
    /**
     * Optional realm used only for token acquisition (e.g. "master" admin account).
     * If blank, defaults to {@link #realm}.
     */
    private String authRealm;

    /**
     * Client credentials for Keycloak admin API access.
     */
    private String clientId;
    private String clientSecret;
    /**
     * Optional fallback using Keycloak user credentials (password grant).
     */
    private String username;
    private String password;
    private String passwordClientId = "admin-cli";

    public String realmBaseUrl() {
        return stripTrailingSlash(baseUrl) + "/realms/" + realm;
    }

    public String adminUsersUrl() {
        return stripTrailingSlash(baseUrl) + "/admin/realms/" + realm + "/users";
    }

    public String tokenRealmBaseUrl() {
        String resolvedAuthRealm = (authRealm == null || authRealm.isBlank()) ? realm : authRealm;
        return stripTrailingSlash(baseUrl) + "/realms/" + resolvedAuthRealm;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }
}
