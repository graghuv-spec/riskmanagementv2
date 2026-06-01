import { Component, OnInit } from '@angular/core';
import { LoanService, BorrowerProfile } from '../../core/services/loan.service';
import { BorrowerContextService } from '../../core/services/borrower-context.service';
import { AuthService } from '../../core/services/auth.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-borrower-hub',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './borrower-hub.component.html',
  styleUrls: ['./borrower-hub.component.scss']
})
export class BorrowerHubComponent implements OnInit {
  borrowers: BorrowerProfile[] = [];
  searchQuery = '';
  loading = false;
  error = '';
  selectedBorrower: BorrowerProfile | null = null;
  isAdmin = false;

  constructor(
    private loanService: LoanService,
    private borrowerContext: BorrowerContextService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.isAdmin = this.authService.getUser()?.role === 'Admin';
    this.loadBorrowers();
  }

  loadBorrowers() {
    this.loading = true;
    this.loanService.searchBorrowers(this.searchQuery || '').subscribe({
      next: (data) => {
        this.borrowers = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load borrowers';
        this.loading = false;
      }
    });
  }

  selectBorrower(borrower: BorrowerProfile) {
    this.borrowerContext.selectBorrower(borrower);
    this.selectedBorrower = borrower;
    // Navigate to dashboard (implement navigation as needed)
  }

  updateCreditScore(borrower: BorrowerProfile, newScore: number) {
    if (!this.isAdmin) return;
    this.loanService.updateCreditScore(borrower.borrowerId, newScore).subscribe({
      next: (updated) => {
        borrower.creditScore = updated.creditScore;
      }
    });
  }

  // Add methods for add/edit borrower as needed
}
