import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LoanService } from '../../core/services/loan.service';

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

  constructor(private router: Router, private loanService: LoanService) {}

  ngOnInit() {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state ?? history.state;
    this.riskScore = state?.['riskScore'];
    this.loanData = state?.['loanData'];
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
    if (!this.loanData) return;
    this.saving = true;
    this.loanService.createLoan({
      loanAmount: this.loanData.loanAmount,
      interestRate: this.loanData.interestRate,
      tenureMonths: this.loanData.tenureMonths,
      status: this.loanData.status,
      disbursementDate: this.loanData.disbursementDate,
      createdAt: new Date().toISOString()
    }).subscribe({
      next: (savedLoan) => {
        const rs = {
          ...this.riskScore,
          loanId: savedLoan.loanId,
          riskScore: this.overrideScore ?? this.riskScore.riskScore,
          riskGrade: this.getGrade(this.overrideScore ?? this.riskScore.riskScore),
          createdAt: new Date().toISOString()
        };
        this.loanService.saveRiskScore(rs).subscribe({
          next: () => { this.saveMsg = '✓ Loan and risk score saved successfully!'; this.saving = false; },
          error: () => { this.saveMsg = '✗ Failed to save. Try again.'; this.saving = false; }
        });
      },
      error: () => { this.saveMsg = '✗ Failed to save loan.'; this.saving = false; }
    });
  }

  getGrade(s: number): string {
    return s >= 80 ? 'A' : s >= 60 ? 'B' : s >= 40 ? 'C' : 'D';
  }

  newApplication() { this.router.navigate(['/new-loan']); }
}
