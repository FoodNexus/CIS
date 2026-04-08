package com.civicplatform.service;

public interface CertificateService {

    /**
     * PDF attestation for a user who has completed participation in an event.
     */
    byte[] generateCertificate(Long eventId, Long userId);
}
