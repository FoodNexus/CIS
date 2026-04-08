import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MetricsService, DashboardStats, ImpactMetrics } from '@core/services/metrics.service';
import { ReportsService } from '@core/services/reports.service';

@Component({
  selector: 'app-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './metrics.component.html',
  styleUrls: ['./metrics.component.scss']
})
export class MetricsComponent implements OnInit {
  stats: DashboardStats | null = null;
  metrics: ImpactMetrics | null = null;
  isLoading = false;
  errorMessage = '';

  reportFrom = '';
  reportTo = '';
  reportType = '';
  reportFormat: 'pdf' | 'csv' = 'pdf';
  reportDownloading = false;
  reportError = '';

  constructor(
    private metricsService: MetricsService,
    private reportsService: ReportsService
  ) {}

  ngOnInit(): void {
    const y = new Date().getFullYear();
    this.reportFrom = `${y}-01-01`;
    this.reportTo = `${y}-12-31`;
    this.loadStats();
    this.loadMetrics();
  }

  loadStats(): void {
    this.isLoading = true;
    this.metricsService.getDashboardStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.isLoading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to load stats';
        this.isLoading = false;
      }
    });
  }

  loadMetrics(): void {
    const today = new Date().toISOString().split('T')[0];
    this.metricsService.getDailyMetrics(today).subscribe({
      next: (metrics) => {
        this.metrics = metrics;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to load metrics';
      }
    });
  }

  exportPdf(): void {
    this.metricsService.exportMetricsPdf().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'metrics-report.pdf';
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to export PDF';
      }
    });
  }

  downloadUserReport(): void {
    this.reportError = '';
    if (!this.reportFrom || !this.reportTo) {
      this.reportError = 'Please choose both from and to dates.';
      return;
    }
    this.reportDownloading = true;
    this.reportsService
      .generateReport({
        from: this.reportFrom,
        to: this.reportTo,
        type: this.reportType || undefined,
        format: this.reportFormat
      })
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          const ext = this.reportFormat === 'pdf' ? 'pdf' : 'csv';
          const typePart = this.reportType ? `-${this.reportType}` : '';
          link.download = `report-${this.reportFrom}-to-${this.reportTo}${typePart}.${ext}`;
          link.click();
          window.URL.revokeObjectURL(url);
          this.reportDownloading = false;
        },
        error: (err) => {
          this.reportDownloading = false;
          this.reportError =
            err?.error?.message || err?.message || 'Failed to generate report. Check date range and permissions.';
        }
      });
  }
}
