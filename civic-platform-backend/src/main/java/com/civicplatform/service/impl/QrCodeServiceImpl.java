package com.civicplatform.service.impl;

import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.QrCodeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

    private static final int QR_SIZE = 300;

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public byte[] generateQrCode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user.getId());
        payload.put("userName", user.getUserName());
        payload.put("userType", user.getUserType().name());
        payload.put("badge", user.getBadge() != null ? user.getBadge().name() : Badge.NONE.name());
        payload.put("email", user.getEmail());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build QR JSON payload", e);
        }

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(json, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code image", e);
        }
    }
}
