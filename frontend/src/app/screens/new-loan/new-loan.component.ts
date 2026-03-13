import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { LoanService } from '../../core/services/loan.service';
import { AuthService } from '../../core/services/auth.service';

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

  form: any = {
    fullName: '', nationalId: '', gender: 'Male', age: null,
    location: '', businessSector: '',
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

  constructor(private loanService: LoanService, private auth: AuthService, private router: Router) {
    this.loadLookups();
  }

  private extractApiError(err: HttpErrorResponse): string | null {
    const apiError = err.error;
    if (!apiError) return null;

    if (typeof apiError === 'string' && apiError.trim()) {
      return apiError.trim();
    }

    if (apiError.message && typeof apiError.message === 'string') {
      return apiError.message;
    }

    if (apiError.errors && typeof apiError.errors === 'object') {
      const first = Object.values(apiError.errors)[0];
      if (typeof first === 'string' && first.trim()) {
        return first;
      }
    }

    return null;
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
          this.form.location = '';
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

    const required = ['fullName','nationalId','age','location','businessSector','monthlyIncome','collateralValue','loanAmount','interestRate','tenureMonths'];
    const hasMissing = required.some((key) => {
      const value = this.form[key];
      if (value === null || value === undefined) return true;
      if (typeof value === 'string') return !value.trim();
      return false;
    });

    if (hasMissing) { this.error = 'Please fill in all required fields.'; return; }
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

    this.loanService.calculateRiskScore(payload).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (result) => {
        const navState = { riskScore: result, loanData: { ...this.form } };
        // Keep latest generated result so Risk Result can recover if router state is lost.
        sessionStorage.setItem('rm_latest_risk_result', JSON.stringify(navState));
        this.router.navigate(['/risk-result'], { state: navState });
      },
      error: (err: HttpErrorResponse) => {
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

        if (err.status === 400) {
          this.error = this.extractApiError(err) ?? 'Request validation failed. Please review your inputs and try again.';
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

        this.error = this.extractApiError(err) ?? 'Unable to generate risk score. Please review inputs and try again.';
      }
    });
  }
}
