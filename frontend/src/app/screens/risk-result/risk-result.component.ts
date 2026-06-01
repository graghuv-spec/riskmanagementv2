import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
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
  loadingResult = false;
  loadError = '';

  drivers = [
    { label: 'Income Stability',  weight: 30, icon: '💰' },
    { label: 'Repayment History', weight: 30, icon: '📋' },
    { label: 'Collateral Ratio',  weight: 20, icon: '🏠' },
    { label: 'Sector Risk',       weight: 10, icon: '🏢' },
    { label: 'Location Risk',     weight: 10, icon: '📍' }
  ];

  constructor(private router: Router, private route: ActivatedRoute, private loanService: LoanService) {
    // Capture navigation state in constructor where getCurrentNavigation() is still available.
    // In ngOnInit of lazy-loaded components, getCurrentNavigation() returns null.
    const nav = this.router.getCurrentNavigation();
    if (nav?.extras?.state) {
      this.riskScore = nav.extras.state['riskScore'] ?? null;
      this.loanData = nav.extras.state['loanData'] ?? null;
    }
  }

  ngOnInit() {
    // Fallback 1: history.state (available after navigation completes)
    if (!this.riskScore) {
      const state = history.state;
      this.riskScore = state?.['riskScore'] ?? null;
      this.loanData = state?.['loanData'] ?? null;
    }

    // Fallback 2: sessionStorage (set synchronously before navigation in new-loan)
    if (!this.riskScore) {
      const raw = sessionStorage.getItem('rm_latest_risk_result');
      if (raw) {
        try {
          const cached = JSON.parse(raw);
          this.riskScore = cached?.riskScore ?? null;
          this.loanData = cached?.loanData ?? null;
        } catch {
          // Ignore malformed cache and continue to API fallback.
        }
      }
    }

    // Fallback 3: fetch by riskId query param from API
    if (!this.riskScore) {
      const riskId = Number(this.route.snapshot.queryParamMap.get('riskId'));
      if (Number.isFinite(riskId) && riskId > 0) {
        this.loadingResult = true;
        this.loanService.getRiskScoreById(riskId).subscribe({
          next: (savedRisk) => {
            this.riskScore = savedRisk;
            this.loadingResult = false;
          },
          error: () => {
            this.loadingResult = false;
            this.loadError = 'Could not load risk assessment. The record may no longer exist.';
          }
        });
      } else {
        this.loadError = 'No risk assessment data available. Please generate a new risk score.';
      }
    }

    if (this.loanData?.autoSaved) {
      this.saveMsg = 'Saved automatically during generation. You can optionally override the score below.';
    }
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
    if (!this.riskScore || this.saving) return;

    if (this.overrideScore === null) {
      this.saveMsg = 'Already saved. Enter an override score only if you want to update it.';
      return;
    }

    if (this.overrideScore < 0 || this.overrideScore > 100) {
      this.saveMsg = '✗ Override score must be between 0 and 100.';
      return;
    }

    if (!this.riskScore?.riskId) {
      this.saveMsg = '✗ Could not locate saved risk record. Please regenerate the loan assessment.';
      return;
    }

    this.saveMsg = '';
    this.saving = true;

    const scoreToSave = this.overrideScore;
    const updated = {
      ...this.riskScore,
      riskScore: scoreToSave,
      riskGrade: this.getGrade(scoreToSave)
    };

    this.loanService.updateRiskScore(this.riskScore.riskId, updated).pipe(
      finalize(() => {
        this.saving = false;
      })
    ).subscribe({
      next: (saved) => {
        this.riskScore = saved;
        this.saveMsg = '✓ Risk score override saved successfully.';
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 401 || err.status === 403) {
          this.saveMsg = '✗ Session expired. Please login and try again.';
          return;
        }
        this.saveMsg = '✗ Failed to update risk score. Please try again.';
      }
    });
  }

  getGrade(s: number): string {
    return s >= 80 ? 'A' : s >= 60 ? 'B' : s >= 40 ? 'C' : 'D';
  }

  newApplication() { this.router.navigate(['/new-loan']); }
}
