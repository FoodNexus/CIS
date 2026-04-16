package com.civicplatform.dto.rate;

import com.civicplatform.enums.InvitationTier;
import lombok.Builder;
import lombok.Value;

/**
 * Transformed output from composite rate + features (advanced business logic).
 */
@Value
@Builder
public class InvitationRateDecision {
    InvitationTier tier;
    /** When true, donor UI may highlight for follow-up (priority list). */
    boolean priorityFollowup;
    /**
     * Derived channel / timing hint: priority invites use the high-attention path
     * (immediate email + in-app); standard uses the regular path.
     */
    String engagementChannel;
    /** For {@link InvitationTier#NURTURE_ALTERNATIVE}: suggested other upcoming event id, if any. */
    Long nurtureSuggestedEventId;
    String nurtureSuggestedEventTitle;
}
