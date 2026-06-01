import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { BorrowerContextService } from '../../core/services/borrower-context.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  selectedBorrower: any = null;
  constructor(public authService: AuthService, private router: Router, private borrowerContext: BorrowerContextService) {
    this.selectedBorrower = this.borrowerContext.getSelectedBorrower();
    this.borrowerContext.borrower$.subscribe(b => this.selectedBorrower = b);
  }
  get user() { return this.authService.getUser(); }
  logout() { this.authService.logout(); this.router.navigate(['/login']); }
}
