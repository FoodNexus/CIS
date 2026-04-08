import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  CampaignsService,
  CampaignRequest,
  CampaignType
} from '@core/services/campaigns.service';

@Component({
  selector: 'app-campaign-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './campaign-form.component.html',
  styleUrls: ['./campaign-form.component.scss']
})
export class CampaignFormComponent implements OnInit {
  campaignForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  isEdit = false;
  campaignId: number | null = null;
  campaignTypes = Object.values(CampaignType);

  constructor(
    private fb: FormBuilder,
    private campaignsService: CampaignsService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.campaignForm = this.fb.group(
      {
        name: ['', [Validators.required]],
        type: [CampaignType.FOOD_COLLECTION, [Validators.required]],
        description: ['', [Validators.required]],
        hashtag: [''],
        neededAmount: [null as number | null, [Validators.required, Validators.min(1)]],
        goalKg: [null as number | null],
        goalMeals: [null as number | null],
        startDate: ['', [Validators.required]],
        endDate: ['', [Validators.required]]
      },
      { validators: [this.dateOrderValidator] }
    );
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.campaignId = Number(id);
      this.loadCampaign(this.campaignId);
    }
  }

  private dateOrderValidator(group: AbstractControl): ValidationErrors | null {
    const start = group.get('startDate')?.value;
    const end = group.get('endDate')?.value;
    if (!start || !end) {
      return null;
    }
    const ds = new Date(start);
    const de = new Date(end);
    if (de > ds) {
      return null;
    }
    return { dateOrder: true };
  }

  private toDateInput(v: string | undefined): string {
    if (!v) {
      return '';
    }
    const s = String(v);
    return s.length >= 10 ? s.slice(0, 10) : s;
  }

  private normalizeHashtag(raw: string): string | undefined {
    const t = raw.trim();
    if (!t) {
      return undefined;
    }
    return t.startsWith('#') ? t : `#${t}`;
  }

  loadCampaign(id: number): void {
    this.isLoading = true;
    this.campaignsService.getCampaignById(id).subscribe({
      next: (c) => {
        this.campaignForm.patchValue({
          name: c.name,
          type: c.type,
          description: c.description,
          hashtag: c.hashtag || '',
          neededAmount: c.neededAmount ?? null,
          goalKg: c.goalKg ?? null,
          goalMeals: c.goalMeals ?? null,
          startDate: this.toDateInput(c.startDate as string),
          endDate: this.toDateInput(c.endDate as string)
        });
        this.isLoading = false;
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Chargement impossible';
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.campaignForm.invalid) {
      this.campaignForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    const raw = this.campaignForm.value;
    const campaignRequest: CampaignRequest = {
      name: raw.name,
      type: raw.type,
      description: raw.description,
      hashtag: this.normalizeHashtag(raw.hashtag),
      neededAmount: Number(raw.neededAmount),
      goalKg: raw.goalKg != null && raw.goalKg !== '' ? Number(raw.goalKg) : undefined,
      goalMeals: raw.goalMeals != null && raw.goalMeals !== '' ? Number(raw.goalMeals) : undefined,
      startDate: raw.startDate,
      endDate: raw.endDate
    };

    if (this.isEdit && this.campaignId != null) {
      this.campaignsService.updateCampaign(this.campaignId, campaignRequest).subscribe({
        next: (c) => {
          this.router.navigate(['/campaigns', c.id], { queryParams: { success: '1' } });
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Échec de la mise à jour';
          this.isLoading = false;
        }
      });
    } else {
      this.campaignsService.createCampaign(campaignRequest).subscribe({
        next: (c) => {
          this.router.navigate(['/campaigns', c.id], { queryParams: { success: '1' } });
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Échec de la création';
          this.isLoading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/campaigns']);
  }
}
