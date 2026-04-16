import { Component, HostListener, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Subscription, interval, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NotificationsService } from '@core/services/notifications.service';
import { EventInvitationService } from '@core/services/event-invitation.service';
import { AuthService } from '@core/services/auth.service';
import { AppNotification } from '@core/models/notification.model';
import { UserType } from '@core/models/auth.models';

/** Bell + dropdown for in-app notifications; polls unread count periodically. */
@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.scss']
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  open = false;
  loading = false;
  unread = 0;
  /** Pending event invitations (INVITED) — citizen / participant / ambassador */
  invitePending = 0;
  items: AppNotification[] = [];
  private sub?: Subscription;
  private poll?: Subscription;
  private invalidateSub?: Subscription;

  /** `dark` variant for admin nav bar */
  @Input() variant: 'default' | 'dark' = 'default';

  constructor(
    private notifications: NotificationsService,
    private eventInvitationService: EventInvitationService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.refreshCount();
    this.poll = interval(45000).subscribe(() => this.refreshCount());
    this.invalidateSub = this.notifications.unreadCountInvalidated$.subscribe(() =>
      this.refreshCount()
    );
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.poll?.unsubscribe();
    this.invalidateSub?.unsubscribe();
  }

  @HostListener('document:click', ['$event'])
  onDocClick(ev: MouseEvent): void {
    const el = ev.target as HTMLElement;
    if (!el.closest?.('[data-notification-bell-root]')) {
      this.open = false;
    }
  }

  toggle(): void {
    this.open = !this.open;
    if (this.open) {
      this.loadPreview();
    }
  }

  refreshCount(): void {
    this.sub?.unsubscribe();
    const u = this.authService.getCurrentUser();
    const pollInvites =
      u &&
      !u.isAdmin &&
      (u.userType === UserType.CITIZEN ||
        u.userType === UserType.PARTICIPANT ||
        u.userType === UserType.AMBASSADOR);

    this.sub = forkJoin({
      unread: this.notifications.unreadCount().pipe(catchError(() => of({ count: 0 }))),
      invites: pollInvites
        ? this.eventInvitationService.getMyInvitations().pipe(catchError(() => of([])))
        : of([])
    }).subscribe({
      next: ({ unread, invites }) => {
        this.unread = unread.count ?? 0;
        this.invitePending = Array.isArray(invites)
          ? invites.filter((i) => i.status === 'INVITED').length
          : 0;
      },
      error: () => {
        this.unread = 0;
        this.invitePending = 0;
      }
    });
  }

  /** Combined badge for nav (unread + pending event invites). */
  totalBadge(): number {
    return this.unread + this.invitePending;
  }

  loadPreview(): void {
    this.loading = true;
    this.notifications.list(0, 8).subscribe({
      next: (page) => {
        this.items = page.content ?? [];
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  onItemClick(n: AppNotification): void {
    if (!n.readAt) {
      this.notifications.markRead(n.id).subscribe({
        next: () => {
          n.readAt = new Date().toISOString();
        }
      });
    }
    this.open = false;
    if (n.linkUrl) {
      this.router.navigateByUrl(n.linkUrl);
    }
  }

  markAllRead(): void {
    this.notifications.markAllRead().subscribe({
      next: () => {
        this.items.forEach((i) => (i.readAt = i.readAt || new Date().toISOString()));
      }
    });
  }

  goToFullPage(): void {
    this.open = false;
    this.router.navigate(['/notifications']);
  }

  goToInvitations(): void {
    this.open = false;
    this.router.navigate(['/dashboard'], { queryParams: { tab: 'invitations' } });
  }
}
