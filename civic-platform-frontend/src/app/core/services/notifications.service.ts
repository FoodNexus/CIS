import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AppNotification, SpringPage } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly base = `${environment.apiUrl}/notifications`;

  /** Emits after markRead / markAllRead succeed so the bell badge can refresh from any screen. */
  private readonly unreadCountInvalidated = new Subject<void>();
  readonly unreadCountInvalidated$ = this.unreadCountInvalidated.asObservable();

  constructor(private http: HttpClient) {}

  list(page = 0, size = 20): Observable<SpringPage<AppNotification>> {
    return this.http.get<SpringPage<AppNotification>>(this.base, {
      params: { page: String(page), size: String(size) }
    });
  }

  unreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.base}/unread-count`);
  }

  markRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}/read`, {}).pipe(
      tap(() => this.unreadCountInvalidated.next())
    );
  }

  markAllRead(): Observable<void> {
    return this.http.put<void>(`${this.base}/read-all`, {}).pipe(
      tap(() => this.unreadCountInvalidated.next())
    );
  }
}
