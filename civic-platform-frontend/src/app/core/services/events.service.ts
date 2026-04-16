import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export enum EventType {
  VISITE = 'VISITE',
  FORMATION = 'FORMATION',
  DISTRIBUTION = 'DISTRIBUTION',
  COLLECTE = 'COLLECTE'
}

export enum EventStatus {
  UPCOMING = 'UPCOMING',
  ONGOING = 'ONGOING',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export interface Event {
  id: number;
  title: string;
  description?: string;
  type: EventType;
  status: EventStatus;
  date: string;
  location?: string;
  maxCapacity?: number;
  currentParticipants?: number;
  organizerId?: number;
  organizerName?: string;
  createdAt?: string;
  availableSpots?: number;
  capacityPercentage?: number;
  /** Set when returned from the ML recommendation feed. */
  isRecommended?: boolean;
}

export interface EventRegistrationStatus {
  registered: boolean;
  status: string | null;
}

export interface EventParticipation {
  id: number;
  registeredAt: string;
  checkedInAt?: string;
  status: string;
  eventId: number;
  eventTitle: string;
  eventDate: string;
  eventLocation?: string;
  userId: number;
  userName: string;
  userEmail: string;
}

export interface EventRequest {
  title: string;
  description?: string;
  type: EventType;
  date: string;
  location?: string;
  maxCapacity?: number;
}

@Injectable({ providedIn: 'root' })
export class EventsService {
  private readonly API_URL = `${environment.apiUrl}/events`;

  constructor(private http: HttpClient) {}

  getAllEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(this.API_URL);
  }

  getEventById(id: number): Observable<Event> {
    return this.http.get<Event>(`${this.API_URL}/${id}`);
  }

  createEvent(eventData: EventRequest): Observable<Event> {
    return this.http.post<Event>(this.API_URL, eventData);
  }

  updateEvent(id: number, eventData: Partial<EventRequest>): Observable<Event> {
    return this.http.put<Event>(`${this.API_URL}/${id}`, eventData);
  }

  transitionEventStatus(id: number, status: EventStatus): Observable<Event> {
    return this.http.patch<Event>(`${this.API_URL}/${id}/status`, { status });
  }

  deleteEvent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  registerForEvent(id: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${id}/register`, {});
  }

  cancelRegistration(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}/register`);
  }

  attendEvent(id: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${id}/attend`, {});
  }

  getMyParticipations(): Observable<EventParticipation[]> {
    return this.http.get<EventParticipation[]>(`${this.API_URL}/my-participations`);
  }

  getRegistrationStatus(eventId: number): Observable<EventRegistrationStatus> {
    return this.http.get<EventRegistrationStatus>(`${this.API_URL}/${eventId}/registration`);
  }

  downloadParticipationCertificate(eventId: number, userId: number): Observable<Blob> {
    return this.http.get(
      `${this.API_URL}/${eventId}/attendance/${userId}/certificate/pdf`,
      { responseType: 'blob' }
    );
  }
}
