package com.civicplatform.service.impl;

import com.civicplatform.entity.Event;
import com.civicplatform.entity.User;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EventParticipantRepository eventParticipantRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void sendRegistrationEmail(String to, String userName) {
        String subject = "Welcome to Civic Platform!";
        String templateName = "email/registration";
        
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "platformName", "Civic Platform"
        );
        
        sendEmailWithTemplate(to, subject, templateName, variables);
    }

    @Override
    public void sendCampaignLaunchEmail(String to, String campaignName) {
        String subject = "New Campaign Launched: " + campaignName;
        String templateName = "email/campaign-launch";
        
        Map<String, Object> variables = Map.of(
            "campaignName", campaignName,
            "platformName", "Civic Platform"
        );
        
        sendEmailWithTemplate(to, subject, templateName, variables);
    }

    @Override
    public void sendProjectFundingEmail(String to, String projectName, String amount) {
        String subject = "Project Funded: " + projectName;
        String templateName = "email/project-funding";
        
        Map<String, Object> variables = Map.of(
            "projectName", projectName,
            "amount", amount,
            "platformName", "Civic Platform"
        );
        
        sendEmailWithTemplate(to, subject, templateName, variables);
    }

    @Override
    public void sendEventRegistrationEmail(String to, String eventTitle) {
        String subject = "Event Registration Confirmed: " + eventTitle;
        String templateName = "email/event-registration";
        
        Map<String, Object> variables = Map.of(
            "eventTitle", eventTitle,
            "platformName", "Civic Platform"
        );
        
        sendEmailWithTemplate(to, subject, templateName, variables);
    }

    @Override
    public void sendAmbassadorPromotionEmail(String to, String userName) {
        String subject = "Congratulations! You've been promoted to Ambassador!";
        String templateName = "email/ambassador-promotion";
        
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "badge", "COEUR",
            "platformName", "Civic Platform"
        );
        
        sendEmailWithTemplate(to, subject, templateName, variables);
    }

    @Override
    public void sendEmailWithTemplate(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            
            Context context = new Context();
            context.setVariables(variables);
            
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Email sent successfully to {} with subject: {}", to, subject);
            
        } catch (MessagingException e) {
            log.error("Failed to send email to {} with subject: {}", to, subject, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendCitizenEventInvitation(
            User citizen,
            Event event,
            User donor,
            String invitationToken,
            double matchScore,
            String publicBaseUrl) {
        try {
            Context ctx = new Context();
            String desc = event.getDescription() != null ? event.getDescription() : "";
            if (desc.length() > 200) {
                desc = desc.substring(0, 200) + "…";
            }
            String donorLabel = donor.getAssociationName();
            if (donorLabel == null || donorLabel.isBlank()) {
                donorLabel = donor.getContactName() != null ? donor.getContactName() : donor.getUserName();
            }
            String first = citizen.getFirstName();
            if (first == null || first.isBlank()) {
                first = citizen.getUserName();
            }
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            ctx.setVariable("citizenFirstName", first);
            ctx.setVariable("eventTitle", event.getTitle());
            ctx.setVariable("eventDescription", desc);
            ctx.setVariable("eventDate", event.getDate());
            String loc = event.getLocation();
            ctx.setVariable("eventLocation", (loc != null && !loc.isBlank()) ? loc : "—");
            ctx.setVariable("eventType", event.getType() != null ? event.getType().name() : "");
            ctx.setVariable("donorName", donorLabel);
            ctx.setVariable("matchScore", matchScore);
            /* matchScore is the 0–100 composite rate from invitation matching */
            ctx.setVariable("matchPercent", Math.min(100, Math.round(matchScore)));
            ctx.setVariable("acceptUrl", base + "/event-invitations/respond?token=" + invitationToken + "&response=ACCEPTED");
            ctx.setVariable("declineUrl", base + "/event-invitations/respond?token=" + invitationToken + "&response=DECLINED");
            ctx.setVariable("emailDate", DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH).format(java.time.LocalDate.now()));

            String body = templateEngine.process("email/citizen-event-invitation", ctx);
            sendHtmlEmail(citizen.getEmail(),
                    "You are invited to the event: " + event.getTitle(),
                    body);
        } catch (Exception e) {
            log.error("sendCitizenEventInvitation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to send citizen event invitation email", e);
        }
    }

    @Override
    public void sendCitizenAcceptedNotification(
            User donor,
            User citizen,
            Event event,
            double matchScore,
            String publicBaseUrl) {
        try {
            Context ctx = new Context();
            String donorFirst = donor.getFirstName();
            if (donorFirst == null || donorFirst.isBlank()) {
                donorFirst = donor.getContactName() != null ? donor.getContactName() : "Hello";
            }
            ctx.setVariable("donorFirstName", donorFirst);
            ctx.setVariable("citizenFullName",
                    (citizen.getFirstName() != null ? citizen.getFirstName() : "")
                            + " "
                            + (citizen.getLastName() != null ? citizen.getLastName() : ""));
            ctx.setVariable("eventTitle", event.getTitle());
            ctx.setVariable("badge", citizen.getBadge() != null ? citizen.getBadge().name() : "NONE");
            ctx.setVariable("matchScore", matchScore);
            ctx.setVariable("eventId", event.getId());
            ctx.setVariable("frontendUrl", frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl);
            int eventsAttended = eventParticipantRepository.countCompletedByUserId(citizen.getId());
            ctx.setVariable("eventsAttended", eventsAttended);
            ctx.setVariable("emailDate", DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH).format(java.time.LocalDate.now()));

            String body = templateEngine.process("email/citizen-event-accepted", ctx);
            sendHtmlEmail(donor.getEmail(),
                    "A citizen accepted your event invitation — " + event.getTitle(),
                    body);
        } catch (Exception e) {
            log.error("sendCitizenAcceptedNotification failed: {}", e.getMessage());
            throw new RuntimeException("Failed to send citizen accepted email", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) throws jakarta.mail.MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
        log.info("HTML email sent successfully to {} with subject: {}", to, subject);
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Simple email sent successfully to {} with subject: {}", to, subject);
            
        } catch (Exception e) {
            log.error("Failed to send simple email to {} with subject: {}", to, subject, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
