import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export enum CampaignType {
  FOOD_COLLECTION = 'FOOD_COLLECTION',
  FUNDRAISING = 'FUNDRAISING',
  VOLUNTEER = 'VOLUNTEER',
  AWARENESS = 'AWARENESS'
}

export enum CampaignStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export interface Campaign {
  id: number;
  name: string;
  description?: string;
  type: CampaignType;
  status: CampaignStatus;
  startDate?: string;
  endDate?: string;
  neededAmount?: number;
  goalKg?: number;
  goalMeals?: number;
  goalAmount?: number;
  currentKg?: number;
  currentMeals?: number;
  hashtag?: string;
  voteCount: number;
  /** Present when returned from the ML recommendation feed. */
  isRecommended?: boolean;
  createdAt?: string;
  createdById?: number;
  createdByName?: string;
  progressPercentage?: number;
}

export interface CampaignRequest {
  name: string;
  description?: string;
  type: CampaignType;
  startDate?: string;
  endDate?: string;
  neededAmount?: number;
  goalKg?: number;
  goalMeals?: number;
  goalAmount?: number;
  hashtag?: string;
}

@Injectable({ providedIn: 'root' })
export class CampaignsService {
  private readonly API_URL = 'http://localhost:8081/api/campaigns';

  constructor(private http: HttpClient) {}

  getAllCampaigns(): Observable<Campaign[]> {
    return this.http.get<Campaign[]>(this.API_URL);
  }

  getCampaignById(id: number): Observable<Campaign> {
    return this.http.get<Campaign>(`${this.API_URL}/${id}`);
  }

  createCampaign(campaignData: CampaignRequest): Observable<Campaign> {
    return this.http.post<Campaign>(this.API_URL, campaignData);
  }

  updateCampaign(id: number, campaignData: Partial<CampaignRequest>): Observable<Campaign> {
    return this.http.put<Campaign>(`${this.API_URL}/${id}`, campaignData);
  }

  deleteCampaign(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  voteForCampaign(id: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${id}/vote`, {});
  }

  hasVoted(id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.API_URL}/${id}/has-voted`);
  }

  launchCampaign(id: number): Observable<Campaign> {
    return this.http.post<Campaign>(`${this.API_URL}/${id}/launch`, {});
  }

  closeCampaign(id: number): Observable<Campaign> {
    return this.http.post<Campaign>(`${this.API_URL}/${id}/close`, {});
  }

  cancelCampaign(id: number): Observable<Campaign> {
    return this.http.post<Campaign>(`${this.API_URL}/${id}/cancel`, {});
  }
}
