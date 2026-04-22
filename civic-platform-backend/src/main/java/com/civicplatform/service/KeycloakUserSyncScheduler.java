package com.civicplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserSyncScheduler {

    private final KeycloakUserSyncService keycloakUserSyncService;
    private final KeycloakSyncProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void runInitialSyncIfEnabled() {
        if (!properties.isEnabled() || !properties.isRunOnStartup()) {
            return;
        }
        log.info("Running startup Keycloak user sync...");
        keycloakUserSyncService.syncUsers();
    }

    @Scheduled(
            initialDelayString = "${app.keycloak.sync.interval-ms:300000}",
            fixedDelayString = "${app.keycloak.sync.interval-ms:300000}"
    )
    public void runScheduledSync() {
        if (!properties.isEnabled()) {
            return;
        }
        keycloakUserSyncService.syncUsers();
    }
}
