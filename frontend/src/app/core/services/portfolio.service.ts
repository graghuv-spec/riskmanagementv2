import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  private readonly BASE = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getLoans(): Observable<any[]> { return this.http.get<any[]>(`${this.BASE}/loans`); }
  getBorrowers(): Observable<any[]> { return this.http.get<any[]>(`${this.BASE}/borrowers`); }
  getRiskScores(): Observable<any[]> { return this.http.get<any[]>(`${this.BASE}/risk-scores`); }
  getRepayments(): Observable<any[]> { return this.http.get<any[]>(`${this.BASE}/repayments`); }
  getMetrics(): Observable<any[]> { return this.http.get<any[]>(`${this.BASE}/portfolio-metrics`); }

  getDashboardData(): Observable<any> {
    return forkJoin({
      loans: this.getLoans(),
      borrowers: this.getBorrowers(),
      riskScores: this.getRiskScores(),
      metrics: this.getMetrics()
    });
  }

  getPortfolioData(): Observable<any> {
    return forkJoin({
      loans: this.getLoans(),
      borrowers: this.getBorrowers(),
      riskScores: this.getRiskScores(),
      repayments: this.getRepayments()
    });
  }
}
