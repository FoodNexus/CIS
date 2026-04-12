import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FeedResponse } from '@core/models/feed.model';

@Injectable({ providedIn: 'root' })
export class RecommendationsService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getFeed(): Observable<FeedResponse> {
    return this.http.get<FeedResponse>(`${this.API_URL}/recommendations/feed`);
  }
}
