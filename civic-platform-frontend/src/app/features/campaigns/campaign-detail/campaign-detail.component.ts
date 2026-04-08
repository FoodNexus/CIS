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
      this.successMessage = 'Campagne enregistrée.';
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
        if (this.authService.isLoggedIn()) {
          this.campaignsService.hasVoted(id).subscribe({
            next: (v) => (this.hasVoted = v),
            error: () => (this.hasVoted = false)
          });
        }
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Échec du chargement de la campagne';
        this.isLoading = false;
      }
    });
  }

  canManage(): boolean {
    const u = this.authService.getCurrentUser();
    if (!u || !this.campaign) {
      return false;
    }
    if (this.authService.hasRole('ADMIN')) {
      return true;
    }
    return u.id === this.campaign.createdById;
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
    const need = Number(c.neededAmount ?? 0);
    const cur = Number(c.goalAmount ?? 0);
    if (!need || need <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((cur / need) * 100));
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
        const msg = error.error?.message || 'Vote impossible';
        this.errorMessage = msg;
        if (error.status === 409 || error.status === 400 || /already voted|déjà voté/i.test(msg)) {
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
        this.successMessage = 'Campagne lancée.';
        this.actionLoading = false;
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Échec';
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
        this.successMessage = 'Campagne clôturée.';
        this.actionLoading = false;
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Échec';
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
        this.router.navigate(['/campaigns']);
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Suppression impossible';
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
