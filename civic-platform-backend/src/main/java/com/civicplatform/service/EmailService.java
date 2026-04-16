package com.civicplatform.service;

import com.civicplatform.entity.Event;
import com.civicplatform.entity.User;

import java.util.Map;

public interface EmailService {
    void sendRegistrationEmail(String to, String userName);
    void sendCampaignLaunchEmail(String to, String campaignName);
    void sendProjectFundingEmail(String to, String projectName, String amount);
    void sendEventRegistrationEmail(String to, String eventTitle);
    void sendAmbassadorPromotionEmail(String to, String userName);
    void sendEmailWithTemplate(String to, String subject, String templateName, Map<String, Object> variables);

    void sendCitizenEventInvitation(
            User citizen,
            Event event,
            User donor,
            String invitationToken,
            double matchScore,
            String publicBaseUrl);

    void sendCitizenAcceptedNotification(
            User donor,
            User citizen,
            Event event,
            double matchScore,
            String publicBaseUrl);
}
