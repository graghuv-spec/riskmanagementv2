import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { BorrowerContextService } from '../../core/services/borrower-context.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  navItems = [
    { label: 'Borrower Hub', icon: '👤', path: '/borrower-hub' },
    { label: 'Dashboard',  icon: '📊', path: '/dashboard' },
    { label: 'New Loan',   icon: '📝', path: '/new-loan'  },
    { label: 'Portfolio',  icon: '📈', path: '/portfolio' },
    { label: 'Reports',    icon: '📄', path: '/reports'   }
  ];

  selectedBorrower: any = null;

  constructor(private borrowerContext: BorrowerContextService) {
    this.selectedBorrower = this.borrowerContext.getSelectedBorrower();
    this.borrowerContext.borrower$.subscribe(b => this.selectedBorrower = b);
  }

  changeBorrower() {
    this.borrowerContext.clearBorrower();
    // Navigate to Borrower Hub
    window.location.href = '/borrower-hub';
  }
}
