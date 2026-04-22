import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  CampaignsService,
  Campaign,
  CampaignStatus,
  CampaignType
} from '@core/services/campaigns.service';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-campaign-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './campaign-detail.component.html',
  styleUrls: ['./campaign-detail.component.scss']
})
export class CampaignDetailComponent implements OnInit {
  campaign: Campaign | null = null;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  hasVoted = false;
  voting = false;
  actionLoading = false;
  showDeleteModal = false;
  deleteLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private campaignsService: CampaignsService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('success') === '1') {
      this.successMessage = 'Campaign saved.';
      this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
      setTimeout(() => (this.successMessage = ''), 4000);
    }
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadCampaign(Number(id));
    }
  }

  loadCampaign(id: number): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.campaignsService.getCampaignById(id).subscribe({
      next: (campaign) => {
        this.campaign = campaign;
        this.isLoading = false;
        if (this.authService.isLoggedIn() && !this.authService.isAdmin()) {
          this.campaignsService.hasVoted(id).subscribe({
            next: (v) => (this.hasVoted = v),
            error: () => (this.hasVoted = false)
          });
        }
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to load campaign';
        this.isLoading = false;
      }
    });
  }

  /** Creator or platform admin may edit, launch, close, or delete. */
  canManageCampaign(): boolean {
    if (!this.campaign) {
      return false;
    }
    if (this.authService.isAdmin()) {
      return true;
    }
    const u = this.authService.getCurrentUser();
    return !!u && u.id === this.campaign.createdById;
  }

  /** Launch votes are for members; admins manage without voting here. */
  showVoteSection(): boolean {
    return !this.authService.isAdmin();
  }

  campaignEditLink(): (string | number)[] {
    if (!this.campaign) {
      return [this.campaignsListPath()];
    }
    return this.isAdminRoute()
      ? ['/admin/campaigns', this.campaign.id, 'edit']
      : ['/campaigns', this.campaign.id, 'edit'];
  }

  campaignsListPath(): string {
    return this.isAdminRoute() ? '/admin/campaigns' : '/campaigns';
  }

  isAdminRoute(): boolean {
    return this.router.url.split('?')[0].startsWith('/admin');
  }

  getStatusPillClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-100 text-green-800';
      case 'DRAFT':
        return 'bg-gray-100 text-gray-800';
      case 'COMPLETED':
        return 'bg-teal-100 text-teal-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  getTypePillClass(type: CampaignType): string {
    return 'bg-emerald-50 text-emerald-700';
  }

  getMoneyProgressPct(c: Campaign): number {
    const goal = Number(c.goalAmount ?? 0);
    if (!goal || goal <= 0) {
      return 0;
    }
    const raised = this.getCurrentRaisedAmount(c);
    return Math.min(100, Math.max(0, Math.round((raised / goal) * 100)));
  }

  getCurrentRaisedAmount(c: Campaign): number {
    const goal = Number(c.goalAmount ?? 0);
    const needed = Number(c.neededAmount ?? 0);
    if (!goal || goal <= 0) {
      return 0;
    }
    if (needed <= 0) {
      return goal;
    }
    return Math.min(goal, Math.max(0, goal - needed));
  }

  voteProgressPct(c: Campaign): number {
    const v = c.voteCount ?? 0;
    return Math.min(100, Math.round((v / 100) * 100));
  }

  formatHashtag(h?: string): string {
    if (!h) {
      return '';
    }
    return h.startsWith('#') ? h : `#${h}`;
  }

  canVote(): boolean {
    return (
      this.campaign?.status === CampaignStatus.DRAFT &&
      !this.hasVoted &&
      this.authService.isLoggedIn()
    );
  }

  vote(): void {
    if (!this.campaign || this.voting) {
      return;
    }
    this.voting = true;
    this.campaignsService.voteForCampaign(this.campaign.id).subscribe({
      next: () => {
        this.hasVoted = true;
        if (this.campaign) {
          this.campaign.voteCount = (this.campaign.voteCount || 0) + 1;
        }
        this.loadCampaign(this.campaign!.id);
        this.voting = false;
      },
      error: (error: { status?: number; error?: { message?: string } }) => {
        const msg = error.error?.message || 'Vote failed';
        this.errorMessage = msg;
        if (error.status === 409 || error.status === 400 || /already voted/i.test(msg)) {
          this.hasVoted = true;
        }
        this.voting = false;
      }
    });
  }

  launch(): void {
    if (!this.campaign) {
      return;
    }
    this.actionLoading = true;
    this.campaignsService.launchCampaign(this.campaign.id).subscribe({
      next: (c) => {
        this.campaign = c;
        this.successMessage = 'Campaign launched.';
        this.actionLoading = false;
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Failed';
        this.actionLoading = false;
      }
    });
  }

  closeCampaign(): void {
    if (!this.campaign) {
      return;
    }
    this.actionLoading = true;
    this.campaignsService.closeCampaign(this.campaign.id).subscribe({
      next: (c) => {
        this.campaign = c;
        this.successMessage = 'Campaign closed.';
        this.actionLoading = false;
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Failed';
        this.actionLoading = false;
      }
    });
  }

  openDeleteModal(): void {
    this.showDeleteModal = true;
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
  }

  confirmDelete(): void {
    if (!this.campaign) {
      return;
    }
    this.deleteLoading = true;
    this.campaignsService.deleteCampaign(this.campaign.id).subscribe({
      next: () => {
        this.router.navigateByUrl(this.campaignsListPath());
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Delete failed';
        this.deleteLoading = false;
        this.showDeleteModal = false;
      }
    });
  }

  creatorInitials(): string {
    const n = this.campaign?.createdByName;
    if (!n) {
      return '?';
    }
    const parts = n.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return n.slice(0, 2).toUpperCase();
  }
}
