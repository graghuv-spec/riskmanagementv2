import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, switchMap } from 'rxjs/operators';
import { LoanService } from '../../core/services/loan.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-risk-result',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './risk-result.component.html',
  styleUrl: './risk-result.component.scss'
})
export class RiskResultComponent implements OnInit {
  riskScore: any = null;
  loanData: any = null;
  overrideScore: number | null = null;
  saveMsg = '';
  saving = false;

  drivers = [
    { label: 'Income Stability',  weight: 30, icon: '💰' },
    { label: 'Repayment History', weight: 30, icon: '📋' },
    { label: 'Collateral Ratio',  weight: 20, icon: '🏠' },
    { label: 'Sector Risk',       weight: 10, icon: '🏢' },
    { label: 'Location Risk',     weight: 10, icon: '📍' }
  ];

  constructor(private router: Router, private loanService: LoanService, private auth: AuthService) {}

  ngOnInit() {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state ?? history.state;
    this.riskScore = state?.['riskScore'];
    this.loanData = state?.['loanData'];

    if (!this.riskScore) {
      const raw = sessionStorage.getItem('rm_latest_risk_result');
      if (raw) {
        try {
          const cached = JSON.parse(raw);
          this.riskScore = cached?.riskScore ?? null;
          this.loanData = cached?.loanData ?? null;
        } catch {
          // Ignore malformed cache and continue to guarded redirect.
        }
      }
    }

    if (!this.riskScore) this.router.navigate(['/new-loan']);
  }

  get scoreColor(): string {
    const s = this.riskScore?.riskScore ?? 0;
    if (s >= 80) return '#22c55e';
    if (s >= 60) return '#38bdf8';
    if (s >= 40) return '#f59e0b';
    return '#ef4444';
  }

  get gradeClass(): string {
    const g = this.riskScore?.riskGrade ?? '';
    return g === 'A' ? 'grade-a' : g === 'B' ? 'grade-b' : g === 'C' ? 'grade-c' : 'grade-d';
  }

  get pdPercent(): string {
    return ((this.riskScore?.probabilityDefault ?? 0) * 100).toFixed(1);
  }

  get driverContributions(): any[] {
    const s = this.riskScore?.riskScore ?? 0;
    return this.drivers.map(d => ({
      ...d,
      value: Math.round(s * (d.weight / 100))
    }));
  }

  get explanation(): string {
    try {
      const json = JSON.parse(this.riskScore?.explanationJson ?? '{}');
      return json.method ?? 'Rule-based scoring using income, collateral, sector and repayment data.';
    } catch { return 'Rule-based scoring model using income stability, collateral ratio, sector risk and repayment history.'; }
  }

  saveWithOverride() {
    if (!this.loanData || !this.riskScore || this.saving) return;

    if (this.overrideScore !== null && (this.overrideScore < 0 || this.overrideScore > 100)) {
      this.saveMsg = '✗ Override score must be between 0 and 100.';
      return;
    }

    this.saveMsg = '';
    this.saving = true;

    const scoreToSave = this.overrideScore ?? this.riskScore.riskScore;
    const user = this.auth.getUser();
    const institutionId = user?.institutionId ?? null;
    const now = this.toApiDateTime(new Date().toISOString());

    const borrowerPayload = {
      fullName: this.loanData.fullName,
      nationalId: this.loanData.nationalId,
      gender: this.loanData.gender,
      age: Number(this.loanData.age),
      location: this.loanData.location,
      businessSector: this.loanData.businessSector,
      monthlyIncome: Number(this.loanData.monthlyIncome),
      collateralValue: Number(this.loanData.collateralValue),
      institutionId,
      createdAt: now
    };

    this.loanService.createBorrower(borrowerPayload).pipe(
      switchMap((savedBorrower) => {
        const loanPayload = {
          borrowerId: savedBorrower?.borrowerId ?? null,
          institutionId,
          loanAmount: Number(this.loanData.loanAmount),
          interestRate: Number(this.loanData.interestRate),
          tenureMonths: Number(this.loanData.tenureMonths),
          status: this.loanData.status,
          disbursementDate: this.toApiDateTime(this.loanData.disbursementDate),
          createdAt: now
        };
        return this.loanService.createLoan(loanPayload);
      }),
      switchMap((savedLoan) => {
        const rs = {
          ...this.riskScore,
          loanId: savedLoan.loanId,
          riskScore: scoreToSave,
          riskGrade: this.getGrade(scoreToSave),
          createdAt: now
        };
        return this.loanService.saveRiskScore(rs);
      }),
      finalize(() => {
        this.saving = false;
      })
    ).subscribe({
      next: () => {
        this.saveMsg = '✓ Borrower, loan and risk score saved successfully!';
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 401 || err.status === 403) {
          this.saveMsg = '✗ Session expired. Please login and try saving again.';
          return;
        }
        this.saveMsg = '✗ Failed to save complete flow. Please try again.';
      }
    });
  }

  private toApiDateTime(value: string | null | undefined): string | null {
    if (!value) return null;
    const trimmed = String(value).trim();
    if (!trimmed) return null;
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
      return `${trimmed}T00:00:00`;
    }
    return trimmed.endsWith('Z') ? trimmed.slice(0, 19) : trimmed;
  }

  getGrade(s: number): string {
    return s >= 80 ? 'A' : s >= 60 ? 'B' : s >= 40 ? 'C' : 'D';
  }

  newApplication() { this.router.navigate(['/new-loan']); }
}
