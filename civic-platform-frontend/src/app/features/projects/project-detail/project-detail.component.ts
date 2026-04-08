import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { EMPTY, Subscription } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { ProjectsService, Project } from '@core/services/projects.service';
import { AuthService } from '@core/services/auth.service';
import { ProjectVoteStateService } from '@core/services/project-vote-state.service';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.scss']
})
export class ProjectDetailComponent implements OnInit, OnDestroy {
  project: Project | null = null;
  isLoading = false;
  errorMessage = '';

  showDonateModal = false;
  donateForm: FormGroup;
  isDonating = false;
  donateSuccess = false;
  hasVoted = false;
  isVoting = false;

  private voteSyncSub?: Subscription;
  private routeSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private projectsService: ProjectsService,
    public readonly voteState: ProjectVoteStateService,
    private authService: AuthService,
    private cd: ChangeDetectorRef,
    private fb: FormBuilder
  ) {
    this.donateForm = this.fb.group({
      amount: [25, [Validators.required, Validators.min(1)]],
      cardHolder: ['', Validators.required],
      cardNumber: ['', [Validators.required, Validators.pattern(/^\d{4}\s?\d{4}\s?\d{4}\s?\d{4}$/)]],
      expiry: ['', [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/)]],
      cvc: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]]
    });
  }

  ngOnInit(): void {
    this.voteSyncSub = this.voteState.changes.subscribe(() => {
      const id = this.project?.id;
      if (id != null && this.voteState.hasLoaded(id)) {
        this.hasVoted = this.voteState.isVoted(id);
        this.cd.markForCheck();
      }
    });

    this.routeSub = this.route.paramMap
      .pipe(
        switchMap((params) => {
          const idStr = params.get('id');
          if (!idStr) {
            return EMPTY;
          }
          const id = Number(idStr);
          this.isLoading = true;
          this.errorMessage = '';
          this.project = null;
          this.cd.markForCheck();
          return this.projectsService.getProjectById(id).pipe(
            map((project) => ({ project, id })),
            catchError((error) => {
              this.errorMessage =
                error.error?.message || 'Failed to load project';
              this.isLoading = false;
              this.cd.markForCheck();
              return EMPTY;
            })
          );
        })
      )
      .subscribe(({ project, id }) => {
        this.project = project;
        this.isLoading = false;
        this.applyVoteFlagFromServer(id);
        this.cd.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.voteSyncSub?.unsubscribe();
    this.routeSub?.unsubscribe();
  }

  /**
   * Prefer shared state (e.g. user voted from list) so list/detail never disagree;
   * otherwise load from GET /has-voted once.
   */
  private applyVoteFlagFromServer(projectId: number): void {
    if (!this.authService.isLoggedIn()) {
      this.hasVoted = false;
      this.cd.markForCheck();
      return;
    }
    if (this.voteState.hasLoaded(projectId)) {
      this.hasVoted = this.voteState.isVoted(projectId);
      this.cd.markForCheck();
      return;
    }
    this.projectsService.hasVoted(projectId).subscribe({
      next: (v) => {
        this.voteState.setFromServer(projectId, v);
        this.hasVoted = v;
        this.cd.markForCheck();
      },
      error: () => {
        this.voteState.setFromServer(projectId, false);
        this.hasVoted = false;
        this.cd.markForCheck();
      }
    });
  }

  vote(): void {
    if (!this.project || this.hasVoted || this.isVoting) {
      return;
    }
    this.isVoting = true;
    this.errorMessage = '';
    const pid = this.project.id;
    this.projectsService.voteForProject(pid).subscribe({
      next: () => {
        this.project!.voteCount = (this.project!.voteCount || 0) + 1;
        this.voteState.markVoted(pid);
        this.hasVoted = true;
        this.isVoting = false;
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
          this.voteState.markVoted(pid);
          this.hasVoted = true;
        }
        this.isVoting = false;
        this.cd.markForCheck();
      }
    });
  }

  openDonateModal(): void {
    this.showDonateModal = true;
    this.donateSuccess = false;
    this.errorMessage = '';
    this.donateForm.reset({ amount: 25 });
  }

  closeDonateModal(): void {
    this.showDonateModal = false;
  }

  formatCardNumber(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '').substring(0, 16);
    value = value.replace(/(.{4})/g, '$1 ').trim();
    input.value = value;
    this.donateForm.get('cardNumber')?.setValue(value, { emitEvent: false });
  }

  formatExpiry(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '').substring(0, 4);
    if (value.length >= 3) {
      value = value.substring(0, 2) + '/' + value.substring(2);
    }
    input.value = value;
    this.donateForm.get('expiry')?.setValue(value, { emitEvent: false });
  }

  submitDonation(): void {
    if (this.donateForm.invalid || !this.project) {
      this.donateForm.markAllAsTouched();
      return;
    }
    this.isDonating = true;
    this.errorMessage = '';

    this.projectsService.fundProject({
      projectId: this.project.id,
      amount: this.donateForm.value.amount,
      paymentMethod: 'CARD'
    }).subscribe({
      next: () => {
        this.project!.currentAmount += this.donateForm.value.amount;
        this.isDonating = false;
        this.donateSuccess = true;
        setTimeout(() => {
          this.closeDonateModal();
        }, 2500);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Donation failed. Please try again.';
        this.isDonating = false;
      }
    });
  }

  getProgress(): number {
    if (!this.project?.goalAmount || this.project.goalAmount === 0) return 0;
    return Math.min(100, Math.round((this.project.currentAmount / this.project.goalAmount) * 100));
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'COMPLETED': return 'bg-blue-100 text-blue-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      default: return 'bg-gray-100 text-gray-700';
    }
  }
}
