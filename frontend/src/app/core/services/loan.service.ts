import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LoanService {
  private readonly BASE = '/api';

  constructor(private http: HttpClient) {}

  getLoans(): Observable<any[]> { return this.http.get<any[]>(`${this.BASE}/loans`); }
  getLoan(id: number): Observable<any> { return this.http.get<any>(`${this.BASE}/loans/${id}`); }
  createLoan(loan: any): Observable<any> { return this.http.post<any>(`${this.BASE}/loans`, loan); }
  updateLoan(id: number, loan: any): Observable<any> { return this.http.put<any>(`${this.BASE}/loans/${id}`, loan); }

  calculateRiskScore(req: any): Observable<any> {
    return this.http.post<any>(`${this.BASE}/risk-scores/calculate`, req);
  }

  saveRiskScore(rs: any): Observable<any> {
    return this.http.post<any>(`${this.BASE}/risk-scores`, rs);
  }
}
