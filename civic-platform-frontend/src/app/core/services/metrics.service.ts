import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardStats {
  totalUsers: number;
  totalProjects: number;
  totalEvents: number;
  totalCampaigns: number;
  totalPosts: number;
  activeVolunteers: number;
  totalFundingAmount: number;
  monthlyDonations: number;
  mostActiveRegion?: string;
  totalMealsDistributed?: number;
  totalUsersByType?: Record<string, number>;
  totalCo2Saved?: number;
  totalCampaignsByStatus?: Record<string, number>;
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
  private readonly API_URL = 'http://localhost:8180/api';

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

  exportMetricsPdf(): Observable<Blob> {
    return this.http.get(`${this.API_URL}/pdf/metrics`, { responseType: 'blob' });
  }
}
