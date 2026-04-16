import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EventInvitation } from '@core/models/event-invitation.model';

@Injectable({ providedIn: 'root' })
export class EventInvitationService {
  private readonly base = `${environment.apiUrl}/event-invitations`;

  constructor(private http: HttpClient) {}

  getEventInvitations(eventId: number): Observable<EventInvitation[]> {
    return this.http.get<EventInvitation[]>(
      `${this.base}/events/${eventId}/invitations`
    );
  }

  getMyInvitations(): Observable<EventInvitation[]> {
    return this.http.get<EventInvitation[]>(`${this.base}/my-invitations`);
  }

  triggerRematch(eventId: number): Observable<EventInvitation[]> {
    return this.http.post<EventInvitation[]>(
      `${this.base}/events/${eventId}/rematch`,
      {}
    );
  }

  respond(
    token: string,
    response: 'ACCEPTED' | 'DECLINED'
  ): Observable<{
    status: string;
    message: string;
    eventTitle: string;
  }> {
    return this.http.get<{
      status: string;
      message: string;
      eventTitle: string;
    }>(`${this.base}/respond`, {
      params: { token, response }
    });
  }
}
