import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, map, switchMap, tap, timeout } from 'rxjs/operators';
import { Subscription, TimeoutError } from 'rxjs';
import { BorrowerProfile, LoanService } from '../../core/services/loan.service';
import { AuthService } from '../../core/services/auth.service';
import { BorrowerContextService } from '../../core/services/borrower-context.service';

@Component({
  selector: 'app-new-loan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './new-loan.component.html',
  styleUrl: './new-loan.component.scss'
})
export class NewLoanComponent {
  loading = false;
  error = '';
  lookupWarning = '';
  profileWarning = '';
  borrowerDetailsReadonly = true;
  financialDetailsReadonly = true;
  private borrowerId: number | null = null;

  form: any = {
    fullName: '', nationalId: '', gender: 'Male', age: null,
    location: '', businessSector: 'Agriculture',
    monthlyIncome: null, collateralValue: null,
    loanAmount: null, interestRate: null, tenureMonths: null,
    disbursementDate: new Date().toISOString().split('T')[0], status: 'Active'
  };

  sectors: string[] = [];
  locations: string[] = [];

  get collateralRatio(): number {
    if (!this.form.loanAmount || !this.form.collateralValue) return 0;
    return +(this.form.collateralValue / this.form.loanAmount).toFixed(2);
  }

  constructor(private loanService: LoanService, private auth: AuthService, private router: Router, private borrowerContext: BorrowerContextService) {
    this.loadLookups();
    this.loadBorrowerFromContext();
  }

  private loadBorrowerFromContext() {
    const borrower = this.borrowerContext.getSelectedBorrower();
    if (!borrower) {
      this.router.navigate(['/borrower-hub']);
      return;
    }
    this.borrowerId = borrower.borrowerId;
    this.form.fullName = borrower.fullName ?? '';
    this.form.nationalId = borrower.nationalId ?? '';
    this.form.gender = borrower.gender ?? 'Male';
    this.form.age = borrower.age ?? null;
    this.form.location = borrower.location ?? '';
    this.form.businessSector = borrower.businessSector ?? '';
    this.form.monthlyIncome = borrower.monthlyIncome ?? null;
    this.form.collateralValue = borrower.collateralValue ?? null;
    this.borrowerDetailsReadonly = true;
    this.financialDetailsReadonly = true;
  }

  private loadLookups() {
    this.lookupWarning = '';
    this.loanService.getBorrowerLookups().subscribe({
      next: (lookups) => {
        const sectors = Array.isArray(lookups?.sectors) ? lookups.sectors.filter(Boolean) : [];
        const locations = Array.isArray(lookups?.locations) ? lookups.locations.filter(Boolean) : [];

        this.sectors = sectors;
        this.locations = locations;

        if (!this.sectors.includes(this.form.businessSector)) {
          this.form.businessSector = this.sectors[0] ?? '';
        }
        if (!this.locations.includes(this.form.location)) {
          this.form.location = this.locations[0] ?? '';
        }

        if (!this.sectors.length || !this.locations.length) {
          this.lookupWarning = 'Lookup data is limited. You can still continue by entering sector/location manually.';
        }
      },
      error: () => {
        this.error = 'Failed to load reference data from backend.';
      }
    });
  }

  get useLocationSelect(): boolean {
    return this.locations.length > 0;
  }

  get useSectorSelect(): boolean {
    return this.sectors.length > 0;
  }

  generate() {
    if (this.loading) return;

    if (!this.borrowerId) {
      this.error = 'Borrower profile is required before creating a loan.';
      return;
    }

    this.form.location = this.normalizeLocation(this.form.location);

    const required: Array<{ key: string; label: string }> = [
      { key: 'loanAmount', label: 'Loan Amount' },
      { key: 'interestRate', label: 'Interest Rate' },
      { key: 'tenureMonths', label: 'Tenure Months' }
    ];

    const missing = required.filter(({ key }) => {
      const value = this.form[key];
      if (value === null || value === undefined) return true;
      if (typeof value === 'string') return !value.trim();
      return false;
    });

    if (missing.length) {
      this.error = `Please fill required fields: ${missing.map((m) => m.label).join(', ')}.`;
      return;
    }
    this.error = ''; this.loading = true;

    const payload = {
      ...this.form,
      age: Number(this.form.age),
      monthlyIncome: Number(this.form.monthlyIncome),
      collateralValue: Number(this.form.collateralValue),
      loanAmount: Number(this.form.loanAmount),
      interestRate: Number(this.form.interestRate),
      tenureMonths: Number(this.form.tenureMonths)
    };

    const user = this.auth.getUser();
    const now = this.toApiDateTime(new Date().toISOString());

    this.loanService.calculateRiskScore(payload).pipe(
      tap((result) => console.log('[generate] 1/3 calculateRiskScore response:', result)),
      switchMap((result) => {
        const loanPayload = {
          borrowerId: this.borrowerId,
          institutionId: user?.institutionId ?? null,
          loanAmount: payload.loanAmount,
          interestRate: payload.interestRate,
          tenureMonths: payload.tenureMonths,
          status: payload.status,
          disbursementDate: this.toApiDateTime(payload.disbursementDate),
          createdAt: now
        };

        return this.loanService.createLoan(loanPayload).pipe(
          tap((savedLoan) => console.log('[generate] 2/3 createLoan response:', savedLoan)),
          switchMap((savedLoan) => {
            const riskPayload = {
              ...result,
              loanId: savedLoan?.loanId,
              createdAt: now
            };

            return this.loanService.saveRiskScore(riskPayload).pipe(
              tap((savedRisk) => console.log('[generate] 3/3 saveRiskScore response:', savedRisk)),
              map((savedRisk) => ({
                riskScore: savedRisk,
                loanData: {
                  ...this.form,
                  borrowerId: this.borrowerId,
                  loanId: savedLoan?.loanId,
                  autoSaved: true
                }
              }))
            );
          })
        );
      }),
      timeout(30000),
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (navState) => {
        const riskId = navState?.riskScore?.riskId;
        if (!riskId) {
          this.error = 'Risk score was calculated but could not be saved. Please try again.';
          return;
        }
        try {
          sessionStorage.setItem('rm_latest_risk_result', JSON.stringify(navState));
        } catch {
          // sessionStorage may be unavailable; navigation can still proceed via state + queryParam.
        }
        console.warn('[generate] All 3 steps done. Navigating to /risk-result?riskId=' + riskId);

        // Monitor router events during this navigation
        const routerSub: Subscription = this.router.events.subscribe(event => {
          if (event instanceof NavigationStart) {
            console.warn('[router] NavigationStart →', event.url);
          } else if (event instanceof NavigationEnd) {
            console.warn('[router] NavigationEnd →', event.urlAfterRedirects);
            routerSub.unsubscribe();
          } else if (event instanceof NavigationCancel) {
            console.error('[router] NavigationCancel →', event.url, 'Reason:', event.reason);
            routerSub.unsubscribe();
          } else if (event instanceof NavigationError) {
            console.error('[router] NavigationError →', event.url, 'Error:', event.error);
            routerSub.unsubscribe();
          }
        });

        this.router.navigate(['/risk-result'], {
          state: navState,
          queryParams: { riskId }
        }).then(
          (success) => console.warn('[generate] router.navigate() resolved:', success),
          (err) => console.error('[generate] router.navigate() rejected:', err)
        );
      },
      error: (err: HttpErrorResponse | TimeoutError) => {
        console.error('[generate] error:', err);

        if (err instanceof TimeoutError) {
          this.error = 'Request timed out. Please check your connection and try again.';
          return;
        }

        if (err.status === 401 || err.status === 403) {
          this.error = 'Session expired or unauthorized. Redirecting to login...';
          this.auth.logout();
          setTimeout(() => {
            this.router.navigate(['/login'], {
              queryParams: { returnUrl: '/new-loan', reason: 'session-expired' }
            });
          }, 800);
          return;
        }

        if (err.status >= 500) {
          this.error = 'Server error while calculating risk score. Please try again.';
          return;
        }

        if (err.status === 0) {
          this.error = 'Cannot reach backend service. Check backend startup and network.';
          return;
        }

        this.error = 'Unable to generate risk score. Please review inputs and try again.';
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

  private normalizeLocation(value: string | null | undefined): string {
    if (!value) return '';
    const normalized = String(value).trim().toLowerCase();
    if (!normalized) return '';

    const aliases: Record<string, string> = {
      mamosa: 'Mombasa',
      mombassa: 'Mombasa',
      kampalla: 'Kampala',
      nairobii: 'Nairobi'
    };

    if (aliases[normalized]) return aliases[normalized];
    return String(value).trim();
  }
}
