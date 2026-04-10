import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError, of } from 'rxjs';
import { Router } from '@angular/router';
import {
  User,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  RefreshTokenRequest,
  AccountType,
  UserType
} from '../models/auth.models';
import { environment } from '../../../environments/environment';

const STORAGE_KEYS = {
  TOKEN: 'auth_token',
  REFRESH_TOKEN: 'auth_refresh_token',
  USER: 'auth_user'
} as const;

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private currentUserSubject = new BehaviorSubject<User | null>(this.loadUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response) => this.handleAuthentication(response))
    );
  }

  register(userData: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, userData).pipe(
      tap((response) => this.handleAuthentication(response))
    );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEYS.TOKEN);
    localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
    localStorage.removeItem(STORAGE_KEYS.USER);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  /** Used by JWT interceptor — body matches backend refresh contract. */
  refreshToken(request: RefreshTokenRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, request).pipe(
      tap((response) => this.handleAuthentication(response))
    );
  }

  getToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.TOKEN);
  }

  /** @alias getToken — interceptor */
  getAccessToken(): string | null {
    return this.getToken();
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  updateCurrentUser(user: User): void {
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  /** @alias isAuthenticated */
  isLoggedIn(): boolean {
    return this.isAuthenticated();
  }

  isAdmin(): boolean {
    return this.getCurrentUser()?.isAdmin === true;
  }

  isRegularUser(): boolean {
    const u = this.getCurrentUser();
    return !!u && !u.isAdmin;
  }

  hasRole(role: string): boolean {
    const r = role.toUpperCase();
    if (r === 'ADMIN') {
      return this.isAdmin();
    }
    if (r === 'USER') {
      return this.isRegularUser();
    }
    return false;
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  isAmbassador(): boolean {
    return this.getCurrentUser()?.userType === UserType.AMBASSADOR;
  }

  isDonor(): boolean {
    return this.getCurrentUser()?.userType === UserType.DONOR;
  }

  isCitizen(): boolean {
    return this.getCurrentUser()?.userType === UserType.CITIZEN;
  }

  isParticipant(): boolean {
    return this.getCurrentUser()?.userType === UserType.PARTICIPANT;
  }

  /** Regular accounts only — DONOR or AMBASSADOR can create events/campaigns. */
  canCreateContent(): boolean {
    const u = this.getCurrentUser();
    if (!u || u.isAdmin) {
      return false;
    }
    return u.userType === UserType.DONOR || u.userType === UserType.AMBASSADOR;
  }

  refreshProfile(): Observable<User | null> {
    if (this.isAdmin()) {
      return of(this.getCurrentUser());
    }
    return this.http.get<User>(`${environment.apiUrl}/users/me`).pipe(
      tap((user) => {
        const prev = this.getCurrentUser();
        const merged: User =
          prev && prev.id === user.id
            ? { ...user, profilePictureRevision: prev.profilePictureRevision }
            : user;
        localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(merged));
        this.currentUserSubject.next(merged);
      }),
      catchError(() => of(this.getCurrentUser()))
    );
  }

  private handleAuthentication(response: AuthResponse): void {
    localStorage.setItem(STORAGE_KEYS.TOKEN, response.token);
    localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, response.refreshToken);

    const isAdmin =
      response.accountType === AccountType.ADMIN ||
      (response.accountType as string) === 'ADMIN';
    const accountType = isAdmin ? AccountType.ADMIN : AccountType.USER;

    const user: User = {
      id: response.userId,
      userName: response.userName,
      email: response.email,
      accountType,
      isAdmin,
      createdAt: response.createdAt as unknown as string,
      userType: isAdmin ? undefined : response.userType,
      badge: isAdmin ? undefined : response.badge,
      points: isAdmin ? undefined : response.points,
      awardedDate: isAdmin ? undefined : (response.awardedDate as unknown as string | undefined),
      badgeProgress: isAdmin ? undefined : response.badgeProgress,
      firstName: isAdmin ? undefined : response.firstName,
      lastName: isAdmin ? undefined : response.lastName,
      phone: isAdmin ? undefined : response.phone,
      address: isAdmin ? undefined : response.address,
      birthDate: isAdmin ? undefined : (response.birthDate as unknown as string | undefined),
      companyName: isAdmin ? undefined : response.companyName,
      associationName: isAdmin ? undefined : response.associationName,
      contactName: isAdmin ? undefined : response.contactName,
      contactEmail: isAdmin ? undefined : response.contactEmail,
      hasProfilePicture: isAdmin ? undefined : response.hasProfilePicture
    };

    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  private loadUserFromStorage(): User | null {
    const userJson = localStorage.getItem(STORAGE_KEYS.USER);
    if (!userJson) {
      return null;
    }
    try {
      const raw = JSON.parse(userJson) as Record<string, unknown>;
      if (typeof raw['isAdmin'] !== 'boolean') {
        raw['isAdmin'] = raw['accountType'] === AccountType.ADMIN || raw['accountType'] === 'ADMIN';
      }
      return raw as unknown as User;
    } catch {
      return null;
    }
  }
}
