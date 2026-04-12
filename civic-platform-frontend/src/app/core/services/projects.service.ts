import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Project {
  id: number;
  title: string;
  description?: string;
  goalAmount: number;
  currentAmount: number;
  voteCount: number;
  status?: string;
  startDate?: string;
  completionDate?: string;
  finalReport?: string;
  organizerType?: string;
  createdAt?: string;
  createdById?: number;
  /** Present when returned from the ML recommendation feed. */
  isRecommended?: boolean;
  fundingPercentage?: number;
  fullyFunded?: boolean;
}

export interface ProjectFunding {
  id: number;
  projectId: number;
  amount: number;
  fundedAt: string;
}

/** Matches backend ProjectFundingResponse (GET /projects/my-fundings) */
export interface ProjectFundingHistory {
  id: number;
  projectId: number;
  projectTitle: string;
  amount: number;
  fundDate: string;
  paymentMethod?: string;
}

export interface ProjectRequest {
  title: string;
  description?: string;
  goalAmount: number;
  organizerType?: string;
}

export interface ProjectFundingRequest {
  projectId: number;
  amount: number;
  paymentMethod?: string;
}

@Injectable({ providedIn: 'root' })
export class ProjectsService {
  private readonly API_URL = 'http://localhost:8081/api/projects';

  constructor(private http: HttpClient) {}

  getAllProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(this.API_URL);
  }

  getProjectById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.API_URL}/${id}`);
  }

  createProject(projectData: ProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.API_URL, projectData);
  }

  updateProject(id: number, projectData: Partial<ProjectRequest>): Observable<Project> {
    return this.http.put<Project>(`${this.API_URL}/${id}`, projectData);
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  voteForProject(id: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${id}/vote`, {});
  }

  /** Whether the current user has already voted for this project (requires auth). */
  hasVoted(id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.API_URL}/${id}/has-voted`);
  }

  fundProject(fundingData: ProjectFundingRequest): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${fundingData.projectId}/fund`, fundingData);
  }

  getMyFundings(): Observable<ProjectFundingHistory[]> {
    return this.http.get<ProjectFundingHistory[]>(`${this.API_URL}/my-fundings`);
  }
}
