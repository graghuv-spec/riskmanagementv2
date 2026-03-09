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
    location: '', businessSector: 'Technology',
    monthlyIncome: null, collateralValue: null,
    loanAmount: null, interestRate: null, tenureMonths: null,
    disbursementDate: new Date().toISOString().split('T')[0], status: 'Active'
  };

  sectors = ['Technology','Finance','Agriculture','Retail','Manufacturing','Healthcare','Education'];
  locations = ['Nairobi','Mombasa','Kampala','Dar es Salaam','Lusaka','Accra','Lagos'];

  get collateralRatio(): number {
    if (!this.form.loanAmount || !this.form.collateralValue) return 0;
    return +(this.form.collateralValue / this.form.loanAmount).toFixed(2);
  }

  constructor(private loanService: LoanService, private router: Router) {}

  generate() {
    const required = ['fullName','nationalId','age','location','monthlyIncome','collateralValue','loanAmount','interestRate','tenureMonths'];
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
