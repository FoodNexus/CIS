package com.civicplatform.service.impl;

import com.civicplatform.dto.response.DashboardStatsResponse;
import com.civicplatform.entity.Campaign;
import com.civicplatform.entity.Project;
import com.civicplatform.service.DashboardService;
import com.civicplatform.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final DashboardService dashboardService;

    private static final float MARGIN = 48f;
    private static final float FONT_SIZE = 11f;
    private static final float TITLE_FONT_SIZE = 20f;
    private static final float LINE_HEIGHT = 14f;
    private static final float HEADER_H = 88f;
    /** Emerald theme (matches app) */
    private static final float HDR_R = 0.02f;
    private static final float HDR_G = 0.59f;
    private static final float HDR_B = 0.41f;

    @Override
    public ByteArrayOutputStream generateCampaignReport(Campaign campaign) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = page.getMediaBox().getHeight() - MARGIN;

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TITLE_FONT_SIZE);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText(pdfSafe("Campaign Report: " + campaign.getName()));
                contentStream.endText();
                yPosition -= LINE_HEIGHT * 2;

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
                yPosition = addTextLine(contentStream, pdfSafe("Campaign ID: " + campaign.getId()), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("Type: " + campaign.getType()), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("Status: " + campaign.getStatus()), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("Created: " + campaign.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("Start Date: " + (campaign.getStartDate() != null ? campaign.getStartDate() : "N/A")), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("End Date: " + (campaign.getEndDate() != null ? campaign.getEndDate() : "N/A")), yPosition);

                yPosition -= LINE_HEIGHT;
                yPosition = addTextLine(contentStream, pdfSafe("Goals:"), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("  - Target KG: " + (campaign.getGoalKg() != null ? campaign.getGoalKg() : "N/A")), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("  - Current KG: " + (campaign.getCurrentKg() != null ? campaign.getCurrentKg() : 0)), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("  - Target Meals: " + (campaign.getGoalMeals() != null ? campaign.getGoalMeals() : "N/A")), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("  - Current Meals: " + (campaign.getCurrentMeals() != null ? campaign.getCurrentMeals() : 0)), yPosition);

                if (campaign.getGoalAmount() != null) {
                    yPosition = addTextLine(contentStream, pdfSafe("  - Target Amount: $" + campaign.getGoalAmount()), yPosition);
                }

                if (campaign.getDescription() != null && !campaign.getDescription().isEmpty()) {
                    yPosition -= LINE_HEIGHT;
                    yPosition = addTextLine(contentStream, pdfSafe("Description:"), yPosition);
                    String[] descriptionLines = wrapText(campaign.getDescription(), 80);
                    for (String line : descriptionLines) {
                        yPosition = addTextLine(contentStream, pdfSafe("  " + line), yPosition);
                    }
                }

                if (campaign.getHashtag() != null && !campaign.getHashtag().isEmpty()) {
                    yPosition = addTextLine(contentStream, pdfSafe("Hashtag: " + campaign.getHashtag()), yPosition);
                }

                yPosition = MARGIN + 30;
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                addTextLine(contentStream, pdfSafe("Generated on: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)), yPosition);
            }

            document.save(outputStream);
            return outputStream;

        } catch (IOException e) {
            log.error("Error generating campaign PDF report", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    @Override
    public ByteArrayOutputStream generateProjectReport(Project project) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = page.getMediaBox().getHeight() - MARGIN;

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TITLE_FONT_SIZE);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText(pdfSafe("Project Report: " + project.getTitle()));
                contentStream.endText();
                yPosition -= LINE_HEIGHT * 2;

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
                yPosition = addTextLine(contentStream, pdfSafe("Project ID: " + project.getId()), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("Status: " + project.getStatus()), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("Created: " + project.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)), yPosition);

                if (project.getStartDate() != null) {
                    yPosition = addTextLine(contentStream, pdfSafe("Start Date: " + project.getStartDate()), yPosition);
                }

                if (project.getCompletionDate() != null) {
                    yPosition = addTextLine(contentStream, pdfSafe("Completion Date: " + project.getCompletionDate()), yPosition);
                }

                yPosition -= LINE_HEIGHT;
                yPosition = addTextLine(contentStream, pdfSafe("Funding:"), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("  - Goal Amount: $" + project.getGoalAmount()), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("  - Current Amount: $" + project.getCurrentAmount()), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("  - Vote Count: " + project.getVoteCount()), yPosition);
                yPosition = addTextLine(contentStream, pdfSafe("  - Progress: " + project.getFundingPercentage() + "%"), yPosition);

                if (project.getOrganizerType() != null) {
                    yPosition = addTextLine(contentStream, pdfSafe("Organizer Type: " + project.getOrganizerType()), yPosition);
                }

                if (project.getDescription() != null && !project.getDescription().isEmpty()) {
                    yPosition -= LINE_HEIGHT;
                    yPosition = addTextLine(contentStream, pdfSafe("Description:"), yPosition);
                    String[] descriptionLines = wrapText(project.getDescription(), 80);
                    for (String line : descriptionLines) {
                        yPosition = addTextLine(contentStream, pdfSafe("  " + line), yPosition);
                    }
                }

                if (project.getFinalReport() != null && !project.getFinalReport().isEmpty()) {
                    yPosition -= LINE_HEIGHT;
                    yPosition = addTextLine(contentStream, pdfSafe("Final Report:"), yPosition);
                    String[] reportLines = wrapText(project.getFinalReport(), 80);
                    for (String line : reportLines) {
                        yPosition = addTextLine(contentStream, pdfSafe("  " + line), yPosition);
                    }
                }

                yPosition = MARGIN + 30;
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                addTextLine(contentStream, pdfSafe("Generated on: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)), yPosition);
            }

            document.save(outputStream);
            return outputStream;

        } catch (IOException e) {
            log.error("Error generating project PDF report", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    @Override
    public ByteArrayOutputStream generateMetricsReport() {
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);
            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // --- Header band ---
                cs.setNonStrokingColor(HDR_R, HDR_G, HDR_B);
                cs.addRect(0, pageH - HEADER_H, pageW, HEADER_H);
                cs.fill();

                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.beginText();
                cs.setFont(bold, 22f);
                cs.newLineAtOffset(MARGIN, pageH - HEADER_H + 52f);
                cs.showText(pdfSafe("Impact Metrics"));
                cs.endText();
                cs.beginText();
                cs.setFont(regular, 11f);
                cs.newLineAtOffset(MARGIN, pageH - HEADER_H + 30f);
                cs.showText(pdfSafe("Civic Platform  -  Administrative overview"));
                cs.endText();

                float y = pageH - HEADER_H - 28f;

                cs.setNonStrokingColor(0.45f, 0.48f, 0.52f);
                cs.beginText();
                cs.setFont(regular, 10f);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(pdfSafe("Generated " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
                cs.endText();
                y -= 28f;

                drawRule(cs, MARGIN, pageW - MARGIN, y);
                y -= 20f;

                // --- KPI grid (2 columns x 3 rows) ---
                String funding = formatMoney(stats.getTotalFundingAmount());
                String[][] kpi = {
                        {"Total funding", funding},
                        {"Total projects", nz(stats.getTotalProjects())},
                        {"Total events", nz(stats.getTotalEvents())},
                        {"Active volunteers", nz(stats.getActiveVolunteers())},
                        {"Active donors", nz(stats.getActiveDonors())},
                        {"Associations (donor type)", nz(stats.getActiveAssociations())}
                };
                float gap = 12f;
                float cardW = (pageW - 2 * MARGIN - gap) / 2f;
                float cardH = 56f;
                float rowY = y;
                for (int i = 0; i < kpi.length; i++) {
                    float x = MARGIN + (i % 2) * (cardW + gap);
                    drawMetricCard(cs, bold, regular, x, rowY, cardW, cardH, kpi[i][0], kpi[i][1]);
                    if (i % 2 == 1) {
                        rowY -= cardH + gap;
                    }
                }
                y = rowY - 8f;

                // --- Secondary stats ---
                y -= 6f;
                cs.setNonStrokingColor(0.12f, 0.14f, 0.18f);
                cs.beginText();
                cs.setFont(bold, 13f);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(pdfSafe("Environmental snapshot"));
                cs.endText();
                y -= 22f;

                String co2 = stats.getTotalCo2Saved() != null ? stats.getTotalCo2Saved().toPlainString() : "0";
                String meals = stats.getTotalMealsDistributed() != null ? String.valueOf(stats.getTotalMealsDistributed()) : "0";
                String region = stats.getMostActiveRegion() != null ? stats.getMostActiveRegion() : "N/A";
                y = addLabelValueLine(cs, regular, MARGIN, y, "CO2 saved (kg, model)", co2);
                y = addLabelValueLine(cs, regular, MARGIN, y, "Meals distributed (model)", meals);
                y = addLabelValueLine(cs, regular, MARGIN, y, "Most active region", region);
                y -= 16f;

                drawRule(cs, MARGIN, pageW - MARGIN, y);
                y -= 20f;

                cs.beginText();
                cs.setFont(bold, 13f);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(pdfSafe("Users by type"));
                cs.endText();
                y -= 20f;
                Map<String, Long> usersByType = stats.getTotalUsersByType() != null
                        ? stats.getTotalUsersByType()
                        : Collections.emptyMap();
                for (Map.Entry<String, Long> e : usersByType.entrySet()) {
                    y = addLabelValueLine(cs, regular, MARGIN + 8f, y, e.getKey(), String.valueOf(e.getValue()));
                }
                y -= 12f;

                drawRule(cs, MARGIN, pageW - MARGIN, y);
                y -= 20f;

                cs.beginText();
                cs.setFont(bold, 13f);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(pdfSafe("Campaigns by status"));
                cs.endText();
                y -= 20f;
                Map<String, Long> camps = stats.getTotalCampaignsByStatus() != null
                        ? stats.getTotalCampaignsByStatus()
                        : Collections.emptyMap();
                for (Map.Entry<String, Long> e : camps.entrySet()) {
                    y = addLabelValueLine(cs, regular, MARGIN + 8f, y, e.getKey(), String.valueOf(e.getValue()));
                }

                // Footer
                float fy = MARGIN;
                cs.setNonStrokingColor(0.65f, 0.68f, 0.72f);
                cs.beginText();
                cs.setFont(regular, 9f);
                cs.newLineAtOffset(MARGIN, fy);
                cs.showText(pdfSafe("Civic Platform  -  Confidential  -  metrics-export.pdf"));
                cs.endText();
            }

            document.save(outputStream);
            return outputStream;

        } catch (IOException e) {
            log.error("Error generating metrics PDF report", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private static void drawRule(PDPageContentStream cs, float x1, float x2, float y) throws IOException {
        cs.setStrokingColor(0.85f, 0.88f, 0.91f);
        cs.setLineWidth(0.75f);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    private static void drawMetricCard(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
                                       float x, float yTop, float w, float h,
                                       String label, String value) throws IOException {
        cs.setNonStrokingColor(0.97f, 0.98f, 0.99f);
        cs.addRect(x, yTop - h, w, h);
        cs.fill();
        cs.setStrokingColor(0.88f, 0.91f, 0.93f);
        cs.setLineWidth(0.5f);
        cs.addRect(x, yTop - h, w, h);
        cs.stroke();

        cs.setNonStrokingColor(0.42f, 0.45f, 0.5f);
        cs.beginText();
        cs.setFont(regular, 9f);
        cs.newLineAtOffset(x + 10f, yTop - 20f);
        cs.showText(pdfSafe(label.toUpperCase(Locale.ROOT)));
        cs.endText();

        cs.setNonStrokingColor(0.08f, 0.1f, 0.12f);
        cs.beginText();
        cs.setFont(bold, 15f);
        cs.newLineAtOffset(x + 10f, yTop - 42f);
        cs.showText(pdfSafe(value));
        cs.endText();
    }

    private static float addLabelValueLine(PDPageContentStream cs, PDType1Font regular, float x, float y,
                                          String label, String value) throws IOException {
        cs.setNonStrokingColor(0.35f, 0.38f, 0.42f);
        cs.beginText();
        cs.setFont(regular, 10f);
        cs.newLineAtOffset(x, y);
        cs.showText(pdfSafe(label));
        cs.endText();
        float labelW = regular.getStringWidth(pdfSafe(label)) / 1000f * 10f;
        cs.setNonStrokingColor(0.1f, 0.12f, 0.15f);
        cs.beginText();
        cs.setFont(regular, 10f);
        cs.newLineAtOffset(x + Math.max(200f, labelW + 24f), y);
        cs.showText(pdfSafe(value));
        cs.endText();
        return y - 16f;
    }

    private static String nz(Long n) {
        return n != null ? String.valueOf(n) : "0";
    }

    private static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return "$" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Standard 14 fonts: keep text in Latin-1 / safe subset for PDFBox {@code showText}.
     */
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

    private float addTextLine(PDPageContentStream contentStream, String text, float yPosition) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        return yPosition - LINE_HEIGHT;
    }

    private String[] wrapText(String text, int maxCharsPerLine) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxCharsPerLine) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    for (int i = 0; i < word.length(); i += maxCharsPerLine) {
                        int endIndex = Math.min(i + maxCharsPerLine, word.length());
                        lines.add(word.substring(i, endIndex));
                    }
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }
}
