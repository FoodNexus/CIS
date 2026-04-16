import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { finalize } from 'rxjs';
import { AuthService } from '@core/services/auth.service';
import { BadgeComponent } from '@shared/components/badge/badge.component';
import { CampaignsService, Campaign } from '@core/services/campaigns.service';
import { EventsService, Event, EventParticipation } from '@core/services/events.service';
import { ProjectsService, Project, ProjectFundingHistory } from '@core/services/projects.service';
import { PostsService, Post } from '@core/services/posts.service';
import { User, UserType } from '@core/models/auth.models';
import { FeedResponse } from '@core/models/feed.model';
import { RecommendationsService } from '@core/services/recommendations.service';
import { EventInvitationService } from '@core/services/event-invitation.service';
import { EventInvitation } from '@core/models/event-invitation.model';
import { isMeaningfulModelVersion } from '@core/utils/ml-display';

interface DashboardTab {
  id: string;
  label: string;
  icon: string;
}


interface FundingHistory {
  projectId: number;
  projectTitle: string;
  amount: number;
  fundDate: string;
  paymentMethod?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, BadgeComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  // User data
  currentUser: User | null = null;

  private readonly tabIcons = {
    campaigns: 'M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z',
    events: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
    projects: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
    volunteering: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
    profile: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
    settings: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065zM15 12a3 3 0 11-6 0 3 3 0 016 0z',
    posts: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z',
    invitations: 'M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z'
  };

  // Active tab — events-first (invitations / volunteering use other tab ids)
  activeTab = 'events';

  // Loading states
  isLoading = true;
  error: string | null = null;

  // Data for tabs
  myCampaigns: Campaign[] = [];
  myEvents: Event[] = [];
  myProjects: Project[] = [];
  myPosts: Post[] = [];

  // Impact stats for donors
  totalAttendees = 0;
  totalFundsRaised = 0;

  // Feed data
  feedCampaigns: Campaign[] = [];
  feedEvents: Event[] = [];
  feedProjects: Project[] = [];
  /** Recommended posts (ML); citizen / participant / ambassador when ML feed is used. */
  feedPosts: Post[] = [];
  feedModelVersion?: string;
  feedColdStart?: boolean;

  /** Expose for template — hide API placeholders like "none". */
  readonly isMeaningfulModelVersion = isMeaningfulModelVersion;

  // Participation data
  myParticipations: EventParticipation[] = [];

  // Funding history
  myFundingHistory: FundingHistory[] = [];

  /** Event invitations for citizens (citizen / participant / ambassador) */
  myInvitations: EventInvitation[] = [];
  inviteToast: string | null = null;

  // Badge progress
  eventsAttended = 0;

  // Like tracking for posts
  likedPosts = new Set<number>();

  certificateLoadingKey: string | null = null;

  constructor(
    private authService: AuthService,
    private campaignsService: CampaignsService,
    private eventsService: EventsService,
    private projectsService: ProjectsService,
    private postsService: PostsService,
    private recommendationsService: RecommendationsService,
    private eventInvitationService: EventInvitationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    if (this.route.snapshot.queryParamMap.get('tab') === 'invitations') {
      this.activeTab = 'invitations';
    }
    this.route.queryParamMap.subscribe((params) => {
      if (params.get('tab') === 'invitations') {
        this.activeTab = 'invitations';
      }
    });
    this.setDefaultTab();
    this.loadAllData();
  }

  setDefaultTab(): void {
    if (this.activeTab === 'invitations') {
      return;
    }
    this.activeTab = 'events';
  }

  getWelcomeSubtitle(): string {
    switch (this.currentUser?.userType) {
      case UserType.DONOR:
        return 'Create events and reach local citizens';
      case UserType.AMBASSADOR:
        return 'Mobilize and engage your community';
      case UserType.CITIZEN:
      case UserType.PARTICIPANT:
        return 'Participate in your community\'s future';
      default:
        return 'Welcome to Civic Platform';
    }
  }

  /** ML feed applies to authenticated non-admin users who are not donors. */
  shouldUseMlFeed(): boolean {
    const u = this.currentUser;
    if (!u || u.isAdmin) {
      return false;
    }
    if (u.userType === UserType.DONOR) {
      return false;
    }
    return true;
  }

  /** Event invitations: non-donor regular users (citizen, participant, ambassador). */
  shouldLoadEventInvitations(): boolean {
    const u = this.currentUser;
    if (!u || u.isAdmin) {
      return false;
    }
    return (
      u.userType === UserType.CITIZEN ||
      u.userType === UserType.PARTICIPANT ||
      u.userType === UserType.AMBASSADOR
    );
  }

  loadAllData(): void {
    this.isLoading = true;
    this.error = null;

    const requests: Observable<any>[] = [
      this.campaignsService.getAllCampaigns().pipe(catchError(() => of([]))),
      this.eventsService.getAllEvents().pipe(catchError(() => of([]))),
      this.projectsService.getAllProjects().pipe(catchError(() => of([]))),
      this.postsService.getAllPosts().pipe(catchError(() => of([]))),
      this.eventsService.getMyParticipations().pipe(catchError(() => of([]))),
      this.projectsService.getMyFundings().pipe(catchError(() => of([])))
    ];
    if (this.shouldUseMlFeed()) {
      requests.push(this.recommendationsService.getFeed().pipe(catchError(() => of(null))));
    }
    if (this.shouldLoadEventInvitations()) {
      requests.push(this.eventInvitationService.getMyInvitations().pipe(catchError(() => of([]))));
    }

    forkJoin(requests).subscribe({
      next: (results: any[]) => {
        const allCampaigns: Campaign[] = results[0] || [];
        const allEvents: Event[] = results[1] || [];
        const allProjects: Project[] = results[2] || [];
        const allPosts: Post[] = results[3] || [];
        let idx = 6;
        const feed: FeedResponse | null = this.shouldUseMlFeed()
          ? (results[idx++] as FeedResponse | null)
          : null;
        this.myInvitations = this.shouldLoadEventInvitations()
          ? (results[idx] as EventInvitation[]) || []
          : [];
        const uid = this.currentUser?.id;

        this.feedEvents = allEvents;

        if (feed) {
          this.feedCampaigns = feed.campaigns ?? [];
          this.feedProjects = feed.projects ?? [];
          this.feedPosts = feed.posts ?? [];
          this.feedModelVersion = feed.modelVersion;
          this.feedColdStart = feed.coldStart;
          if (feed.events?.length) {
            this.feedEvents = feed.events;
          }
        } else {
          this.feedCampaigns = allCampaigns;
          this.feedProjects = allProjects;
          this.feedPosts = [];
          this.feedModelVersion = undefined;
          this.feedColdStart = undefined;
        }

        if (uid) {
          this.myCampaigns = allCampaigns.filter(c => c.createdById === uid);
          this.myEvents = allEvents.filter(e => e.organizerId === uid);
          this.myProjects = allProjects.filter(p => p.createdById === uid);
        } else {
          this.myCampaigns = [];
          this.myEvents = [];
          this.myProjects = [];
        }

        const currentUserName = this.currentUser?.userName;
        this.myPosts = currentUserName
          ? allPosts.filter(p => p.creator === currentUserName)
          : [];

        this.myPosts.forEach(post => {
          this.postsService.checkLike(post.id).subscribe({
            next: (liked) => { if (liked) this.likedPosts.add(post.id); },
            error: () => {}
          });
        });

        this.myParticipations = results[4] || [];
        const fundings: ProjectFundingHistory[] = results[5] || [];
        this.myFundingHistory = fundings.map(f => ({
          projectId: f.projectId,
          projectTitle: f.projectTitle,
          amount: Number(f.amount),
          fundDate: f.fundDate,
          paymentMethod: f.paymentMethod
        }));
        const completed = this.myParticipations.filter(p => p.status === 'COMPLETED').length;
        this.eventsAttended = completed;

        this.calculateImpactStats();

        this.authService.refreshProfile().pipe(
          finalize(() => this.isLoading = false)
        ).subscribe({
          next: () => {
            this.currentUser = this.authService.getCurrentUser();
            const pts = this.currentUser?.points ?? 0;
            this.eventsAttended = Math.max(this.eventsAttended, pts);
          },
          error: () => {}
        });
      },
      error: (err) => {
        this.error = 'Failed to load dashboard data';
        this.isLoading = false;
        console.error('Dashboard error:', err);
      }
    });
  }

  calculateImpactStats(): void {
    this.totalAttendees = this.myEvents.reduce((sum, event) => sum + (event.currentParticipants || 0), 0);
    this.totalFundsRaised = this.myProjects.reduce((sum, project) => sum + (project.currentAmount || 0), 0);
  }

  getUserName(): string {
    const user = this.authService.getCurrentUser();
    return user?.userName || 'User';
  }

  getUserType(): string {
    const user = this.authService.getCurrentUser();
    return user?.userType ?? '';
  }

  /** DONOR or AMBASSADOR — creator dashboard and create actions. */
  canCreateContent(): boolean {
    return this.authService.canCreateContent();
  }

  // User type helpers
  isDonor(): boolean {
    return this.authService.isDonor();
  }

  isCitizen(): boolean {
    return this.authService.isCitizen();
  }

  isParticipant(): boolean {
    return this.authService.isParticipant();
  }

  isAmbassador(): boolean {
    return this.authService.isAmbassador();
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  // Tab helpers — role-specific (platform admins use /admin, not this dashboard)
  getTabs(): DashboardTab[] {
    const t = this.currentUser?.userType;
    const profile: DashboardTab = { id: 'profile', label: 'Profile', icon: this.tabIcons.profile };
    const settings: DashboardTab = { id: 'settings', label: 'Settings', icon: this.tabIcons.settings };
    const invitations: DashboardTab = {
      id: 'invitations',
      label: 'My Invitations 📬',
      icon: this.tabIcons.invitations
    };

    if (t === UserType.DONOR) {
      return [
        { id: 'events', label: 'Events', icon: this.tabIcons.events },
        profile,
        settings
      ];
    }
    if (t === UserType.AMBASSADOR) {
      return [
        { id: 'events', label: 'Events', icon: this.tabIcons.events },
        { id: 'volunteering', label: 'Volunteering', icon: this.tabIcons.volunteering },
        invitations,
        profile,
        settings
      ];
    }
    return [
      { id: 'events', label: 'Events', icon: this.tabIcons.events },
      invitations,
      profile,
      settings
    ];
  }

  setActiveTab(tabId: string): void {
    this.activeTab = tabId;
    if (tabId === 'invitations') {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { tab: 'invitations' },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    } else {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { tab: null },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    }
  }

  isActiveTab(tabId: string): boolean {
    return this.activeTab === tabId;
  }

  // User info helpers
  getUserBadge(): string {
    return this.currentUser?.badge || 'NONE';
  }

  getUserPoints(): number {
    return this.currentUser?.points || 0;
  }

  getAwardedDate(): string | undefined {
    return this.currentUser?.awardedDate;
  }

  // Badge progress — completed events attended (synced with server points after refresh)
  /** Server-authoritative attended count (completed events only). */
  getEventsAttendedDisplay(): number {
    const bp = this.currentUser?.badgeProgress;
    if (bp != null && typeof bp.events_attended === 'number') {
      return bp.events_attended;
    }
    return this.currentUser?.points ?? this.eventsAttended;
  }

  getNextBadgeThreshold(): number {
    const bp = this.currentUser?.badgeProgress;
    if (bp?.events_for_next != null) {
      return bp.events_for_next;
    }
    const n = this.getEventsAttendedDisplay();
    if (n >= 8) return 8;
    if (n >= 5) return 8;
    if (n >= 3) return 5;
    if (n >= 1) return 3;
    return 1;
  }

  getBadgeProgressPercent(): number {
    const n = this.getEventsAttendedDisplay();
    if (n >= 8) return 100;
    const next = this.getNextBadgeThreshold();
    if (next <= 0) return 0;
    return Math.min(100, (n / next) * 100);
  }

  getBadgeLabel(badge: string): string {
    switch (badge) {
      case 'NONE': return 'No Badge Yet';
      case 'BRONZE': return 'Bronze';
      case 'SILVER': return 'Silver';
      case 'GOLD': return 'Gold';
      case 'PLATINUM': return 'Platinum';
      default: return 'Unknown';
    }
  }

  getNextBadge(): string {
    const nb = this.currentUser?.badgeProgress?.next_badge;
    if (nb) return nb;
    const current = this.getUserBadge();
    const badges = ['NONE', 'BRONZE', 'SILVER', 'GOLD', 'PLATINUM'];
    const currentIndex = badges.indexOf(current);
    if (currentIndex < badges.length - 1) {
      return badges[currentIndex + 1];
    }
    return 'PLATINUM';
  }

  getEventsRemainingForNextBadge(): number {
    const r = this.currentUser?.badgeProgress?.events_remaining;
    if (r != null) return r;
    const next = this.getNextBadgeThreshold();
    const n = this.getEventsAttendedDisplay();
    return Math.max(0, next - n);
  }

  // Project funding progress
  getProjectProgress(project: Project): number {
    if (!project.goalAmount || project.goalAmount === 0) return 0;
    return Math.round((project.currentAmount / project.goalAmount) * 100);
  }

  // Campaign status color
  getCampaignStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'COMPLETED': return 'bg-slate-100 text-slate-800';
      case 'DRAFT': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  // Event status color
  getEventStatusColor(status: string): string {
    switch (status) {
      case 'UPCOMING': return 'bg-yellow-100 text-yellow-800';
      case 'ONGOING': return 'bg-green-100 text-green-800';
      case 'COMPLETED': return 'bg-slate-100 text-slate-800';
      case 'CANCELLED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  // Post helpers
  toggleLike(post: Post): void {
    if (this.likedPosts.has(post.id)) {
      this.postsService.unlikePost(post.id).subscribe({
        next: () => {
          post.likesCount = Math.max(0, post.likesCount - 1);
          this.likedPosts.delete(post.id);
        }
      });
    } else {
      this.postsService.likePost(post.id).subscribe({
        next: () => {
          post.likesCount++;
          this.likedPosts.add(post.id);
        }
      });
    }
  }

  isLiked(postId: number): boolean {
    return this.likedPosts.has(postId);
  }

  formatPostType(type: string): string {
    return type?.replace(/_/g, ' ') || '';
  }

  getPostTypeColor(type: string): string {
    switch (type) {
      case 'EVENT_ANNOUNCEMENT':    return 'bg-emerald-100 text-emerald-800';
      case 'CAMPAIGN_ANNOUNCEMENT': return 'bg-green-100 text-green-800';
      case 'TESTIMONIAL':           return 'bg-emerald-100 text-emerald-800';
      default:                      return 'bg-gray-100 text-gray-700';
    }
  }

  // Refresh data
  refreshData(): void {
    this.loadAllData();
  }

  certificateKey(p: EventParticipation): string {
    return `${p.eventId}-${p.userId}`;
  }

  respondInvitation(inv: EventInvitation, response: 'ACCEPTED' | 'DECLINED'): void {
    const token = inv.invitationToken;
    if (!token) {
      return;
    }
    this.eventInvitationService.respond(token, response).subscribe({
      next: (r) => {
        this.inviteToast = r.message;
        inv.status = response;
        inv.respondedAt = new Date().toISOString();
      },
      error: () => {
        this.inviteToast = 'Could not save your response.';
      }
    });
  }

  invitationScoreBarClass(percent: number): string {
    if (percent <= 40) {
      return 'bg-red-500';
    }
    if (percent <= 70) {
      return 'bg-amber-500';
    }
    return 'bg-emerald-500';
  }

  invitationTierLabel(tier: string): string {
    switch (tier) {
      case 'PRIORITY_IMMEDIATE':
        return 'Priority invite';
      case 'STANDARD_INVITE':
        return 'Standard invite';
      default:
        return tier;
    }
  }

  downloadParticipationCertificate(p: EventParticipation): void {
    const key = this.certificateKey(p);
    this.certificateLoadingKey = key;
    this.eventsService.downloadParticipationCertificate(p.eventId, p.userId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `certificate-event-${p.eventId}-user-${p.userId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.certificateLoadingKey = null;
      },
      error: () => {
        this.certificateLoadingKey = null;
      }
    });
  }
}
