export enum UserType {
  AMBASSADOR = 'AMBASSADOR',
  DONOR = 'DONOR',
  CITIZEN = 'CITIZEN',
  PARTICIPANT = 'PARTICIPANT'
}

/** Platform account kind — separate from user_type (regular users only). */
export enum AccountType {
  ADMIN = 'ADMIN',
  USER = 'USER'
}

export enum Badge {
  NONE = 'NONE',
  BRONZE = 'BRONZE',
  SILVER = 'SILVER',
  GOLD = 'GOLD',
  PLATINUM = 'PLATINUM'
}

export interface BadgeProgressInfo {
  current_badge: string;
  events_attended: number;
  next_badge: string | null;
  events_for_next: number | null;
  events_remaining: number | null;
}

export interface User {
  id: number;
  userName: string;
  email: string;
  accountType: AccountType;
  /** True for seeded platform admins only. */
  isAdmin: boolean;
  /** Set for regular accounts only (CITIZEN | PARTICIPANT | AMBASSADOR | DONOR). */
  userType?: UserType;
  badge?: Badge;
  points?: number;
  awardedDate?: string;
  createdAt: string;
  badgeProgress?: BadgeProgressInfo;

  firstName?: string;
  lastName?: string;
  phone?: string;
  address?: string;
  birthDate?: string;

  companyName?: string;
  associationName?: string;
  contactName?: string;
  contactEmail?: string;

  /** From API — user has uploaded a profile image. */
  hasProfilePicture?: boolean;
  /** Client-only cache-bust for avatar URLs after upload (not from API). */
  profilePictureRevision?: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  userName: string;
  email: string;
  password: string;
  userType: UserType.CITIZEN | UserType.DONOR;

  firstName: string;
  lastName: string;
  phone: string;
  address: string;

  companyName?: string;
  associationName?: string;
  contactName?: string;
  contactEmail?: string;

  birthDate?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  userId: number;
  userName: string;
  email: string;
  accountType: AccountType;

  userType?: UserType;
  badge?: Badge;
  points?: number;
  awardedDate?: string;
  createdAt: string;

  firstName?: string;
  lastName?: string;
  phone?: string;
  address?: string;
  birthDate?: string;
  companyName?: string;
  associationName?: string;
  contactName?: string;
  contactEmail?: string;

  badgeProgress?: BadgeProgressInfo;

  hasProfilePicture?: boolean;
}
