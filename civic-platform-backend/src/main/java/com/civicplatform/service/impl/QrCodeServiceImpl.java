package com.civicplatform.service.impl;

import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.QrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

    private static final int QR_SIZE = 300;

    private final UserRepository userRepository;

    /**
     * Plain-text, line-oriented payload (never JSON). Does not include numeric user id — use email + admin lookup to resolve the account.
     */
    @Override
    public byte[] generateQrCode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        String payload = buildIdentityPayload(user);

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code image", e);
        }
    }

    static String buildIdentityPayload(User user) {
        String badge = user.getBadge() != null ? user.getBadge().name() : Badge.NONE.name();
        return String.join("\n",
                "CivicIdentity/v2",
                "userName: " + oneLine(user.getUserName()),
                "userType: " + user.getUserType().name(),
                "badge: " + badge,
                "email: " + oneLine(user.getEmail())
        );
    }

    private static String oneLine(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
