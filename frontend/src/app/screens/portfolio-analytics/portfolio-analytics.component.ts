import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PortfolioService } from '../../core/services/portfolio.service';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-portfolio-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portfolio-analytics.component.html',
  styleUrl: './portfolio-analytics.component.scss'
})
export class PortfolioAnalyticsComponent implements OnInit, AfterViewInit {
  @ViewChild('sectorChart') sectorRef!: ElementRef;
  @ViewChild('regionChart') regionRef!: ElementRef;
  @ViewChild('trendChart')  trendRef!: ElementRef;
  @ViewChild('defaultChart') defaultRef!: ElementRef;

  loading = true;
  private dataReady = false; private viewReady = false;
  private chartData: any = {};
  charts: Chart[] = [];

  constructor(private ps: PortfolioService) {}

  ngOnInit() {
    this.ps.getPortfolioData().subscribe({
      next: ({ loans, borrowers, riskScores }) => {
        const borrowerMap: any = {};
        borrowers.forEach((b: any) => borrowerMap[b.borrowerId] = b);

        // Sector exposure (loan amounts)
        const sectorAmt: any = {};
        loans.forEach((l: any) => {
          const sector = borrowerMap[l.borrowerId]?.businessSector ?? 'Unknown';
          sectorAmt[sector] = (sectorAmt[sector] ?? 0) + (l.loanAmount ?? 0);
        });

        // Regional exposure (loan count)
        const regionCount: any = {};
        loans.forEach((l: any) => {
          const loc = borrowerMap[l.borrowerId]?.location ?? 'Unknown';
          regionCount[loc] = (regionCount[loc] ?? 0) + 1;
        });

        // Risk trend over time (group by createdAt month)
        const monthScores: any = {};
        riskScores.forEach((r: any) => {
          const d = new Date(r.createdAt);
          const key = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;
          if (!monthScores[key]) monthScores[key] = { total: 0, count: 0 };
          monthScores[key].total += r.riskScore ?? 0;
          monthScores[key].count++;
        });
        const trendMonths = Object.keys(monthScores).sort();
        const trendVals = trendMonths.map(k => +(monthScores[k].total / monthScores[k].count).toFixed(1));

        // Default trend
        const monthPd: any = {};
        riskScores.forEach((r: any) => {
          const d = new Date(r.createdAt);
          const key = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;
          if (!monthPd[key]) monthPd[key] = { total: 0, count: 0 };
          monthPd[key].total += r.probabilityDefault ?? 0;
          monthPd[key].count++;
        });
        const pdMonths = Object.keys(monthPd).sort();
        const pdVals = pdMonths.map(k => +((monthPd[k].total / monthPd[k].count) * 100).toFixed(2));

        this.chartData = { sectorAmt, regionCount, trendMonths, trendVals, pdMonths, pdVals };
        this.loading = false; this.dataReady = true;
        if (this.viewReady) this.buildCharts();
      }
    });
  }

  ngAfterViewInit() { this.viewReady = true; if (this.dataReady) this.buildCharts(); }

  buildCharts() {
    setTimeout(() => {
      const { sectorAmt, regionCount, trendMonths, trendVals, pdMonths, pdVals } = this.chartData;
      const cols = ['#38bdf8','#0284c7','#22c55e','#f59e0b','#ef4444','#8b5cf6','#ec4899'];

      this.charts.forEach(c => c.destroy());
      this.charts = [];

      this.charts.push(new Chart(this.sectorRef.nativeElement, {
        type: 'doughnut',
        data: { labels: Object.keys(sectorAmt), datasets: [{ data: Object.values(sectorAmt), backgroundColor: cols, borderWidth: 2, borderColor: '#fff' }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
      }));

      this.charts.push(new Chart(this.regionRef.nativeElement, {
        type: 'bar',
        data: { labels: Object.keys(regionCount), datasets: [{ label: 'Loans', data: Object.values(regionCount), backgroundColor: cols, borderRadius: 6 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
      }));

      this.charts.push(new Chart(this.trendRef.nativeElement, {
        type: 'line',
        data: { labels: trendMonths, datasets: [{ label: 'Avg Risk Score', data: trendVals, borderColor: '#0284c7', backgroundColor: 'rgba(56,189,248,0.1)', fill: true, tension: 0.4, pointRadius: 5 }] },
        options: { responsive: true, maintainAspectRatio: false, scales: { y: { min: 0, max: 100 } } }
      }));

      this.charts.push(new Chart(this.defaultRef.nativeElement, {
        type: 'line',
        data: { labels: pdMonths, datasets: [{ label: 'Predicted Default %', data: pdVals, borderColor: '#ef4444', backgroundColor: 'rgba(239,68,68,0.1)', fill: true, tension: 0.4, pointRadius: 5 }] },
        options: { responsive: true, maintainAspectRatio: false, scales: { y: { min: 0 } } }
      }));
    }, 50);
  }
}
