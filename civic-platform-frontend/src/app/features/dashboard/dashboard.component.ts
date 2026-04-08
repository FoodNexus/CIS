import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '@core/services/auth.service';
import { BadgeComponent } from '@shared/components/badge/badge.component';
import { CampaignsService, Campaign } from '@core/services/campaigns.service';
import { EventsService, Event } from '@core/services/events.service';
import { ProjectsService, Project } from '@core/services/projects.service';
import { PostsService, Post } from '@core/services/posts.service';
import { User, Badge } from '@core/models/auth.models';

interface DashboardTab {
  id: string;
  label: string;
  icon: string;
}

interface EventParticipation {
  event: Event;
  attendedAt: string;
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

  // Tabs
  donorTabs: DashboardTab[] = [
    { id: 'campaigns', label: 'My Campaigns', icon: 'M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z' },
    { id: 'events', label: 'My Events', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
    { id: 'projects', label: 'My Projects', icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
    { id: 'posts', label: 'My Posts', icon: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z' },
    { id: 'impact', label: 'Impact Stats', icon: 'M13 7h8m0 0v8m0-8l-8 8-4-4-6 6' }
  ];

  citizenTabs: DashboardTab[] = [
    { id: 'feed', label: 'Feed', icon: 'M6 5c7.182 0 7.182 0 12 0m-6 5c6.667 0 6.667 0 10 0m-6 5c4.571 0 4.571 0 8 0m-10 5l-2 2m0 0l-2-2m2 2v-6' },
    { id: 'posts', label: 'My Posts', icon: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z' },
    { id: 'participations', label: 'My Participations', icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' },
    { id: 'funding', label: 'My Funding', icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599-1M12 8V7m0 1v8m0 0v8m0-6c2 0 1.998-1.586 4-5.143V8c0-3.557-2-6.143-4-6.143z' },
    { id: 'badge', label: 'My Badge', icon: 'M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z' }
  ];

  // Active tab
  activeTab = 'campaigns';

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

  // Participation data
  myParticipations: EventParticipation[] = [];

  // Funding history
  myFundingHistory: FundingHistory[] = [];

  // Badge progress
  eventsAttended = 0;

  // Like tracking for posts
  likedPosts = new Set<number>();

  constructor(
    private authService: AuthService,
    private campaignsService: CampaignsService,
    private eventsService: EventsService,
    private projectsService: ProjectsService,
    private postsService: PostsService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.setDefaultTab();
    this.loadAllData();
  }

  setDefaultTab(): void {
    if (this.isDonor()) {
      this.activeTab = 'campaigns';
    } else {
      this.activeTab = 'feed';
    }
  }

  loadAllData(): void {
    this.isLoading = true;
    this.error = null;

    const requests: Observable<any>[] = [
      this.campaignsService.getAllCampaigns().pipe(catchError(() => of([]))),
      this.eventsService.getAllEvents().pipe(catchError(() => of([]))),
      this.projectsService.getAllProjects().pipe(catchError(() => of([]))),
      this.postsService.getAllPosts().pipe(catchError(() => of([])))
    ];

    forkJoin(requests).subscribe({
      next: (results: any[]) => {
        const allPosts: Post[] = results[3] || [];
        const currentUserName = this.currentUser?.userName;
        // Only show the logged-in user's own posts in "My Posts"
        this.myPosts = currentUserName
          ? allPosts.filter(p => p.creator === currentUserName)
          : allPosts;

        if (this.isDonor()) {
          this.myCampaigns = results[0] || [];
          this.myEvents = results[1] || [];
          this.myProjects = results[2] || [];
          this.calculateImpactStats();
        } else {
          this.feedCampaigns = results[0] || [];
          this.feedEvents = results[1] || [];
          this.feedProjects = results[2] || [];
        }
        this.isLoading = false;
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
    return user?.userType || 'USER';
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
    return this.authService.hasRole('ADMIN');
  }

  // Tab helpers
  getTabs(): DashboardTab[] {
    if (this.isDonor()) {
      return this.donorTabs;
    }
    return this.citizenTabs;
  }

  setActiveTab(tabId: string): void {
    this.activeTab = tabId;
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

  // Badge progress calculation
  getNextBadgeThreshold(): number {
    const points = this.getUserPoints();
    if (points >= 8) return 8;
    if (points >= 5) return 8;
    if (points >= 3) return 5;
    if (points >= 1) return 3;
    return 1;
  }

  getBadgeProgressPercent(): number {
    const current = this.getUserPoints();
    const next = this.getNextBadgeThreshold();
    if (current >= 8) return 100;
    return (current / next) * 100;
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
    const current = this.getUserBadge();
    const badges = ['NONE', 'BRONZE', 'SILVER', 'GOLD', 'PLATINUM'];
    const currentIndex = badges.indexOf(current);
    if (currentIndex < badges.length - 1) {
      return badges[currentIndex + 1];
    }
    return 'PLATINUM';
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
      case 'COMPLETED': return 'bg-blue-100 text-blue-800';
      case 'DRAFT': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  // Event status color
  getEventStatusColor(status: string): string {
    switch (status) {
      case 'UPCOMING': return 'bg-yellow-100 text-yellow-800';
      case 'ONGOING': return 'bg-green-100 text-green-800';
      case 'COMPLETED': return 'bg-blue-100 text-blue-800';
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
      case 'EVENT_ANNOUNCEMENT':    return 'bg-blue-100 text-blue-800';
      case 'CAMPAIGN_ANNOUNCEMENT': return 'bg-green-100 text-green-800';
      case 'TESTIMONIAL':           return 'bg-purple-100 text-purple-800';
      default:                      return 'bg-gray-100 text-gray-700';
    }
  }

  // Refresh data
  refreshData(): void {
    this.loadAllData();
  }
}
