import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { EventsService, EventType } from '@core/services/events.service';

@Component({
  selector: 'app-event-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="max-w-2xl mx-auto p-6">
      <h2 class="text-2xl font-bold mb-6">Create New Event</h2>
      <form [formGroup]="eventForm" (ngSubmit)="onSubmit()" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700">Title *</label>
          <input type="text" formControlName="title" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm border px-3 py-2">
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Description</label>
          <textarea formControlName="description" rows="3" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm border px-3 py-2"></textarea>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700">Date *</label>
            <input type="datetime-local" formControlName="date" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm border px-3 py-2">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">Location</label>
            <input type="text" formControlName="location" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm border px-3 py-2">
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700">Type *</label>
            <select formControlName="type" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm border px-3 py-2">
              <option *ngFor="let t of eventTypes" [value]="t">{{ t }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">Max Capacity *</label>
            <input type="number" formControlName="maxCapacity" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm border px-3 py-2">
          </div>
        </div>
        <div class="flex justify-end space-x-3 pt-4">
          <button type="button" (click)="router.navigate(['/events'])" class="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50">Cancel</button>
          <button type="submit" [disabled]="eventForm.invalid" class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50">Create Event</button>
        </div>
      </form>
    </div>
  `
})
export class EventFormComponent {
  eventForm: FormGroup;
  eventTypes = Object.values(EventType);

  constructor(
    private fb: FormBuilder,
    public router: Router,
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

  onSubmit(): void {
    if (this.eventForm.valid) {
      this.eventsService.createEvent(this.eventForm.value).subscribe({
        next: () => this.router.navigate(['/events']),
        error: (err) => console.error('Failed to create event:', err)
      });
    }
  }
}
