import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PortfolioService } from '../../core/services/portfolio.service';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, AfterViewInit {
  @ViewChild('sectorChart') sectorRef!: ElementRef;
  @ViewChild('riskDistChart') riskDistRef!: ElementRef;

  loading = true;
  loans: any[] = [];
  riskScores: any[] = [];
  metrics: any = null;

  totalActive = 0;
  totalLoans = 0;
  totalValue = 0;
  par30 = 0;
  defaultForecast = '0';

  private dataReady = false;
  private viewReady = false;
  private sectorChart: any; private riskChart: any;

  constructor(private ps: PortfolioService) {}

  ngOnInit() {
    this.ps.getDashboardData().subscribe({
      next: ({ loans, borrowers, riskScores, metrics }) => {
        this.loans = loans; this.riskScores = riskScores;
        this.metrics = metrics?.[0] ?? null;
        this.totalLoans = loans.length;
        this.totalActive = loans.filter((l: any) => l.status === 'Active').length;
        this.totalValue = loans.reduce((s: number, l: any) => s + (l.loanAmount || 0), 0);
        this.par30 = this.metrics?.par30 ?? 0;
        const avgPd = riskScores.length
          ? riskScores.reduce((s: number, r: any) => s + (r.probabilityDefault || 0), 0) / riskScores.length
          : this.metrics?.forecastDefaultRate ?? 0;
        this.defaultForecast = (avgPd * 100).toFixed(1);

        // Build sector map using borrower data
        const borrowerMap: any = {};
        borrowers.forEach((b: any) => borrowerMap[b.borrowerId] = b);

        const sectorScores: any = {};
        loans.forEach((l: any) => {
          const borrower = borrowerMap[l.borrowerId];
          const sector = borrower?.businessSector ?? 'Unknown';
          const rs = riskScores.find((r: any) => r.loanId === l.loanId);
          if (rs) {
            if (!sectorScores[sector]) sectorScores[sector] = { total: 0, count: 0 };
            sectorScores[sector].total += rs.riskScore || 0;
            sectorScores[sector].count++;
          }
        });

        const gradeMap: any = { A: 0, B: 0, C: 0, D: 0 };
        riskScores.forEach((r: any) => { if (r.riskGrade) gradeMap[r.riskGrade]++; });

        this.loading = false;
        this.dataReady = true;
        if (this.viewReady) this.buildCharts(sectorScores, gradeMap);
        else { (this as any)._sectorScores = sectorScores; (this as any)._gradeMap = gradeMap; }
      }
    });
  }

  ngAfterViewInit() {
    this.viewReady = true;
    if (this.dataReady && (this as any)._sectorScores) {
      this.buildCharts((this as any)._sectorScores, (this as any)._gradeMap);
    }
  }

  buildCharts(sectorScores: any, gradeMap: any) {
    setTimeout(() => {
      const sLabels = Object.keys(sectorScores);
      const sData = sLabels.map(k => +(sectorScores[k].total / sectorScores[k].count).toFixed(1));

      if (this.sectorChart) this.sectorChart.destroy();
      this.sectorChart = new Chart(this.sectorRef.nativeElement, {
        type: 'bar',
        data: {
          labels: sLabels,
          datasets: [{ label: 'Avg Risk Score', data: sData,
            backgroundColor: sData.map(v => v >= 80 ? '#22c55e' : v >= 60 ? '#38bdf8' : v >= 40 ? '#f59e0b' : '#ef4444'),
            borderRadius: 6, borderSkipped: false }]
        },
        options: { indexAxis: 'y', responsive: true, maintainAspectRatio: false,
          plugins: { legend: { display: false } }, scales: { x: { min: 0, max: 100 } } }
      });

      if (this.riskChart) this.riskChart.destroy();
      this.riskChart = new Chart(this.riskDistRef.nativeElement, {
        type: 'doughnut',
        data: {
          labels: ['Grade A (≥80)', 'Grade B (≥60)', 'Grade C (≥40)', 'Grade D (<40)'],
          datasets: [{ data: [gradeMap.A, gradeMap.B, gradeMap.C, gradeMap.D],
            backgroundColor: ['#22c55e', '#38bdf8', '#f59e0b', '#ef4444'], borderWidth: 2, borderColor: '#fff' }]
        },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
      });
    }, 50);
  }
}
