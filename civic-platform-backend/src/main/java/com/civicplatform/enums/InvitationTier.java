package com.civicplatform.enums;

/**
 * Result of rate-driven business logic: who gets a direct invite vs. an alternative nurture path.
 */
public enum InvitationTier {
    /** High composite rate — immediate invite + priority follow-up list for the donor. */
    PRIORITY_IMMEDIATE,
    /** Standard invite channel. */
    STANDARD_INVITE,
    /** Rate below invite threshold — no direct invite; alternative engagement (e.g. suggested event). */
    NURTURE_ALTERNATIVE
}
