import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardStats {
  totalUsersByType?: Record<string, number>;
  totalCampaignsByStatus?: Record<string, number>;
  totalFundingAmount?: number;
  totalCo2Saved?: number;
  totalMealsDistributed?: number;
  mostActiveRegion?: string;
  totalProjects?: number;
  totalEvents?: number;
  activeVolunteers?: number;
  activeDonors?: number;
  activeAssociations?: number;
}

export interface ImpactMetrics {
  peopleHelped: number;
  mealsServed: number;
  volunteersEngaged: number;
  fundsRaised: number;
  campaignsCompleted: number;
  eventsHeld: number;
}

@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.API_URL}/admin/dashboard`);
  }

  getDailyMetrics(date: string): Observable<ImpactMetrics> {
    return this.http.get<ImpactMetrics>(`${this.API_URL}/metrics/daily?date=${date}`);
  }

  getMonthlyMetrics(year: number, month: number): Observable<ImpactMetrics> {
    return this.http.get<ImpactMetrics>(`${this.API_URL}/metrics/monthly?year=${year}&month=${month}`);
  }

  getYearlyMetrics(year: number): Observable<ImpactMetrics> {
    return this.http.get<ImpactMetrics>(`${this.API_URL}/metrics/yearly?year=${year}`);
  }

  /** Admin-only: platform KPI / environmental snapshot PDF (not user lists — use Reports). */
  exportMetricsPdf(): Observable<Blob> {
    return this.http.get(`${this.API_URL}/pdf/metrics`, { responseType: 'blob' });
  }
}
