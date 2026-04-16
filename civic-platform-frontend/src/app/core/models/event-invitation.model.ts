export interface EventInvitation {
  invitationId: number;
  eventId: number;
  eventTitle: string;
  citizenId: number;
  citizenFullName: string;
  citizenUserType: string;
  citizenBadge: string;
  citizenEventsAttended: number;
  matchScore: number;
  /** Normalized 0–100 composite rate (multi-feature). */
  compositeRate?: number | null;
  matchScorePercent: number;
  invitationTier?: string | null;
  priorityFollowup?: boolean;
  featureBreakdownJson?: string | null;
  status: 'INVITED' | 'ACCEPTED' | 'DECLINED' | 'NO_RESPONSE';
  invitedAt: string;
  respondedAt: string | null;
  invitationToken?: string | null;
  donorAssociationName?: string | null;
}
