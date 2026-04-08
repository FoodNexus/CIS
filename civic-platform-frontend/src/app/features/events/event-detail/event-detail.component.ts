import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { EventsService, Event } from '@core/services/events.service';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.scss']
})
export class EventDetailComponent implements OnInit {
  event: Event | null = null;
  isLoading = false;
  errorMessage = '';
  registrationLoading = false;
  actionMessage = '';

  isRegistered = false;
  registrationStatus: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private eventsService: EventsService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadEvent(Number(id));
    }
  }

  loadEvent(id: number): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.eventsService.getEventById(id).subscribe({
      next: (event) => {
        this.event = event;
        this.isLoading = false;
        this.loadRegistrationStatus(id);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to load event';
        this.isLoading = false;
      }
    });
  }

  private loadRegistrationStatus(eventId: number): void {
    this.eventsService.getRegistrationStatus(eventId).subscribe({
      next: (s) => {
        this.isRegistered = s.registered;
        this.registrationStatus = s.status;
      },
      error: () => {
        this.isRegistered = false;
        this.registrationStatus = null;
      }
    });
  }

  register(): void {
    if (!this.event) return;
    this.registrationLoading = true;
    this.actionMessage = '';
    this.eventsService.registerForEvent(this.event.id).subscribe({
      next: () => {
        this.registrationLoading = false;
        this.isRegistered = true;
        this.registrationStatus = 'REGISTERED';
        this.actionMessage = 'You are registered for this event.';
        this.loadEvent(this.event.id);
      },
      error: (error) => {
        this.registrationLoading = false;
        this.actionMessage = error.error?.message || 'Could not register';
      }
    });
  }

  cancelRegistration(): void {
    if (!this.event) return;
    this.registrationLoading = true;
    this.actionMessage = '';
    this.eventsService.cancelRegistration(this.event.id).subscribe({
      next: () => {
        this.registrationLoading = false;
        this.isRegistered = false;
        this.registrationStatus = null;
        this.actionMessage = 'Registration cancelled.';
        this.loadEvent(this.event.id);
      },
      error: (error) => {
        this.registrationLoading = false;
        this.actionMessage = error.error?.message || 'Could not cancel';
      }
    });
  }

  getStatusLabel(): string {
    if (!this.registrationStatus) return '';
    if (this.registrationStatus === 'COMPLETED') return 'Attended';
    if (this.registrationStatus === 'REGISTERED') return 'Registered';
    if (this.registrationStatus === 'CHECKED_IN') return 'Checked in';
    return this.registrationStatus.replace(/_/g, ' ');
  }
}
