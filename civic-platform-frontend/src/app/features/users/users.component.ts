import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AdminUserPayload, UsersService } from '@core/services/users.service';
import { User, UserType } from '@core/models/auth.models';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  isLoading = false;
  isSaving = false;
  errorMessage = '';
  formErrorMessage = '';
  userTypes = Object.values(UserType);
  badgeOptions = ['NONE', 'BRONZE', 'SILVER', 'GOLD', 'PLATINUM'];
  isFormOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingUserId: number | null = null;
  formModel: AdminUserPayload = this.emptyFormModel();

  constructor(private usersService: UsersService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading = true;
    this.usersService.getAllUsers().subscribe({
      next: (users: User[]) => {
        this.users = users;
        this.isLoading = false;
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || 'Failed to load users';
        this.isLoading = false;
      }
    });
  }

  deleteUser(id: number): void {
    if (confirm('Are you sure?')) {
      this.usersService.deleteUser(id).subscribe({
        next: () => {
          this.users = this.users.filter((u: User) => u.id !== id);
        },
        error: (err: any) => {
          this.errorMessage = err.error?.message || 'Failed to delete';
        }
      });
    }
  }

  openCreateForm(): void {
    this.formMode = 'create';
    this.editingUserId = null;
    this.formErrorMessage = '';
    this.formModel = this.emptyFormModel();
    this.isFormOpen = true;
  }

  openEditForm(user: User): void {
    this.formMode = 'edit';
    this.editingUserId = user.id;
    this.formErrorMessage = '';
    this.formModel = {
      userName: user.userName,
      email: user.email,
      userType: user.userType,
      firstName: user.firstName,
      lastName: user.lastName,
      phone: user.phone,
      address: user.address,
      companyName: user.companyName,
      associationName: user.associationName,
      contactName: user.contactName,
      contactEmail: user.contactEmail,
      birthDate: user.birthDate,
      points: user.points,
      badge: user.badge
    };
    this.isFormOpen = true;
  }

  closeForm(): void {
    if (this.isSaving) {
      return;
    }
    this.isFormOpen = false;
    this.formErrorMessage = '';
    this.editingUserId = null;
  }

  submitForm(): void {
    this.formErrorMessage = '';
    const payload = this.buildPayload();
    if (!payload.userName || !payload.email || !payload.userType) {
      this.formErrorMessage = 'Username, email, and user type are required.';
      return;
    }
    if (this.formMode === 'create' && !payload.password) {
      this.formErrorMessage = 'Password is required when creating a user.';
      return;
    }

    this.isSaving = true;
    const request$ = this.formMode === 'create'
      ? this.usersService.createUser(payload)
      : this.usersService.updateUser(this.editingUserId as number, payload);

    request$.subscribe({
      next: () => {
        this.isSaving = false;
        this.closeForm();
        this.loadUsers();
      },
      error: (err: any) => {
        this.isSaving = false;
        this.formErrorMessage = err.error?.message || `Failed to ${this.formMode} user`;
      }
    });
  }

  shouldShowCitizenFields(): boolean {
    return this.formModel.userType === UserType.CITIZEN;
  }

  shouldShowDonorFields(): boolean {
    return this.formModel.userType === UserType.DONOR;
  }

  private emptyFormModel(): AdminUserPayload {
    return {
      userName: '',
      email: '',
      password: '',
      userType: UserType.CITIZEN,
      firstName: '',
      lastName: '',
      phone: '',
      address: '',
      companyName: '',
      associationName: '',
      contactName: '',
      contactEmail: '',
      birthDate: '',
      points: undefined,
      badge: 'NONE'
    };
  }

  private buildPayload(): AdminUserPayload {
    const trimmed = (value?: string): string | undefined => {
      const v = value?.trim();
      return v ? v : undefined;
    };
    return {
      userName: trimmed(this.formModel.userName),
      email: trimmed(this.formModel.email),
      password: trimmed(this.formModel.password),
      userType: this.formModel.userType,
      firstName: trimmed(this.formModel.firstName),
      lastName: trimmed(this.formModel.lastName),
      phone: trimmed(this.formModel.phone),
      address: trimmed(this.formModel.address),
      companyName: trimmed(this.formModel.companyName),
      associationName: trimmed(this.formModel.associationName),
      contactName: trimmed(this.formModel.contactName),
      contactEmail: trimmed(this.formModel.contactEmail),
      birthDate: trimmed(this.formModel.birthDate),
      points: this.formModel.points != null ? Number(this.formModel.points) : undefined,
      badge: trimmed(this.formModel.badge)
    };
  }
}
