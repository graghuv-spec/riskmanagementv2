import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  navItems = [
    { label: 'Dashboard',  icon: '📊', path: '/dashboard' },
    { label: 'New Loan',   icon: '📝', path: '/new-loan'  },
    { label: 'Portfolio',  icon: '📈', path: '/portfolio' },
    { label: 'Reports',    icon: '📄', path: '/reports'   }
  ];
}
