package com.civicplatform.service.impl;

import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final float PAGE_W = 595f;
    private static final float PAGE_H = 842f;
    private static final float MARGIN = 40f;
    private static final float HEADER_H = 60f;
    private static final float FOOTER_H = 30f;
    private static final float ROW_H = 18f;
    private static final float SECTION_TITLE_H = 24f;
    private static final float SECTION_GAP = 14f;
    /** ID, full name, username, email, created at */
    private static final float[] COL_WIDTHS = {38f, 132f, 88f, 198f, 74f};
    private static final float Y_FLOOR = FOOTER_H + 28f;

    private final UserRepository userRepository;

    private static String pdfSafe(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\u2192', '-').replace('\u2014', '-').replace('\u2013', '-');
        StringBuilder out = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            out.append(c <= 0xFF ? c : '?');
        }
        return out.toString();
    }

    @Override
    public byte[] generateReport(LocalDate from, LocalDate to, UserType type, String format) {
        if (from.isAfter(to)) {
            throw new IllegalStateException("Invalid date range: 'from' must be before or equal to 'to'");
        }
        String f = format != null ? format.trim().toLowerCase(Locale.ROOT) : "";
        if (!"pdf".equals(f) && !"csv".equals(f)) {
            throw new IllegalStateException("Format must be 'pdf' or 'csv'");
        }

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59, 999_999_999);
        List<User> users = userRepository.findByDateRangeAndType(fromDt, toDt, type);

        if ("csv".equals(f)) {
            return generateCsv(users, from, to, type);
        }
        return generatePdf(users, from, to, type);
    }

    private record GroupedUsers(EnumMap<UserType, List<User>> byType, List<User> unknownType) {}

    private GroupedUsers groupUsersByType(List<User> users) {
        EnumMap<UserType, List<User>> map = new EnumMap<>(UserType.class);
        for (UserType ut : UserType.values()) {
            map.put(ut, new ArrayList<>());
        }
        List<User> unknown = new ArrayList<>();
        for (User u : users) {
            if (u.getUserType() == null) {
                unknown.add(u);
            } else {
                map.get(u.getUserType()).add(u);
            }
        }
        Comparator<User> byId = Comparator.comparing(User::getId, Comparator.nullsLast(Long::compareTo));
        for (List<User> list : map.values()) {
            list.sort(byId);
        }
        unknown.sort(byId);
        return new GroupedUsers(map, unknown);
    }

    private byte[] generateCsv(List<User> users, LocalDate from, LocalDate to, UserType type) {
        String typeLabel = type != null ? type.name() : "ALL";
        String today = LocalDate.now().toString();
        StringBuilder sb = new StringBuilder();
        sb.append("Civic Engagement Platform - User Report\n");
        sb.append("Period: ").append(from).append(" to ").append(to).append('\n');
        sb.append("Type filter: ").append(typeLabel).append('\n');
        sb.append("Generated on: ").append(today).append('\n');
        sb.append("Total records: ").append(users.size()).append('\n');
        sb.append('\n');

        GroupedUsers grouped = groupUsersByType(users);
        sb.append("Columns: ID, Full name, Username, Email, Created at\n\n");

        for (UserType ut : UserType.values()) {
            List<User> list = grouped.byType().get(ut);
            if (list == null || list.isEmpty()) {
                continue;
            }
            sb.append("=== ").append(ut.name()).append(" (").append(list.size()).append(") ===\n");
            sb.append("ID,Full name,Username,Email,Created at\n");
            for (User u : list) {
                sb.append(csvCell(u.getId())).append(',');
                sb.append(csvCell(fullName(u))).append(',');
                sb.append(csvCell(u.getUserName())).append(',');
                sb.append(csvCell(u.getEmail())).append(',');
                sb.append(csvCell(u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""));
                sb.append('\n');
            }
            sb.append('\n');
        }
        if (!grouped.unknownType().isEmpty()) {
            sb.append("=== UNSPECIFIED TYPE (").append(grouped.unknownType().size()).append(") ===\n");
            sb.append("ID,Full name,Username,Email,Created at\n");
            for (User u : grouped.unknownType()) {
                sb.append(csvCell(u.getId())).append(',');
                sb.append(csvCell(fullName(u))).append(',');
                sb.append(csvCell(u.getUserName())).append(',');
                sb.append(csvCell(u.getEmail())).append(',');
                sb.append(csvCell(u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""));
                sb.append('\n');
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvCell(Object value) {
        String s = value == null ? "" : String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private byte[] generatePdf(List<User> users, LocalDate from, LocalDate to, UserType type) {
        try {
            return generatePdfInternal(users, from, to, type);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate report PDF", e);
        }
    }

    private byte[] generatePdfInternal(List<User> users, LocalDate from, LocalDate to, UserType type) throws IOException {
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        GroupedUsers grouped = groupUsersByType(users);

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int[] pageNum = {0};

            if (users.isEmpty()) {
                PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
                doc.addPage(page);
                pageNum[0]++;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    drawPageHeader(cs, bold, type, pageNum[0]);
                    drawReportMeta(cs, regular, from, to, users.size(), true);
                    float y = PAGE_H - HEADER_H - 100f;
                    cs.setNonStrokingColor(0.392f, 0.455f, 0.545f);
                    cs.beginText();
                    cs.setFont(regular, 12f);
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText(pdfSafe("No data found"));
                    cs.endText();
                    drawPageFooter(cs, regular, pageNum[0]);
                }
            } else {
                PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
                doc.addPage(page);
                pageNum[0]++;
                PdfState st = new PdfState();
                st.cs = new PDPageContentStream(doc, page);
                drawPageHeader(st.cs, bold, type, pageNum[0]);
                drawReportMeta(st.cs, regular, from, to, users.size(), true);
                st.y = PAGE_H - HEADER_H - 118f;

                for (UserType ut : UserType.values()) {
                    List<User> sectionUsers = grouped.byType().get(ut);
                    if (sectionUsers == null || sectionUsers.isEmpty()) {
                        continue;
                    }
                    st = writeUserTypeSection(doc, st, bold, regular, type, pageNum, ut, sectionUsers);
                }
                if (!grouped.unknownType().isEmpty()) {
                    st = writeUserTypeSection(doc, st, bold, regular, type, pageNum, null, grouped.unknownType());
                }

                drawPageFooter(st.cs, regular, pageNum[0]);
                st.cs.close();

                pageNum[0]++;
                PDPage summaryPage = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
                doc.addPage(summaryPage);
                try (PDPageContentStream cs2 = new PDPageContentStream(doc, summaryPage)) {
                    drawPageHeader(cs2, bold, type, pageNum[0]);
                    drawSummaryPage(cs2, bold, regular, users);
                    drawPageFooter(cs2, regular, pageNum[0]);
                }
            }

            doc.save(out);
            return out.toByteArray();
        }
    }

    private static final class PdfState {
        PDPageContentStream cs;
        float y;
    }

    /**
     * Writes one user-type block (or unknown if {@code ut} is null). Handles pagination.
     */
    private PdfState writeUserTypeSection(PDDocument doc, PdfState state, PDType1Font bold, PDType1Font regular,
                                          UserType filterType, int[] pageNum, UserType ut, List<User> sectionUsers)
            throws IOException {
        PDPageContentStream cs = state.cs;
        float y = state.y;
        String sectionLabel = ut != null ? ut.name() : "UNSPECIFIED TYPE";

        if (y < Y_FLOOR + SECTION_TITLE_H + ROW_H + ROW_H) {
            drawPageFooter(cs, regular, pageNum[0]);
            cs.close();
            PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
            doc.addPage(page);
            pageNum[0]++;
            cs = new PDPageContentStream(doc, page);
            drawPageHeader(cs, bold, filterType, pageNum[0]);
            drawContinuationBanner(cs, regular);
            y = PAGE_H - HEADER_H - 48f;
        }

        y = drawSectionTitle(cs, bold, sectionLabel, sectionUsers.size(), y);
        y -= 6f;
        drawTableHeaderSlim(cs, bold, y);
        y -= ROW_H;

        int rowIdx = 0;
        for (User u : sectionUsers) {
            if (y < Y_FLOOR + ROW_H) {
                drawPageFooter(cs, regular, pageNum[0]);
                cs.close();
                PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
                doc.addPage(page);
                pageNum[0]++;
                cs = new PDPageContentStream(doc, page);
                drawPageHeader(cs, bold, filterType, pageNum[0]);
                drawContinuationBanner(cs, regular);
                y = PAGE_H - HEADER_H - 48f;
                y = drawSectionTitle(cs, bold, sectionLabel + " (suite)", sectionUsers.size(), y);
                y -= 6f;
                drawTableHeaderSlim(cs, bold, y);
                y -= ROW_H;
            }
            drawDataRowSlim(cs, regular, u, y, rowIdx % 2 == 0);
            y -= ROW_H;
            rowIdx++;
        }
        y -= SECTION_GAP;
        PdfState out = new PdfState();
        out.cs = cs;
        out.y = y;
        return out;
    }

    private void drawContinuationBanner(PDPageContentStream cs, PDType1Font regular) throws IOException {
        cs.setNonStrokingColor(0.55f, 0.58f, 0.62f);
        cs.beginText();
        cs.setFont(regular, 10f);
        cs.newLineAtOffset(MARGIN, PAGE_H - HEADER_H - 28f);
        cs.showText(pdfSafe("Suite du rapport"));
        cs.endText();
    }

    private float drawSectionTitle(PDPageContentStream cs, PDType1Font bold, String title, int count, float y)
            throws IOException {
        cs.setNonStrokingColor(0.90f, 0.96f, 0.93f);
        cs.addRect(MARGIN, y - SECTION_TITLE_H, PAGE_W - 2 * MARGIN, SECTION_TITLE_H);
        cs.fill();
        cs.setNonStrokingColor(0.06f, 0.45f, 0.32f);
        cs.beginText();
        cs.setFont(bold, 11f);
        cs.newLineAtOffset(MARGIN + 8f, y - SECTION_TITLE_H + 7f);
        cs.showText(pdfSafe(title + "  -  " + count + " utilisateur(s)"));
        cs.endText();
        return y - SECTION_TITLE_H - 4f;
    }

    private void drawTableHeaderSlim(PDPageContentStream cs, PDType1Font bold, float topY) throws IOException {
        float x = MARGIN;
        cs.setNonStrokingColor(0.118f, 0.161f, 0.231f);
        cs.addRect(MARGIN, topY - ROW_H, PAGE_W - 2 * MARGIN, ROW_H);
        cs.fill();
        cs.setNonStrokingColor(1f, 1f, 1f);
        String[] headers = {"ID", "Nom complet", "Username", "Email", "Inscrit le"};
        float fs = 9f;
        for (int i = 0; i < headers.length; i++) {
            cs.beginText();
            cs.setFont(bold, fs);
            cs.newLineAtOffset(x + 2f, topY - ROW_H + 5f);
            cs.showText(pdfSafe(headers[i]));
            cs.endText();
            x += COL_WIDTHS[i];
        }
    }

    private void drawDataRowSlim(PDPageContentStream cs, PDType1Font regular, User u, float rowY, boolean evenRow)
            throws IOException {
        float x = MARGIN;
        cs.setNonStrokingColor(evenRow ? 1f : 0.973f, evenRow ? 1f : 0.980f, evenRow ? 1f : 0.988f);
        cs.addRect(MARGIN, rowY - ROW_H, PAGE_W - 2 * MARGIN, ROW_H);
        cs.fill();

        String fullName = fullName(u);
        String email = u.getEmail() != null ? u.getEmail() : "";
        String created = u.getCreatedAt() != null
                ? u.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH))
                : "";

        float fs = 9f;
        cs.setNonStrokingColor(0.118f, 0.161f, 0.231f);
        drawCell(cs, regular, fs, pdfSafe(String.valueOf(u.getId())), x, rowY, COL_WIDTHS[0]);
        x += COL_WIDTHS[0];
        drawCell(cs, regular, fs, pdfSafe(fullName), x, rowY, COL_WIDTHS[1]);
        x += COL_WIDTHS[1];
        drawCell(cs, regular, fs, pdfSafe(u.getUserName()), x, rowY, COL_WIDTHS[2]);
        x += COL_WIDTHS[2];
        drawCell(cs, regular, fs, pdfSafe(email), x, rowY, COL_WIDTHS[3]);
        x += COL_WIDTHS[3];
        drawCell(cs, regular, fs, pdfSafe(created), x, rowY, COL_WIDTHS[4]);
    }

    private static void drawCell(PDPageContentStream cs, PDType1Font font, float fs, String text,
                                 float x, float rowY, float colW) throws IOException {
        cs.setNonStrokingColor(0.118f, 0.161f, 0.231f);
        cs.beginText();
        cs.setFont(font, fs);
        cs.newLineAtOffset(x + 2f, rowY - ROW_H + 5f);
        cs.showText(ellipsize(font, fs, text != null ? text : "", colW - 4f));
        cs.endText();
    }

    private static String ellipsize(PDType1Font font, float size, String text, float maxW) throws IOException {
        if (text == null) {
            return "";
        }
        String ellipsis = "...";
        float ew = font.getStringWidth(ellipsis) / 1000f * size;
        if (font.getStringWidth(text) / 1000f * size <= maxW) {
            return text;
        }
        String t = text;
        while (t.length() > 0 && font.getStringWidth(t) / 1000f * size > maxW - ew) {
            t = t.substring(0, t.length() - 1);
        }
        return t + ellipsis;
    }

    private static String fullName(User u) {
        String fn = u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u.getLastName() != null ? u.getLastName() : "";
        String c = (fn + " " + ln).trim();
        return c.isEmpty() ? (u.getUserName() != null ? u.getUserName() : "") : c;
    }

    private void drawPageHeader(PDPageContentStream cs, PDType1Font bold, UserType type, int pageNum) throws IOException {
        cs.setNonStrokingColor(0.145f, 0.388f, 0.922f);
        cs.addRect(0, PAGE_H - HEADER_H, PAGE_W, HEADER_H);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        String left = "Civic Engagement Platform";
        float fs = 14f;
        cs.beginText();
        cs.setFont(bold, fs);
        cs.newLineAtOffset(MARGIN, PAGE_H - HEADER_H / 2f - fs / 3f);
        cs.showText(pdfSafe(left));
        cs.endText();

        String typeOrAll = type != null ? type.name() : "ALL";
        String right = "User Report — " + typeOrAll;
        String rightPdf = pdfSafe(right);
        float rw = bold.getStringWidth(rightPdf) / 1000f * fs;
        cs.beginText();
        cs.setFont(bold, fs);
        cs.newLineAtOffset(PAGE_W - MARGIN - rw, PAGE_H - HEADER_H / 2f - fs / 3f);
        cs.showText(rightPdf);
        cs.endText();
    }

    private void drawReportMeta(PDPageContentStream cs, PDType1Font regular, LocalDate from, LocalDate to,
                                int count, boolean drawMeta) throws IOException {
        if (!drawMeta) {
            return;
        }
        float y = PAGE_H - HEADER_H - 28f;
        cs.setNonStrokingColor(0.392f, 0.455f, 0.545f);
        float fs = 11f;
        cs.beginText();
        cs.setFont(regular, fs);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(pdfSafe("Période : " + from + " → " + to));
        cs.endText();
        y -= 16f;
        cs.beginText();
        cs.setFont(regular, fs);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(pdfSafe("Généré le : " + LocalDate.now()));
        cs.endText();
        y -= 16f;
        cs.beginText();
        cs.setFont(regular, fs);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(pdfSafe("Total utilisateurs : " + count));
        cs.endText();
    }

    private void drawPageFooter(PDPageContentStream cs, PDType1Font regular, int pageNum) throws IOException {
        cs.setNonStrokingColor(0.945f, 0.961f, 0.976f);
        cs.addRect(0, 0, PAGE_W, FOOTER_H);
        cs.fill();
        cs.setNonStrokingColor(0.580f, 0.639f, 0.722f);
        float fs = 8f;
        String left = "Civic Engagement Platform — Rapport confidentiel";
        String leftPdf = pdfSafe(left);
        cs.beginText();
        cs.setFont(regular, fs);
        cs.newLineAtOffset(MARGIN, 10f);
        cs.showText(leftPdf);
        cs.endText();
        String right = "Page " + pageNum;
        float rw = regular.getStringWidth(right) / 1000f * fs;
        cs.beginText();
        cs.setFont(regular, fs);
        cs.newLineAtOffset(PAGE_W - MARGIN - rw, 10f);
        cs.showText(right);
        cs.endText();
    }

    private void drawSummaryPage(PDPageContentStream cs, PDType1Font bold, PDType1Font regular, List<User> users)
            throws IOException {
        float y = PAGE_H - HEADER_H - 40f;
        cs.setNonStrokingColor(0.118f, 0.161f, 0.231f);
        cs.beginText();
        cs.setFont(bold, 18f);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(pdfSafe("Résumé du rapport"));
        cs.endText();
        y -= 40f;

        int total = users.size();
        long amb = users.stream().filter(u -> u.getUserType() == UserType.AMBASSADOR).count();
        long don = users.stream().filter(u -> u.getUserType() == UserType.DONOR).count();
        long cit = users.stream().filter(u -> u.getUserType() == UserType.CITIZEN).count();
        long part = users.stream().filter(u -> u.getUserType() == UserType.PARTICIPANT).count();

        EnumMap<Badge, Integer> dist = new EnumMap<>(Badge.class);
        for (Badge b : Badge.values()) {
            dist.put(b, 0);
        }
        for (User u : users) {
            Badge b = u.getBadge() != null ? u.getBadge() : Badge.NONE;
            dist.merge(b, 1, Integer::sum);
        }

        float boxW = 86f;
        float boxH = 50f;
        float gap = 10f;
        float bx = MARGIN;

        drawStatBox(cs, bold, regular, bx, y, boxW, boxH, 0.145f, 0.388f, 0.922f, "Total utilisateurs", String.valueOf(total));
        bx += boxW + gap;
        drawStatBox(cs, bold, regular, bx, y, boxW, boxH, 0.133f, 0.694f, 0.298f, "Ambassadeurs", String.valueOf(amb));
        bx += boxW + gap;
        drawStatBox(cs, bold, regular, bx, y, boxW, boxH, 0.961f, 0.620f, 0.043f, "Donateurs", String.valueOf(don));
        bx += boxW + gap;
        drawStatBox(cs, bold, regular, bx, y, boxW, boxH, 0.545f, 0.361f, 0.965f, "Citoyens", String.valueOf(cit));
        bx += boxW + gap;
        drawStatBox(cs, bold, regular, bx, y, boxW, boxH, 0.086f, 0.639f, 0.604f, "Participants", String.valueOf(part));

        y -= boxH + 36f;
        cs.setNonStrokingColor(0.392f, 0.455f, 0.545f);
        cs.beginText();
        cs.setFont(regular, 11f);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(pdfSafe("PLATINUM: " + dist.get(Badge.PLATINUM)
                + " | GOLD: " + dist.get(Badge.GOLD)
                + " | SILVER: " + dist.get(Badge.SILVER)
                + " | BRONZE: " + dist.get(Badge.BRONZE)
                + " | NONE: " + dist.get(Badge.NONE)));
        cs.endText();
    }

    private void drawStatBox(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
                             float x, float y, float w, float h, float r, float g, float b,
                             String label, String value) throws IOException {
        cs.setNonStrokingColor(r, g, b);
        cs.addRect(x, y - h, w, h);
        cs.fill();
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.beginText();
        cs.setFont(regular, 9f);
        cs.newLineAtOffset(x + 8f, y - 18f);
        cs.showText(pdfSafe(label));
        cs.endText();
        cs.beginText();
        cs.setFont(bold, 16f);
        cs.newLineAtOffset(x + 8f, y - 38f);
        cs.showText(pdfSafe(value));
        cs.endText();
    }
}
