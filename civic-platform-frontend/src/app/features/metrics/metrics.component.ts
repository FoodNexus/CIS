import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MetricsService, DashboardStats } from '@core/services/metrics.service';

@Component({
  selector: 'app-metrics',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './metrics.component.html',
  styleUrls: ['./metrics.component.scss']
})
export class MetricsComponent implements OnInit {
  stats: DashboardStats | null = null;
  isLoading = false;
  errorMessage = '';
  exportLoading = false;

  constructor(private metricsService: MetricsService) {}

  ngOnInit(): void {
    this.loadStats();
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

  exportMetricsPdf(): void {
    this.errorMessage = '';
    this.exportLoading = true;
    this.metricsService.exportMetricsPdf().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        const d = new Date().toISOString().slice(0, 10);
        link.download = `civic-platform-metrics-${d}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.exportLoading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to export metrics PDF';
        this.exportLoading = false;
      }
    });
  }
}
