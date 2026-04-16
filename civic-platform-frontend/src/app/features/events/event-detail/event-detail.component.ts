import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { EventInvitationService } from '@core/services/event-invitation.service';
import { EventInvitation } from '@core/models/event-invitation.model';
import { EventsService, Event, EventStatus } from '@core/services/events.service';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.scss']
})
export class EventDetailComponent implements OnInit {
  readonly EventStatus = EventStatus;

  event: Event | null = null;
  isLoading = false;
  errorMessage = '';
  registrationLoading = false;
  actionMessage = '';

  statusLoading = false;

  isRegistered = false;
  registrationStatus: string | null = null;

  showDeleteModal = false;
  deleteLoading = false;

  /** Organizer: view automatically matched invitees */
  eventTab: 'details' | 'invitations' = 'details';
  invitations: EventInvitation[] = [];
  invitationsLoading = false;
  invitationsError = '';
  rematchLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventsService: EventsService,
    private authService: AuthService,
    private eventInvitationService: EventInvitationService
  ) {}

  isAdminRoute(): boolean {
    return this.router.url.split('?')[0].startsWith('/admin');
  }

  eventsListPath(): string {
    return this.isAdminRoute() ? '/admin/events' : '/events';
  }

  /** Platform admins review events; they do not register as participants. */
  showParticipantRegistration(): boolean {
    return !this.authService.isAdmin();
  }

  isPlatformAdmin(): boolean {
    return this.authService.isAdmin();
  }

  /** Organizer or platform admin may change event lifecycle. */
  canManageEventLifecycle(): boolean {
    return this.isOrganizer() || this.isPlatformAdmin();
  }

  editEventLink(): (string | number)[] {
    if (!this.event) {
      return [this.eventsListPath()];
    }
    return this.isAdminRoute()
      ? ['/admin/events', this.event.id, 'edit']
      : ['/events', this.event.id, 'edit'];
  }

  openDeleteModal(): void {
    this.showDeleteModal = true;
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
  }

  confirmDelete(): void {
    if (!this.event) {
      return;
    }
    this.deleteLoading = true;
    this.eventsService.deleteEvent(this.event.id).subscribe({
      next: () => {
        this.router.navigateByUrl(this.eventsListPath());
      },
      error: (err) => {
        this.actionMessage = err.error?.message || 'Could not delete event';
        this.deleteLoading = false;
        this.showDeleteModal = false;
      }
    });
  }

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
        if (this.showParticipantRegistration()) {
          this.loadRegistrationStatus(id);
        } else {
          this.isRegistered = false;
          this.registrationStatus = null;
        }
        if (this.isOrganizer() && this.eventTab === 'invitations') {
          this.loadInvitations(id);
        }
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

  isOrganizer(): boolean {
    if (!this.event?.organizerId) return false;
    const u = this.authService.getCurrentUser();
    return u != null && u.id === this.event.organizerId;
  }

  transitionStatus(status: EventStatus): void {
    if (!this.event) return;
    this.statusLoading = true;
    this.actionMessage = '';
    this.eventsService.transitionEventStatus(this.event.id, status).subscribe({
      next: (ev) => {
        this.event = ev;
        this.statusLoading = false;
        this.actionMessage = 'Event status updated.';
        if (this.showParticipantRegistration()) {
          this.loadRegistrationStatus(ev.id);
        }
      },
      error: (err) => {
        this.statusLoading = false;
        this.actionMessage = err.error?.message || 'Could not update status';
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

  setEventTab(tab: 'details' | 'invitations'): void {
    this.eventTab = tab;
    if (tab === 'invitations' && this.event?.id) {
      this.loadInvitations(this.event.id);
    }
  }

  loadInvitations(eventId: number): void {
    this.invitationsLoading = true;
    this.invitationsError = '';
    this.eventInvitationService.getEventInvitations(eventId).subscribe({
      next: (rows) => {
        this.invitations = rows;
        this.invitationsLoading = false;
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.invitationsError =
          err.status === 403
            ? 'Access denied.'
            : err.error?.message || 'Could not load invitations.';
        this.invitationsLoading = false;
      }
    });
  }

  runRematch(): void {
    if (!this.event?.id) {
      return;
    }
    this.rematchLoading = true;
    this.invitationsError = '';
    this.eventInvitationService.triggerRematch(this.event.id).subscribe({
      next: (rows) => {
        this.invitations = rows;
        this.rematchLoading = false;
      },
      error: () => {
        this.rematchLoading = false;
        this.invitationsError = 'Could not run matching again.';
      }
    });
  }

  invitationScoreLabel(m: EventInvitation): string {
    if (m.compositeRate != null && m.compositeRate !== undefined) {
      return `${m.compositeRate.toFixed(1)} / 100`;
    }
    return `${Math.round(m.matchScore)} pts`;
  }

  scoreBarClass(percent: number): string {
    if (percent <= 40) {
      return 'bg-red-500';
    }
    if (percent <= 70) {
      return 'bg-amber-500';
    }
    return 'bg-emerald-500';
  }

  invitationStatusClass(status: string): string {
    switch (status) {
      case 'INVITED':
        return 'bg-blue-100 text-blue-800 border border-blue-200';
      case 'ACCEPTED':
        return 'bg-emerald-100 text-emerald-800';
      case 'DECLINED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  }
}
