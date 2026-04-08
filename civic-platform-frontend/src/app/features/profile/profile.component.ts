import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '@core/services/auth.service';
import { User } from '@core/models/auth.models';
import { UsersService } from '@core/services/users.service';

import { BadgeComponent } from '@shared/components/badge/badge.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, BadgeComponent],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-emerald-50/90 via-white to-teal-50/80 py-8">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8" *ngIf="currentUser as user">
        <div class="bg-white rounded-2xl shadow-xl overflow-hidden border border-emerald-100">
          <div class="bg-gradient-to-r from-green-500 to-emerald-600 p-6 text-white">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-4">
                <div class="h-14 w-14 rounded-full bg-white/20 flex items-center justify-center text-2xl font-bold">
                  {{ user.userName.charAt(0).toUpperCase() }}
                </div>
                <div>
                  <p class="text-xs font-semibold uppercase tracking-wide text-emerald-100">Profile</p>
                  <h1 class="text-2xl font-bold">{{ user.firstName || '' }} {{ user.lastName || '' }}</h1>
                  <p class="text-emerald-100">{{ '@' + user.userName }}</p>
                </div>
              </div>
              <app-badge [badge]="user.badge" [awardedDate]="user.awardedDate"></app-badge>
            </div>
          </div>

          <div class="p-6 border-b border-emerald-100">
            <div class="rounded-2xl overflow-hidden shadow-lg border border-emerald-100 bg-white">
              <div class="bg-gradient-to-r from-emerald-500 to-teal-600 px-5 py-3 flex items-center justify-between">
                <span class="text-white text-xs font-bold uppercase tracking-wider">My QR code</span>
              </div>
              <div class="p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <p class="text-sm text-gray-600">PNG for check-in — encodes your public identity JSON (same spirit as My donations cards).</p>
                <button type="button" (click)="downloadMyQr()" [disabled]="qrDownloading"
                        class="inline-flex items-center justify-center px-5 py-2.5 rounded-xl font-bold text-white bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 shadow-md disabled:opacity-50 text-sm whitespace-nowrap">
                  {{ qrDownloading ? 'Preparing…' : 'Download QR (PNG)' }}
                </button>
              </div>
            </div>
          </div>

          <form [formGroup]="profileForm" (ngSubmit)="save()" class="p-6 space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="bg-blue-50 rounded-xl p-4">
                <label class="text-xs text-blue-700 font-semibold">User Type (read-only)</label>
                <p class="text-lg font-semibold text-blue-900">{{ user.userType }}</p>
              </div>
              <div class="bg-emerald-50 rounded-xl p-4">
                <label class="text-xs text-emerald-700 font-semibold">Role (read-only)</label>
                <p class="text-lg font-semibold text-emerald-900">{{ user.role }}</p>
              </div>
            </div>

            <div class="bg-emerald-50 rounded-xl p-4">
              <h2 class="text-lg font-semibold text-emerald-900 mb-3">Personal Info</h2>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <input class="input" placeholder="First Name" formControlName="firstName" />
                <input class="input" placeholder="Last Name" formControlName="lastName" />
                <input class="input" placeholder="Username" formControlName="userName" />
                <input class="input" placeholder="Birth Date" type="date" formControlName="birthDate" />
              </div>
            </div>

            <div class="bg-green-50 rounded-xl p-4">
              <h2 class="text-lg font-semibold text-green-900 mb-3">Contact Info</h2>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <input class="input" placeholder="Email" type="email" formControlName="email" />
                <input class="input" placeholder="Phone" formControlName="phone" />
                <input class="input md:col-span-2" placeholder="Address" formControlName="address" />
              </div>
            </div>

            <div class="bg-emerald-50 rounded-xl p-4" *ngIf="user.userType === 'DONOR' || user.associationName || user.companyName">
              <h2 class="text-lg font-semibold text-emerald-900 mb-3">Association Info</h2>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <input class="input" placeholder="Association Name" formControlName="associationName" />
                <input class="input" placeholder="Company Name" formControlName="companyName" />
                <input class="input" placeholder="Contact Name" formControlName="contactName" />
                <input class="input" placeholder="Contact Email" type="email" formControlName="contactEmail" />
              </div>
            </div>

            <div class="flex items-center justify-between pt-2">
              <a routerLink="/dashboard" class="text-emerald-700 hover:text-emerald-900 font-medium">← Back to Dashboard</a>
              <button type="submit" [disabled]="isSaving || profileForm.invalid"
                      class="bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white rounded-xl px-5 py-2.5 font-semibold disabled:opacity-50 shadow-md">
                {{ isSaving ? 'Saving...' : 'Save Profile' }}
              </button>
            </div>

            <p *ngIf="message" class="text-sm font-medium" [class.text-red-600]="messageType==='error'" [class.text-green-700]="messageType==='success'">
              {{ message }}
            </p>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .input {
      width: 100%;
      border: 1px solid #d1d5db;
      border-radius: 0.5rem;
      padding: 0.55rem 0.75rem;
      background: #fff;
      outline: none;
    }
    .input:focus {
      border-color: #10b981;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
    }
  `]
})
export class ProfileComponent {
  currentUser: User | null = null;
  profileForm: FormGroup;
  isSaving = false;
  qrDownloading = false;
  message = '';
  messageType: 'success' | 'error' = 'success';

  constructor(
    private authService: AuthService,
    private usersService: UsersService,
    private fb: FormBuilder
  ) {
    this.profileForm = this.fb.group({
      userName: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      firstName: [''],
      lastName: [''],
      phone: [''],
      address: [''],
      birthDate: [''],
      associationName: [''],
      companyName: [''],
      contactName: [''],
      contactEmail: ['', Validators.email]
    });

    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      if (user) {
        this.profileForm.patchValue({
          userName: user.userName,
          email: user.email,
          firstName: user.firstName,
          lastName: user.lastName,
          phone: user.phone,
          address: user.address,
          birthDate: this.formatDateForInput(user.birthDate),
          associationName: user.associationName,
          companyName: user.companyName,
          contactName: user.contactName,
          contactEmail: user.contactEmail
        });
      }
    });
  }

  // Helper to convert date from backend (object or string) to YYYY-MM-DD format for HTML date input
  private formatDateForInput(dateValue: any): string {
    if (!dateValue) return '';
    
    // If it's already a string in YYYY-MM-DD format, return it
    if (typeof dateValue === 'string') {
      // Check if it's ISO format (YYYY-MM-DDTHH:mm:ss...)
      if (dateValue.includes('T')) {
        return dateValue.split('T')[0];
      }
      return dateValue;
    }
    
    // If it's an object with year, month, day (LocalDate serialization)
    if (typeof dateValue === 'object' && dateValue.year !== undefined) {
      const year = dateValue.year;
      const month = String(dateValue.monthValue || dateValue.month).padStart(2, '0');
      const day = String(dateValue.dayOfMonth || dateValue.day).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
    
    return '';
  }

  downloadMyQr(): void {
    if (!this.currentUser?.id) return;
    this.qrDownloading = true;
    this.usersService.downloadQrCodePng(this.currentUser.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `qrcode-user-${this.currentUser!.id}.png`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.qrDownloading = false;
      },
      error: () => {
        this.qrDownloading = false;
      }
    });
  }

  save(): void {
    if (!this.currentUser || this.profileForm.invalid) return;
    this.isSaving = true;
    this.message = '';

    // Only send profile fields - never send userName, email, role, userType, badge, points
    const profileData = {
      firstName: this.profileForm.value.firstName,
      lastName: this.profileForm.value.lastName,
      phone: this.profileForm.value.phone,
      address: this.profileForm.value.address,
      birthDate: this.profileForm.value.birthDate,
      companyName: this.profileForm.value.companyName,
      associationName: this.profileForm.value.associationName,
      contactName: this.profileForm.value.contactName,
      contactEmail: this.profileForm.value.contactEmail
    };

    this.usersService.updateProfile(this.currentUser.id, profileData).subscribe({
      next: (updated) => {
        // Merge updated fields with current user, preserving identity fields
        const refreshed: User = {
          ...this.currentUser!,
          ...updated,
          // Ensure identity fields are never changed
          userType: this.currentUser!.userType,
          role: this.currentUser!.role,
          badge: this.currentUser!.badge,
          points: this.currentUser!.points,
          userName: this.currentUser!.userName,
          email: this.currentUser!.email
        };
        
        // Update AuthService so all components get fresh data
        this.authService.updateCurrentUser(refreshed);
        this.currentUser = refreshed;
        
        this.messageType = 'success';
        this.message = 'Profile updated successfully.';
        this.isSaving = false;
      },
      error: (err) => {
        this.messageType = 'error';
        // Show specific backend error message if available
        this.message = err?.error?.message || err?.message || 'Failed to update profile. Please try again.';
        console.error('Profile update error:', err);
        this.isSaving = false;
      }
    });
  }
}
