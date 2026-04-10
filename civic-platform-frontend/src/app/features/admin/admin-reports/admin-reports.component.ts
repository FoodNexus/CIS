import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReportsService } from '@core/services/reports.service';

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-reports.component.html',
  styleUrls: ['./admin-reports.component.scss']
})
export class AdminReportsComponent implements OnInit {
  reportFrom = '';
  reportTo = '';
  reportType = '';
  reportFormat: 'pdf' | 'csv' = 'pdf';
  reportDownloading = false;
  reportError = '';

  constructor(private reportsService: ReportsService) {}

  ngOnInit(): void {
    const y = new Date().getFullYear();
    this.reportFrom = `${y}-01-01`;
    this.reportTo = `${y}-12-31`;
  }

  download(): void {
    this.reportError = '';
    if (!this.reportFrom || !this.reportTo) {
      this.reportError = 'Choose both from and to dates.';
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
          link.download = `civic-platform-user-activity-${this.reportFrom}-to-${this.reportTo}.${ext}`;
          link.click();
          window.URL.revokeObjectURL(url);
          this.reportDownloading = false;
        },
        error: (err) => {
          this.reportDownloading = false;
          this.reportError =
            err?.error?.message || err?.message || 'Failed to generate report.';
        }
      });
  }
}
