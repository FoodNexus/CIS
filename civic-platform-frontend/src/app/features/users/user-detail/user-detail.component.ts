import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { UsersService } from '@core/services/users.service';
import { User } from '@core/models/auth.models';
import { ZoomableImageDirective } from '@shared/directives/zoomable-image.directive';

@Component({
  selector: 'app-user-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ZoomableImageDirective],
  templateUrl: './user-detail.component.html',
  styleUrls: ['./user-detail.component.scss']
})
export class UserDetailComponent implements OnInit {
  user: User | null = null;
  isLoading = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private usersService: UsersService
  ) {}

  backLink(): string {
    return this.router.url.split('?')[0].startsWith('/admin') ? '/admin/users' : '/dashboard';
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadUser(Number(id));
    }
  }

  avatarUrl(u: User): string {
    if (!u?.hasProfilePicture || !u.id) {
      return '';
    }
    return this.usersService.profilePictureUrl(u.id, u.profilePictureRevision);
  }

  loadUser(id: number): void {
    this.isLoading = true;
    this.usersService.getUserById(id).subscribe({
      next: (user: User) => {
        this.user = user;
        this.isLoading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to load user';
        this.isLoading = false;
      }
    });
  }
}
