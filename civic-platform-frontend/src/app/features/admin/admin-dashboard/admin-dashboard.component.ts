import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MetricsService, DashboardStats, ImpactMetrics } from '@core/services/metrics.service';
import { PostsService, Post, PostStatus } from '@core/services/posts.service';
import { UsersService } from '@core/services/users.service';
import { User } from '@core/models/auth.models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  metrics: ImpactMetrics | null = null;
  recentUsers: User[] = [];
  pendingPosts: Post[] = [];
  isLoading = true;
  errorMessage = '';
  /** Inline moderation errors (approve/reject from dashboard). */
  activityError = '';
  actionPostId: number | null = null;

  mlRetrainPending = false;
  mlRetrainMessage = '';

  constructor(
    private metricsService: MetricsService,
    private postsService: PostsService,
    private usersService: UsersService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.activityError = '';
    const today = new Date().toISOString().split('T')[0];

    forkJoin({
      stats: this.metricsService.getDashboardStats().pipe(catchError(() => of(null))),
      metrics: this.metricsService.getDailyMetrics(today).pipe(catchError(() => of(null))),
      users: this.usersService.getAllUsers().pipe(catchError(() => of([] as User[]))),
      pending: this.postsService.getPostsByStatus(PostStatus.PENDING).pipe(catchError(() => of([] as Post[])))
    }).subscribe({
      next: ({ stats, metrics, users, pending }) => {
        this.stats = stats;
        this.metrics = metrics;
        this.recentUsers = [...users]
          .filter((u) => !u.isAdmin)
          .sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
          .slice(0, 8);
        this.pendingPosts = pending.slice(0, 8);
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to load dashboard';
        this.isLoading = false;
      }
    });
  }

  totalUsers(): number {
    const m = this.stats?.totalUsersByType;
    if (!m) {
      return 0;
    }
    return Object.values(m).reduce((a, b) => a + (Number(b) || 0), 0);
  }

  countByType(t: string): number {
    return Number(this.stats?.totalUsersByType?.[t] ?? 0);
  }

  activeCampaigns(): number {
    return Number(this.stats?.totalCampaignsByStatus?.['ACTIVE'] ?? 0);
  }

  /** Shown in the page header (local date). */
  get todayLabel(): string {
    return new Date().toLocaleDateString(undefined, {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  /** Short date for the daily snapshot card. */
  get todayShort(): string {
    return new Date().toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }

  userTypePillClass(t?: string): string {
    switch (t) {
      case 'DONOR':
        return 'bg-amber-100 text-amber-900 ring-1 ring-amber-200/80';
      case 'AMBASSADOR':
        return 'bg-violet-100 text-violet-900 ring-1 ring-violet-200/80';
      case 'CITIZEN':
        return 'bg-sky-100 text-sky-900 ring-1 ring-sky-200/80';
      case 'PARTICIPANT':
        return 'bg-emerald-100 text-emerald-900 ring-1 ring-emerald-200/80';
      default:
        return 'bg-slate-100 text-slate-700 ring-1 ring-slate-200/80';
    }
  }

  approvePost(id: number): void {
    this.activityError = '';
    this.actionPostId = id;
    this.postsService.approvePost(id).subscribe({
      next: () => {
        this.pendingPosts = this.pendingPosts.filter((p) => p.id !== id);
        this.actionPostId = null;
      },
      error: (e) => {
        this.activityError = e.error?.message || 'Approve failed';
        this.actionPostId = null;
      }
    });
  }

  retrainMlModel(): void {
    this.mlRetrainPending = true;
    this.mlRetrainMessage = '';
    this.metricsService.triggerMlRetrain().subscribe({
      next: () => {
        this.mlRetrainPending = false;
        this.mlRetrainMessage = 'Réentraînement accepté.';
      },
      error: () => {
        this.mlRetrainPending = false;
        this.mlRetrainMessage = 'Échec du lancement.';
      }
    });
  }

  rejectPost(id: number): void {
    this.activityError = '';
    this.actionPostId = id;
    this.postsService.rejectPost(id).subscribe({
      next: () => {
        this.pendingPosts = this.pendingPosts.filter((p) => p.id !== id);
        this.actionPostId = null;
      },
      error: (e) => {
        this.activityError = e.error?.message || 'Reject failed';
        this.actionPostId = null;
      }
    });
  }
}
