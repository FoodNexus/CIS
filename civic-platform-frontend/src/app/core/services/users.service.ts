import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserType } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly API_URL = 'http://localhost:8081/api/users';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.API_URL);
  }

  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${this.API_URL}/${id}`);
  }

  updateUser(id: number, userData: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${id}`, userData);
  }

  updateProfile(id: number, profileData: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${id}/profile`, profileData);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  downloadQrCodePng(userId: number): Observable<Blob> {
    return this.http.get(`${this.API_URL}/${userId}/qrcode`, { responseType: 'blob' });
  }
}
