package com.civicplatform.service;

public interface QrCodeService {

    /**
     * Generates a PNG image (300×300) of a QR code encoding the user's identity as plain text (CivicIdentity/v1 lines).
     */
    byte[] generateQrCode(Long userId);
}
