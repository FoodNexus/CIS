import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, firstValueFrom, from, map, of, tap } from 'rxjs';
import {
  User,
  LoginRequest,
  RegisterRequest,
  AccountType,
  UserType
} from '../models/auth.models';
import { environment } from '../../../environments/environment';
import Keycloak, { KeycloakLoginOptions } from 'keycloak-js';

const STORAGE_KEYS = {
  USER: 'auth_user'
} as const;

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly profileUrl = `${environment.apiUrl}/users/me`;
  private readonly oidcScope = environment.keycloak.scope ?? 'openid profile email';
  private readonly oidcRedirectUri = window.location.origin;
  private readonly oidcPostLogoutRedirectUri = window.location.origin;
  private readonly keycloak = new Keycloak({
    url: environment.keycloak.url,
    realm: environment.keycloak.realm,
    clientId: environment.keycloak.clientId
  });
  private initialized = false;

  private currentUserSubject = new BehaviorSubject<User | null>(this.loadUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  async initializeAuth(): Promise<void> {
    if (this.initialized) {
      return;
    }
    this.initialized = true;

    const authenticated = await this.keycloak.init({
      onLoad: 'check-sso',
      flow: 'standard',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      redirectUri: this.oidcRedirectUri
    });

    if (!authenticated) {
      this.clearAuthState();
      return;
    }

    const tokenUser = this.buildUserFromToken();
    if (tokenUser) {
      this.updateCurrentUser(tokenUser);
    }
    await this.refreshProfileSnapshot();
  }

  login(credentials: LoginRequest): Observable<void> {
    const options: KeycloakLoginOptions = {
      redirectUri: this.oidcRedirectUri,
      scope: this.oidcScope
    };
    if (credentials?.email?.trim()) {
      (options as KeycloakLoginOptions & { loginHint?: string }).loginHint = credentials.email.trim();
    }
    return from(this.keycloak.login(options)).pipe(map(() => void 0));
  }

  register(userData: RegisterRequest): Observable<void> {
    const options: KeycloakLoginOptions = {
      action: 'register',
      redirectUri: this.oidcRedirectUri,
      scope: this.oidcScope
    };
    if (userData?.email?.trim()) {
      (options as KeycloakLoginOptions & { loginHint?: string }).loginHint = userData.email.trim();
    }
    return from(this.keycloak.login(options)).pipe(map(() => void 0));
  }

  loginRedirect(returnUrl?: string): void {
    const sanitized = returnUrl?.startsWith('/') ? returnUrl : '/';
    void this.keycloak.login({
      redirectUri: this.oidcRedirectUri + sanitized,
      scope: this.oidcScope
    });
  }

  logout(): void {
    this.clearAuthState();
    void this.keycloak.logout({ redirectUri: this.oidcPostLogoutRedirectUri });
  }

  getToken(): string | null {
    return this.keycloak.token ?? null;
  }

  /** @alias getToken — interceptor */
  getAccessToken(): string | null {
    return this.getToken();
  }

  async getValidAccessToken(): Promise<string | null> {
    if (!this.keycloak.authenticated) {
      return null;
    }
    try {
      await this.keycloak.updateToken(30);
      return this.keycloak.token ?? null;
    } catch {
      this.clearAuthState();
      return null;
    }
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  updateCurrentUser(user: User): void {
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  isAuthenticated(): boolean {
    return this.keycloak.authenticated === true;
  }

  /** @alias isAuthenticated */
  isLoggedIn(): boolean {
    return this.isAuthenticated();
  }

  isAdmin(): boolean {
    const fromProfile = this.getCurrentUser()?.isAdmin === true;
    return fromProfile || this.keycloak.hasRealmRole('ADMIN');
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
    if (!this.isLoggedIn()) {
      return of(null);
    }
    if (this.isAdmin()) {
      return of(this.getCurrentUser());
    }
    return this.http.get<User>(this.profileUrl).pipe(
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

  private async refreshProfileSnapshot(): Promise<void> {
    await firstValueFrom(this.refreshProfile());
  }

  private buildUserFromToken(): User | null {
    const claims = this.keycloak.tokenParsed as Record<string, unknown> | undefined;
    if (!claims) {
      return null;
    }
    const roles = this.extractRealmRoles(claims);
    const isAdmin = roles.includes('ADMIN');
    const preferredUsername = String(
      claims['preferred_username'] ?? claims['email'] ?? claims['sub'] ?? 'user'
    );
    const emailRaw = claims['email'];
    const email = typeof emailRaw === 'string' && emailRaw.trim()
      ? emailRaw
      : `${preferredUsername}@keycloak.local`;
    const userTypeClaim = String(claims['user_type'] ?? '').toUpperCase();
    const userType = Object.values(UserType).includes(userTypeClaim as UserType)
      ? (userTypeClaim as UserType)
      : undefined;

    return {
      id: Number(claims['local_user_id'] ?? -1),
      userName: preferredUsername,
      email,
      accountType: isAdmin ? AccountType.ADMIN : AccountType.USER,
      isAdmin,
      userType,
      createdAt: new Date().toISOString(),
      firstName: typeof claims['given_name'] === 'string' ? claims['given_name'] : undefined,
      lastName: typeof claims['family_name'] === 'string' ? claims['family_name'] : undefined
    };
  }

  private extractRealmRoles(claims: Record<string, unknown>): string[] {
    const realmAccess = claims['realm_access'];
    if (!realmAccess || typeof realmAccess !== 'object') {
      return [];
    }
    const roles = (realmAccess as { roles?: unknown }).roles;
    if (!Array.isArray(roles)) {
      return [];
    }
    return roles.map((r) => String(r).toUpperCase());
  }

  private clearAuthState(): void {
    localStorage.removeItem(STORAGE_KEYS.USER);
    this.currentUserSubject.next(null);
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
