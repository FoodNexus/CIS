export enum UserType {
  AMBASSADOR = 'AMBASSADOR',
  DONOR = 'DONOR',
  CITIZEN = 'CITIZEN',
  PARTICIPANT = 'PARTICIPANT'
}

export enum Role {
  USER = 'USER',
  ADMIN = 'ADMIN'
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
  userType: UserType;
  role: Role;
  badge: Badge;
  points: number;
  awardedDate?: string;
  createdAt: string;
  badgeProgress?: BadgeProgressInfo;
  
  // Profile fields
  firstName?: string;
  lastName?: string;
  phone?: string;
  address?: string;
  birthDate?: string;
  
  // DONOR fields
  companyName?: string;
  associationName?: string;
  contactName?: string;
  contactEmail?: string;
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
  
  // Common fields
  firstName: string;
  lastName: string;
  phone: string;
  address: string;
  
  // DONOR-specific fields
  companyName?: string;
  associationName?: string;
  contactName?: string;
  contactEmail?: string;
  
  // CITIZEN-specific fields
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
  userType: UserType;
  role: Role;
  badge: Badge;
  points: number;
  awardedDate?: string;
  createdAt: string;
  
  // Additional user fields
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
}
