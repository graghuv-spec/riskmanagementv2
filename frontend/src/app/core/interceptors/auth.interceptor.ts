import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const raw = localStorage.getItem('rm_user');
  if (!raw) return next(req);

  try {
    const user = JSON.parse(raw) as { token?: string };
    if (!user?.token || !req.url.startsWith('/api')) {
      return next(req);
    }
    return next(req.clone({
      setHeaders: {
        Authorization: `Bearer ${user.token}`
      }
    }));
  } catch {
    return next(req);
  }
};
