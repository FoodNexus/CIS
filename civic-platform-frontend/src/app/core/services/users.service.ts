import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserType } from '../models/auth.models';
import { environment } from '../../../environments/environment';

export interface AdminUserPayload {
  userName?: string;
  email?: string;
  password?: string;
  userType?: UserType;
  firstName?: string;
  lastName?: string;
  phone?: string;
  address?: string;
  companyName?: string;
  associationName?: string;
  contactName?: string;
  contactEmail?: string;
  birthDate?: string;
  points?: number;
  badge?: string;
}

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly baseUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.baseUrl);
  }

  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/${id}`);
  }

  createUser(userData: AdminUserPayload): Observable<User> {
    return this.http.post<User>(this.baseUrl, userData);
  }

  updateUser(id: number, userData: AdminUserPayload): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/${id}`, userData);
  }

  updateProfile(id: number, profileData: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/${id}/profile`, profileData);
  }

  updateMyProfile(profileData: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/me/profile`, profileData);
  }

  uploadProfilePicture(id: number, file: File): Observable<User> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<User>(`${this.baseUrl}/${id}/profile-picture`, formData);
  }

  uploadMyProfilePicture(file: File): Observable<User> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<User>(`${this.baseUrl}/me/profile-picture`, formData);
  }

  /** Public image URL (add cache-bust query when needed). */
  profilePictureUrl(userId: number, revision?: number): string {
    const v = revision != null ? `?v=${revision}` : '';
    return `${this.baseUrl}/${userId}/profile-picture${v}`;
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  downloadQrCodePng(userId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${userId}/qrcode`, { responseType: 'blob' });
  }

  /** Admin: resolve account by email (for QR payloads that omit user id). */
  getUserByEmailForAdmin(email: string): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/email/${encodeURIComponent(email)}`);
  }
}
