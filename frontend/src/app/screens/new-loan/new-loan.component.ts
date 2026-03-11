import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LoanService } from '../../core/services/loan.service';

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

  constructor(private loanService: LoanService, private router: Router) {
    this.loadLookups();
  }

  private loadLookups() {
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
          this.error = 'Reference data is missing. Please add borrowers with sector and location data first.';
        }
      },
      error: () => {
        this.error = 'Failed to load reference data from backend.';
      }
    });
  }

  generate() {
    const required = ['fullName','nationalId','age','location','businessSector','monthlyIncome','collateralValue','loanAmount','interestRate','tenureMonths'];
    if (required.some(k => !this.form[k])) { this.error = 'Please fill in all required fields.'; return; }
    this.error = ''; this.loading = true;
    this.loanService.calculateRiskScore(this.form).subscribe({
      next: (result) => {
        this.router.navigate(['/risk-result'], { state: { riskScore: result, loanData: { ...this.form } } });
      },
      error: () => { this.error = 'Failed to calculate risk score. Is the backend running?'; this.loading = false; }
    });
  }
}
