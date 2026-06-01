import { Injectable, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { BorrowerContextService } from '../services/borrower-context.service';

export const borrowerContextGuard: CanActivateFn = (route, state) => {
  const context = inject(BorrowerContextService);
  const router = inject(Router);
  if (!context.getSelectedBorrower()) {
    router.navigate(['/borrower-hub']);
    return false;
  }
  return true;
};
