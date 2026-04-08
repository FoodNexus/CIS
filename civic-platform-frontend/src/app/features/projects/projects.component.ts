import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ProjectsService, Project } from '@core/services/projects.service';
import { AuthService } from '@core/services/auth.service';
import { ProjectVoteStateService } from '@core/services/project-vote-state.service';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.scss']
})
export class ProjectsComponent implements OnInit, OnDestroy {
  allProjects: Project[] = [];
  isLoading = false;
  errorMessage = '';
  searchQuery = '';
  statusFilter = '';
  sortBy: 'votes' | 'title' | 'progress' = 'votes';
  votingId: number | null = null;
  /** True while GET has-voted calls are in flight for the list. */
  voteFlagsLoading = false;

  readonly statuses = ['SUBMITTED', 'FULLY_FUNDED', 'COMPLETED'];

  private voteSub?: Subscription;

  constructor(
    public readonly voteState: ProjectVoteStateService,
    private projectsService: ProjectsService,
    private authService: AuthService,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.voteSub = this.voteState.changes.subscribe(() => this.cd.markForCheck());
    this.loadProjects();
  }

  ngOnDestroy(): void {
    this.voteSub?.unsubscribe();
  }

  canCreateProject(): boolean {
    return this.authService.isDonor() || this.authService.isAmbassador();
  }

  get filteredProjects(): Project[] {
    let list = [...this.allProjects];
    const q = this.searchQuery.trim().toLowerCase();
    if (q) {
      list = list.filter(
        (p) =>
          p.title.toLowerCase().includes(q) ||
          (p.description && p.description.toLowerCase().includes(q))
      );
    }
    if (this.statusFilter) {
      list = list.filter((p) => (p.status || '') === this.statusFilter);
    }
    switch (this.sortBy) {
      case 'title':
        list.sort((a, b) => a.title.localeCompare(b.title));
        break;
      case 'progress':
        list.sort(
          (a, b) => this.getFundingProgressPct(b) - this.getFundingProgressPct(a)
        );
        break;
      case 'votes':
      default:
        list.sort((a, b) => (b.voteCount || 0) - (a.voteCount || 0));
        break;
    }
    return list;
  }

  get totalVotesInView(): number {
    return this.filteredProjects.reduce((s, p) => s + (p.voteCount || 0), 0);
  }

  get fundedCountInView(): number {
    return this.filteredProjects.filter((p) => this.isFullyFunded(p)).length;
  }

  loadProjects(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.projectsService.getAllProjects().subscribe({
      next: (projects) => {
        this.allProjects = projects;
        this.isLoading = false;
        this.loadVoteFlags();
        this.cd.markForCheck();
      },
      error: (error) => {
        this.errorMessage =
          error.error?.message || 'Impossible de charger les projets.';
        this.isLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  private loadVoteFlags(): void {
    if (!this.authService.isLoggedIn()) {
      return;
    }
    const ids = this.allProjects.map((p) => p.id);
    if (ids.length === 0) {
      return;
    }
    this.voteFlagsLoading = true;
    forkJoin(
      ids.map((id) =>
        this.projectsService.hasVoted(id).pipe(
          map((v) => ({ projectId: id, voted: v })),
          catchError(() => of({ projectId: id, voted: false }))
        )
      )
    ).subscribe({
      next: (rows) => {
        this.voteState.mergeFromServer(rows);
        this.voteFlagsLoading = false;
        this.cd.markForCheck();
      },
      error: () => {
        this.voteFlagsLoading = false;
        this.cd.markForCheck();
      }
    });
  }

  getFundingProgressPct(p: Project): number {
    const goal = Number(p.goalAmount ?? 0);
    const cur = Number(p.currentAmount ?? 0);
    if (!goal || goal <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((cur / goal) * 100));
  }

  isFullyFunded(p: Project): boolean {
    if (p.fullyFunded) {
      return true;
    }
    const goal = Number(p.goalAmount ?? 0);
    const cur = Number(p.currentAmount ?? 0);
    return goal > 0 && cur >= goal;
  }

  getStatusPillClass(status: string | undefined): string {
    switch (status) {
      case 'SUBMITTED':
        return 'bg-emerald-50 text-emerald-800 ring-1 ring-emerald-200';
      case 'FULLY_FUNDED':
        return 'bg-sky-50 text-sky-800 ring-1 ring-sky-200';
      case 'COMPLETED':
        return 'bg-violet-50 text-violet-800 ring-1 ring-violet-200';
      default:
        return 'bg-gray-100 text-gray-700 ring-1 ring-gray-200';
    }
  }

  getHeaderAccentClass(status: string | undefined): string {
    switch (status) {
      case 'COMPLETED':
        return 'from-slate-600 to-slate-800';
      case 'FULLY_FUNDED':
        return 'from-sky-500 to-indigo-600';
      default:
        return 'from-emerald-500 to-emerald-700';
    }
  }

  hasVoteDisabled(p: Project): boolean {
    return this.voteFlagsLoading || this.voteState.isVoted(p.id);
  }

  vote(p: Project, ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    if (this.hasVoteDisabled(p) || this.votingId != null) {
      return;
    }
    this.votingId = p.id;
    this.projectsService.voteForProject(p.id).subscribe({
      next: () => {
        p.voteCount = (p.voteCount || 0) + 1;
        this.voteState.markVoted(p.id);
        this.votingId = null;
        this.errorMessage = '';
        this.cd.markForCheck();
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        const msg = err.error?.message || 'Vote impossible.';
        this.errorMessage = msg;
        if (
          err.status === 409 ||
          err.status === 400 ||
          /already voted|déjà voté/i.test(msg)
        ) {
          this.voteState.markVoted(p.id);
        }
        this.votingId = null;
        this.cd.markForCheck();
      }
    });
  }
}
