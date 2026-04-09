import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  CampaignsService,
  Campaign,
  CampaignStatus,
  CampaignType
} from '@core/services/campaigns.service';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-campaigns',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './campaigns.component.html',
  styleUrls: ['./campaigns.component.scss']
})
export class CampaignsComponent implements OnInit {
  allCampaigns: Campaign[] = [];
  isLoading = false;
  errorMessage = '';
  searchQuery = '';
  statusFilter: CampaignStatus | '' = '';
  typeFilter: CampaignType | '' = '';
  hasVotedMap: Record<number, boolean> = {};
  votingId: number | null = null;

  readonly statuses: CampaignStatus[] = [
    CampaignStatus.DRAFT,
    CampaignStatus.ACTIVE,
    CampaignStatus.COMPLETED,
    CampaignStatus.CANCELLED
  ];
  readonly types = Object.values(CampaignType);

  constructor(
    private campaignsService: CampaignsService,
    private authService: AuthService,
    private router: Router
  ) {}

  canCreateCampaign(): boolean {
    return this.authService.canCreateContent();
  }

  /** Members create campaigns; admin console is manage-only (no create CTA). */
  showNewCampaignButton(): boolean {
    return this.canCreateCampaign() && !this.isAdminRoute();
  }

  isAdminRoute(): boolean {
    return this.router.url.split('?')[0].startsWith('/admin');
  }

  dashboardLink(): string {
    return this.isAdminRoute() ? '/admin/dashboard' : '/dashboard';
  }

  campaignDetailLink(id: number): (string | number)[] {
    return this.isAdminRoute() ? ['/admin/campaigns', id] : ['/campaigns', id];
  }

  isPlatformAdmin(): boolean {
    return this.authService.isAdmin();
  }

  ngOnInit(): void {
    this.loadCampaigns();
  }

  get filteredCampaigns(): Campaign[] {
    let list = [...this.allCampaigns];
    const q = this.searchQuery.trim().toLowerCase();
    if (q) {
      list = list.filter(
        (c) =>
          c.name.toLowerCase().includes(q) ||
          (c.hashtag && c.hashtag.toLowerCase().includes(q))
      );
    }
    if (this.statusFilter) {
      list = list.filter((c) => c.status === this.statusFilter);
    }
    if (this.typeFilter) {
      list = list.filter((c) => c.type === this.typeFilter);
    }
    return list;
  }

  loadCampaigns(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.campaignsService.getAllCampaigns().subscribe({
      next: (campaigns) => {
        this.allCampaigns = campaigns;
        this.isLoading = false;
        this.loadVoteFlags();
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Échec du chargement des campagnes';
        this.isLoading = false;
      }
    });
  }

  private loadVoteFlags(): void {
    if (!this.authService.isLoggedIn() || this.authService.isAdmin()) {
      return;
    }
    const ids = this.allCampaigns.map((c) => c.id);
    if (ids.length === 0) {
      return;
    }
    forkJoin(
      ids.map((id) =>
        this.campaignsService.hasVoted(id).pipe(
          map((v) => ({ id, v })),
          catchError(() => of({ id, v: false }))
        )
      )
    ).subscribe({
      next: (rows) => {
        this.hasVotedMap = {};
        for (const r of rows) {
          this.hasVotedMap[r.id] = r.v;
        }
      }
    });
  }

  getStatusPillClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-100 text-green-800';
      case 'DRAFT':
        return 'bg-gray-100 text-gray-800';
      case 'COMPLETED':
        return 'bg-slate-100 text-slate-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  getTypeHeaderClass(type: CampaignType): string {
    switch (type) {
      case CampaignType.FOOD_COLLECTION:
        return 'bg-gradient-to-r from-green-500 to-emerald-600';
      case CampaignType.FUNDRAISING:
        return 'bg-gradient-to-r from-amber-500 to-orange-600';
      case CampaignType.VOLUNTEER:
        return 'bg-gradient-to-r from-green-500 to-emerald-600';
      case CampaignType.AWARENESS:
        return 'bg-gradient-to-r from-emerald-500 to-emerald-600';
      default:
        return 'bg-gradient-to-r from-green-500 to-emerald-600';
    }
  }

  getMoneyProgressPct(c: Campaign): number {
    const need = Number(c.neededAmount ?? 0);
    const cur = Number(c.goalAmount ?? 0);
    if (!need || need <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((cur / need) * 100));
  }

  formatHashtag(h?: string): string {
    if (!h) {
      return '';
    }
    return h.startsWith('#') ? h : `#${h}`;
  }

  /** Backend: votes only on DRAFT campaigns to reach launch threshold. */
  canShowVote(c: Campaign): boolean {
    return c.status === CampaignStatus.DRAFT;
  }

  isCampaignCreator(c: Campaign): boolean {
    const uid = this.authService.getCurrentUser()?.id;
    return uid != null && c.createdById != null && c.createdById === uid;
  }

  canVoteOnCampaign(c: Campaign): boolean {
    return this.canShowVote(c) && !this.isCampaignCreator(c);
  }

  hasVoteButtonDisabled(c: Campaign): boolean {
    return c.status !== CampaignStatus.DRAFT || !!this.hasVotedMap[c.id];
  }

  vote(c: Campaign, ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    if (this.hasVoteButtonDisabled(c) || this.votingId != null) {
      return;
    }
    this.votingId = c.id;
    this.campaignsService.voteForCampaign(c.id).subscribe({
      next: () => {
        c.voteCount = (c.voteCount || 0) + 1;
        this.hasVotedMap[c.id] = true;
        this.votingId = null;
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        const msg = err.error?.message || 'Vote impossible';
        this.errorMessage = msg;
        if (err.status === 409 || err.status === 400 || /already voted|déjà voté/i.test(msg)) {
          this.hasVotedMap[c.id] = true;
        }
        this.votingId = null;
      }
    });
  }
}
