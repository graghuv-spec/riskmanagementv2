import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // Allow anonymous demo flow for new loan creation.
  if (state.url.startsWith('/new-loan')) {
    return true;
  }

  // Allow read-only risk result links in browsers that do not retain auth storage.
  if (state.url.startsWith('/risk-result') && !!route.queryParamMap.get('riskId')) {
    return true;
  }

  if (auth.isLoggedIn()) return true;
  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  });
};
