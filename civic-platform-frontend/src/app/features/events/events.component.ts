import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { EventsService, Event } from '@core/services/events.service';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './events.component.html',
  styleUrls: ['./events.component.scss']
})
export class EventsComponent implements OnInit {
  events: Event[] = [];
  isLoading = false;
  errorMessage = '';

  constructor(
    private eventsService: EventsService,
    private authService: AuthService,
    private router: Router
  ) {}

  canCreateEvent(): boolean {
    return this.authService.canCreateContent();
  }

  showNewEventButton(): boolean {
    return this.canCreateEvent() || (this.isAdminRoute() && this.authService.isAdmin());
  }

  newEventPath(): string {
    return this.isAdminRoute() ? '/admin/events/new' : '/events/new';
  }

  isAdminRoute(): boolean {
    return this.router.url.split('?')[0].startsWith('/admin');
  }

  eventDetailLink(id: number): (string | number)[] {
    return this.isAdminRoute() ? ['/admin/events', id] : ['/events', id];
  }

  ngOnInit(): void {
    this.loadEvents();
  }

  loadEvents(): void {
    this.isLoading = true;
    this.eventsService.getAllEvents().subscribe({
      next: (events) => {
        this.events = events;
        this.isLoading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to load events';
        this.isLoading = false;
      }
    });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'UPCOMING':
        return 'bg-emerald-100 text-emerald-900';
      case 'ONGOING':
        return 'bg-green-100 text-green-900';
      case 'COMPLETED':
        return 'bg-slate-100 text-slate-700';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-slate-100 text-slate-700';
    }
  }

  dashboardLink(): string {
    return this.isAdminRoute() ? '/admin/dashboard' : '/dashboard';
  }

  eventTypeLabel(type: string): string {
    if (!type) return '';
    return type.charAt(0) + type.slice(1).toLowerCase();
  }
}
