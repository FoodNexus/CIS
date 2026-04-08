import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly API_URL = 'http://localhost:8081/api/admin/reports';

  constructor(private http: HttpClient) {}

  generateReport(params: {
    from: string;
    to: string;
    type?: string;
    format: 'pdf' | 'csv';
  }): Observable<Blob> {
    let httpParams = new HttpParams()
      .set('from', params.from)
      .set('to', params.to)
      .set('format', params.format);
    if (params.type) {
      httpParams = httpParams.set('type', params.type);
    }
    return this.http.get(this.API_URL, {
      params: httpParams,
      responseType: 'blob'
    });
  }
}
