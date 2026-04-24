import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EventsService, EventType } from '@core/services/events.service';

@Component({
  selector: 'app-event-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './event-form.component.html'
})
export class EventFormComponent implements OnInit {
  private readonly minLeadHours = 3;
  eventForm: FormGroup;
  eventTypes = Object.values(EventType);
  isEdit = false;
  eventId: number | null = null;
  isLoading = false;
  submitLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    public router: Router,
    private route: ActivatedRoute,
    private eventsService: EventsService
  ) {
    this.eventForm = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      date: ['', Validators.required],
      location: [''],
      type: [EventType.VISITE, Validators.required],
      maxCapacity: [100, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const path = this.route.snapshot.routeConfig?.path ?? '';
    if (id && path.includes('edit')) {
      this.isEdit = true;
      this.eventId = Number(id);
      this.loadEvent(this.eventId);
    }
  }

  isAdminRoute(): boolean {
    return this.router.url.split('?')[0].startsWith('/admin');
  }

  /** Display labels in English (values remain backend enum strings). */
  eventTypeLabel(t: EventType): string {
    const labels: Record<EventType, string> = {
      [EventType.VISITE]: 'Site visit',
      [EventType.FORMATION]: 'Training / workshop',
      [EventType.DISTRIBUTION]: 'Distribution',
      [EventType.COLLECTE]: 'Collection drive'
    };
    return labels[t] ?? t;
  }

  eventsListPath(): string {
    return this.isAdminRoute() ? '/admin/events' : '/events';
  }

  private toDatetimeLocal(iso: string): string {
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) {
      return '';
    }
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  private loadEvent(id: number): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.eventsService.getEventById(id).subscribe({
      next: (ev) => {
        this.eventForm.patchValue({
          title: ev.title,
          description: ev.description ?? '',
          date: this.toDatetimeLocal(ev.date),
          location: ev.location ?? '',
          type: ev.type,
          maxCapacity: ev.maxCapacity ?? 100
        });
        this.isLoading = false;
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Failed to load event';
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.eventForm.invalid) {
      this.eventForm.markAllAsTouched();
      return;
    }
    const localDateValue = this.eventForm.get('date')?.value as string;
    if (!this.isDateWithLeadTimeValid(localDateValue)) {
      this.errorMessage = `Event date must be at least ${this.minLeadHours} hours in the future.`;
      return;
    }
    const raw = this.eventForm.value;
    const payload = {
      title: raw.title,
      description: raw.description || undefined,
      date: raw.date,
      location: raw.location || undefined,
      type: raw.type as EventType,
      maxCapacity: Number(raw.maxCapacity)
    };

    this.submitLoading = true;
    this.errorMessage = '';

    if (this.isEdit && this.eventId != null) {
      this.eventsService.updateEvent(this.eventId, payload).subscribe({
        next: () => {
          this.submitLoading = false;
          this.router.navigate(this.isAdminRoute() ? ['/admin/events', this.eventId] : ['/events', this.eventId]);
        },
        error: (err) => {
          this.errorMessage = this.extractApiError(err, 'Could not update event');
          this.submitLoading = false;
        }
      });
    } else {
      this.eventsService.createEvent(payload).subscribe({
        next: (ev) => {
          this.submitLoading = false;
          this.router.navigate(this.isAdminRoute() ? ['/admin/events', ev.id] : ['/events', ev.id]);
        },
        error: (err) => {
          this.errorMessage = this.extractApiError(err, 'Could not create event');
          this.submitLoading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigateByUrl(this.eventsListPath());
  }

  private isDateWithLeadTimeValid(dateValue: string): boolean {
    if (!dateValue) {
      return false;
    }
    const selected = new Date(dateValue);
    if (Number.isNaN(selected.getTime())) {
      return false;
    }
    const minAllowed = new Date(Date.now() + this.minLeadHours * 60 * 60 * 1000);
    return selected.getTime() >= minAllowed.getTime();
  }

  private extractApiError(err: any, fallback: string): string {
    if (err?.error?.errors && typeof err.error.errors === 'object') {
      const first = Object.values(err.error.errors)[0];
      if (typeof first === 'string' && first.trim()) {
        return first;
      }
    }
    if (typeof err?.error?.message === 'string' && err.error.message.trim()) {
      return err.error.message;
    }
    return fallback;
  }
}
