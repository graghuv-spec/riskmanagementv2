import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./screens/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./screens/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'new-loan',
    canActivate: [authGuard],
    loadComponent: () => import('./screens/new-loan/new-loan.component').then(m => m.NewLoanComponent)
  },
  {
    path: 'risk-result',
    canActivate: [authGuard],
    loadComponent: () => import('./screens/risk-result/risk-result.component').then(m => m.RiskResultComponent)
  },
  {
    path: 'portfolio',
    canActivate: [authGuard],
    loadComponent: () => import('./screens/portfolio-analytics/portfolio-analytics.component').then(m => m.PortfolioAnalyticsComponent)
  },
  {
    path: 'reports',
    canActivate: [authGuard],
    loadComponent: () => import('./screens/report-generator/report-generator.component').then(m => m.ReportGeneratorComponent)
  },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];
