package com.civicplatform.service.impl;

import com.civicplatform.entity.Event;
import com.civicplatform.entity.EventParticipant;
import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.enums.ParticipantStatus;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.repository.EventRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.CertificateService;
import com.civicplatform.service.QrCodeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private static final float PAGE_W = 842f;
    private static final float PAGE_H = 595f;
    private static final float MARGIN = 48f;
    private static final int HEADER_H = 80;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final QrCodeService qrCodeService;

    @Override
    public byte[] generateCertificate(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));
        User participant = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Optional<EventParticipant> participantRow = eventParticipantRepository.findByEventIdAndUserId(eventId, userId);
        if (participantRow.isEmpty()) {
            throw new EntityNotFoundException("Attendance record not found");
        }
        if (participantRow.get().getStatus() != ParticipantStatus.COMPLETED) {
            throw new IllegalStateException("Certificate can only be generated for completed participations");
        }

        byte[] qrPng = qrCodeService.generateQrCode(userId);

        PDType1Font helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
            document.addPage(page);

            PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, qrPng, "qr");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float yTop = PAGE_H;

                cs.setNonStrokingColor(0.145f, 0.388f, 0.922f);
                cs.addRect(0, PAGE_H - HEADER_H, PAGE_W, HEADER_H);
                cs.fill();

                cs.setNonStrokingColor(1f, 1f, 1f);
                String headerTitle = "ATTESTATION DE PARTICIPATION";
                float titleSize = 24f;
                float titleW = helveticaBold.getStringWidth(headerTitle) / 1000f * titleSize;
                cs.beginText();
                cs.setFont(helveticaBold, titleSize);
                cs.newLineAtOffset((PAGE_W - titleW) / 2f, PAGE_H - HEADER_H / 2f - titleSize / 3f);
                cs.showText(headerTitle);
                cs.endText();

                yTop = PAGE_H - HEADER_H - 28f;
                cs.setNonStrokingColor(0.392f, 0.455f, 0.545f);
                String platform = "Civic Engagement Platform";
                float platSize = 14f;
                float platW = helvetica.getStringWidth(platform) / 1000f * platSize;
                cs.beginText();
                cs.setFont(helvetica, platSize);
                cs.newLineAtOffset((PAGE_W - platW) / 2f, yTop);
                cs.showText(platform);
                cs.endText();

                yTop -= 28f;
                cs.setStrokingColor(0.886f, 0.910f, 0.941f);
                cs.moveTo(MARGIN, yTop);
                cs.lineTo(PAGE_W - MARGIN, yTop);
                cs.stroke();

                yTop -= 36f;
                cs.setNonStrokingColor(0.392f, 0.455f, 0.545f);
                centerText(cs, helvetica, 13f, "Nous certifions que", yTop);

                yTop -= 36f;
                String fullName = formatFullName(participant);
                cs.setNonStrokingColor(0.118f, 0.161f, 0.231f);
                float nameSize = 28f;
                float nw = helveticaBold.getStringWidth(fullName) / 1000f * nameSize;
                cs.beginText();
                cs.setFont(helveticaBold, nameSize);
                cs.newLineAtOffset((PAGE_W - nw) / 2f, yTop);
                cs.showText(fullName);
                cs.endText();

                yTop -= 40f;
                cs.setNonStrokingColor(0.392f, 0.455f, 0.545f);
                centerText(cs, helvetica, 13f, "a particip\u00e9 \u00e0 l'\u00e9v\u00e9nement", yTop);

                yTop -= 36f;
                String eventTitle = event.getTitle() != null ? event.getTitle().toUpperCase(Locale.FRENCH) : "";
                float evSize = 20f;
                float evW = helveticaBold.getStringWidth(eventTitle) / 1000f * evSize;
                cs.setNonStrokingColor(0.145f, 0.388f, 0.922f);
                cs.beginText();
                cs.setFont(helveticaBold, evSize);
                cs.newLineAtOffset((PAGE_W - Math.min(evW, PAGE_W - 2 * MARGIN)) / 2f, yTop);
                cs.showText(truncate(eventTitle, 80));
                cs.endText();

                yTop -= 36f;
                cs.setNonStrokingColor(0.392f, 0.455f, 0.545f);
                String dateFr = event.getDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
                centerText(cs, helvetica, 13f, "le " + dateFr, yTop);

                yTop -= 28f;
                String loc = event.getLocation() != null ? event.getLocation() : "\u2014";
                centerText(cs, helvetica, 12f, "Lieu : " + loc, yTop);

                Badge badge = participant.getBadge() != null ? participant.getBadge() : Badge.NONE;
                if (badge != Badge.NONE) {
                    yTop -= 44f;
                    float[] rgb = badgeRgb(badge);
                    float pillW = 280f;
                    float pillH = 28f;
                    float pillX = (PAGE_W - pillW) / 2f;
                    cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
                    cs.addRect(pillX, yTop - pillH, pillW, pillH);
                    cs.fill();
                    cs.setNonStrokingColor(1f, 1f, 1f);
                    String badgeText = "Badge obtenu : " + badge.name();
                    float bt = 11f;
                    float btw = helveticaBold.getStringWidth(badgeText) / 1000f * bt;
                    cs.beginText();
                    cs.setFont(helveticaBold, bt);
                    cs.newLineAtOffset((PAGE_W - btw) / 2f, yTop - pillH + 8f);
                    cs.showText(badgeText);
                    cs.endText();
                }

                float qrSize = 80f;
                float qrX = PAGE_W - MARGIN - qrSize;
                float qrY = MARGIN + 70f;
                cs.drawImage(qrImage, qrX, qrY, qrSize, qrSize);

                cs.setNonStrokingColor(0.580f, 0.639f, 0.722f);
                String verif = "V\u00e9rification en ligne";
                float vs = 8f;
                float vw = helvetica.getStringWidth(verif) / 1000f * vs;
                cs.beginText();
                cs.setFont(helvetica, vs);
                cs.newLineAtOffset(qrX + (qrSize - vw) / 2f, qrY - 14f);
                cs.showText(verif);
                cs.endText();

                float footerH = 50f;
                cs.setNonStrokingColor(0.945f, 0.961f, 0.976f);
                cs.addRect(0, 0, PAGE_W, footerH);
                cs.fill();

                String organizerLabel = resolveOrganizerName(event.getOrganizerId());
                LocalDate today = LocalDate.now();
                String todayStr = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH));

                cs.setNonStrokingColor(0.392f, 0.455f, 0.545f);
                float fs = 9f;
                cs.beginText();
                cs.setFont(helvetica, fs);
                cs.newLineAtOffset(MARGIN, 18f);
                cs.showText("D\u00e9livr\u00e9 le " + todayStr);
                cs.endText();

                String centerFooter = "Document officiel \u2014 Civic Platform";
                float cw = helvetica.getStringWidth(centerFooter) / 1000f * fs;
                cs.beginText();
                cs.setFont(helvetica, fs);
                cs.newLineAtOffset((PAGE_W - cw) / 2f, 18f);
                cs.showText(centerFooter);
                cs.endText();

                String right = "Organis\u00e9 par : " + organizerLabel;
                float rw = helvetica.getStringWidth(right) / 1000f * fs;
                cs.beginText();
                cs.setFont(helvetica, fs);
                cs.newLineAtOffset(PAGE_W - MARGIN - rw, 18f);
                cs.showText(truncate(right, 60));
                cs.endText();

                float signY = footerH + 24f;
                cs.setNonStrokingColor(0.392f, 0.455f, 0.545f);
                cs.beginText();
                cs.setFont(helvetica, 10f);
                cs.newLineAtOffset(MARGIN, signY);
                cs.showText("Signature de l'organisateur");
                cs.endText();

                cs.setStrokingColor(0.6f, 0.6f, 0.6f);
                cs.setLineDashPattern(new float[]{2, 3}, 0);
                cs.moveTo(MARGIN, signY - 8f);
                cs.lineTo(MARGIN + 200f, signY - 8f);
                cs.stroke();
                cs.setLineDashPattern(new float[]{}, 0);

                cs.beginText();
                cs.setFont(helvetica, 10f);
                cs.newLineAtOffset(PAGE_W - MARGIN - 160f, signY);
                cs.showText("Cachet officiel");
                cs.endText();

                cs.setStrokingColor(0.6f, 0.6f, 0.6f);
                cs.addRect(PAGE_W - MARGIN - 160f, signY - 48f, 80f, 40f);
                cs.stroke();
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate certificate PDF", e);
        }
    }

    private static void centerText(PDPageContentStream cs, PDType1Font font, float size, String text, float y)
            throws IOException {
        float w = font.getStringWidth(text) / 1000f * size;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset((PAGE_W - w) / 2f, y);
        cs.showText(text);
        cs.endText();
    }

    private static String formatFullName(User u) {
        String fn = u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u.getLastName() != null ? u.getLastName() : "";
        String combined = (fn + " " + ln).trim();
        if (combined.isEmpty()) {
            combined = u.getUserName() != null ? u.getUserName() : "PARTICIPANT";
        }
        return combined.toUpperCase(Locale.FRENCH);
    }

    private String resolveOrganizerName(Long organizerId) {
        return userRepository.findById(organizerId)
                .map(o -> {
                    String fn = o.getFirstName() != null ? o.getFirstName() : "";
                    String ln = o.getLastName() != null ? o.getLastName() : "";
                    String n = (fn + " " + ln).trim();
                    if (!n.isEmpty()) {
                        return n;
                    }
                    return o.getUserName() != null ? o.getUserName() : "";
                })
                .orElse("\u2014");
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }

    private static float[] badgeRgb(Badge badge) {
        return switch (badge) {
            case BRONZE -> new float[]{0.804f, 0.498f, 0.196f};
            case SILVER -> new float[]{0.753f, 0.753f, 0.753f};
            case GOLD -> new float[]{1f, 0.843f, 0f};
            case PLATINUM -> new float[]{0.899f, 0.894f, 0.886f};
            default -> new float[]{0.6f, 0.6f, 0.6f};
        };
    }
}
